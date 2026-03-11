package de.signaliduna.dltmanager.core.service;

import de.signaliduna.dltmanager.adapter.db.DltEventPersistenceAdapter;
import de.signaliduna.dltmanager.adapter.http.client.PapierantragEingangAdapter;
import de.signaliduna.dltmanager.core.exception.DltEventAdminServiceException;
import de.signaliduna.dltmanager.core.model.*;
import de.signaliduna.dltmanager.utils.SafeExceptionLogger;
import de.signaliduna.elpa.sharedlib.model.Vorgang;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DltEventAdminService {
    private static final Logger log = LoggerFactory.getLogger(DltEventAdminService.class);
    private final DltEventPersistenceAdapter dltEventPersistenceAdapter;
    private final PapierantragEingangAdapter papierantragEingangAdapter;
    private final JsonMapper jsonMapper;
    
    public DltEventAdminService(
            DltEventPersistenceAdapter dltEventPersistenceAdapter,
            PapierantragEingangAdapter papierantragEingangAdapter,
            JsonMapper jsonMapper
    ) {
        this.dltEventPersistenceAdapter = dltEventPersistenceAdapter;
        this.papierantragEingangAdapter = papierantragEingangAdapter;
        this.jsonMapper = jsonMapper;
    }
    
    public List<DltEventWithAdminActions> getDltEvents() {
        return dltEventPersistenceAdapter.findAll().stream()
                .map(event -> new DltEventWithAdminActions(event, this.getAvailableAdminActionsFor(event)))
                .toList();
    }
    
    public Optional<DltEventWithAdminActions> getDltEventByDltEventId(UUID dltEventId) {
        return dltEventPersistenceAdapter.findDltEventById(dltEventId).map(dltEvent ->
                new DltEventWithAdminActions(dltEvent, getAvailableAdminActionsFor(dltEvent))
        );
    }
    
    // --- Resend / Delete ---
    
    public boolean resendPapierantrag(UUID dltEventId, String userName) {
        log.info("resending application for DltEventId {} to PapierantragEingang", dltEventId);
        Optional<DltEvent> dltEventOpt = dltEventPersistenceAdapter.findDltEventById(dltEventId);
        if (dltEventOpt.isEmpty()) {
            return false;
        }
        
        final Vorgang vorgang = decodeDltEventPayload(dltEventOpt.get(), Vorgang.class);
        final String rohdatenUuidId = vorgang.metadaten().rohdatenUuid();
        if (rohdatenUuidId == null) {
            log.error("Cannot resend: metadaten.rohdatenUuid of DltEvent is null");
            throw new DltEventAdminServiceException(
                    "Cannot resend: metadaten.rohdatenUuid of DltEvent %s is null".formatted(dltEventId));
        }
        resendPapierantragImpl(dltEventId, rohdatenUuidId, userName);
        return true;
    }
    
    void resendPapierantragImpl(UUID dltEventId, String rohdatenUuidId, String userName) {
        final var adminActionItemBuilder = AdminActionHistoryItem.builder()
                .actionName(AdminAction.RESEND_TO_PAPIERANTRAG_EINGANG.name())
                .timestamp(LocalDateTime.now())
                .userName(userName);
        
        try {
            papierantragEingangAdapter.resendPapierantrag(rohdatenUuidId);
            dltEventPersistenceAdapter.addAdminAction(dltEventId,
                    adminActionItemBuilder.status(DltEventAction.Status.TRIGGERED.name()).build()
            );
            final String safeDltEventId = SafeExceptionLogger.sanitizeLogArg(dltEventId.toString());
            final String safeRohdatenUuidId = SafeExceptionLogger.sanitizeLogArg(rohdatenUuidId);
            log.info("resent application for dltEventId {} (rohdatenUuidId: {})", safeDltEventId, safeRohdatenUuidId);
        } catch (FeignException e) {
            // Security: SafeExceptionLogger strips request/response body that may contain PII
            String safeMsg = SafeExceptionLogger.sanitizeFeignException(e);
            log.warn("failed to resend for dltEventId={} (rohdatenUuidId={}): {}", dltEventId, rohdatenUuidId, safeMsg);
            dltEventPersistenceAdapter.addAdminAction(dltEventId,
                    adminActionItemBuilder
                            .status(DltEventAction.Status.FAILED.name())
                            .statusError(safeMsg)
                            .build()
            );
        }
    }
    
    public boolean deleteDltEvent(UUID dltEventId, String userName) {
        log.info("deleting DltEvent with dltEventId {}", dltEventId);
        try {
            return dltEventPersistenceAdapter.deleteByDltEventId(dltEventId);
        } catch (Exception e) {
            dltEventPersistenceAdapter.addAdminAction(dltEventId, AdminActionHistoryItem.builder()
                    .actionName(AdminAction.DELETE_DLT_EVENT.name())
                    .timestamp(LocalDateTime.now())
                    .userName(userName)
                    .status(DltEventAction.Status.FAILED.name())
                    .statusError(SafeExceptionLogger.safeClassName(e))
                    .build()
            );
            throw e;
        }
    }
    
    <T> T decodeDltEventPayload(DltEvent dltEvent, Class<T> targetType) {
        try {
            return jsonMapper.readValue(dltEvent.payload(), targetType);
        } catch (JacksonException e) {
            throw new DltEventAdminServiceException("failed to decode payload of DltEvent id " + dltEvent.dltEventId(), e);
        }
    }
    
    @SuppressWarnings("unused")
    public List<DltEventAction> getAvailableAdminActionsFor(DltEvent dltEvent) {
        return List.of(
                new DltEventAction("Retry", "Triggers a re-processing of the event that caused the DLT-event in papierantrag-eingang."),
                new DltEventAction("Delete", "Deletes an DltEvent from the the dlt-manager database.")
        );
    }
    
}
