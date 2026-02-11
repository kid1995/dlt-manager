package de.signaliduna.dltmanager.core.service;

import tools.jackson.databind.ObjectMapper;
import de.signaliduna.dltmanager.adapter.db.DltEventPersistenceAdapter;
import de.signaliduna.dltmanager.adapter.http.client.PapierantragEingangAdapter;
import de.signaliduna.dltmanager.core.exception.DltEventAdminServiceException;
import de.signaliduna.dltmanager.core.model.*;
import de.signaliduna.elpa.sharedlib.model.Vorgang;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class DltEventAdminService {
	private static final Logger log = LoggerFactory.getLogger(DltEventAdminService.class);

	private final DltEventPersistenceAdapter dltEventPersistenceAdapter;
	private final PapierantragEingangAdapter papierantragEingangAdapter;
	private final ObjectMapper objectMapper;

	public DltEventAdminService(
		DltEventPersistenceAdapter dltEventPersistenceAdapter, PapierantragEingangAdapter papierantragEingangAdapter, ObjectMapper objectMapper
	) {
		this.dltEventPersistenceAdapter = dltEventPersistenceAdapter;
		this.papierantragEingangAdapter = papierantragEingangAdapter;
		this.objectMapper = objectMapper;
	}

	public List<DltEventWithAdminActions> getDltEvents() {
		try (final var dltEventsStream = dltEventPersistenceAdapter.streamAll()) {
			return dltEventsStream.map(event -> new DltEventWithAdminActions(event, this.getAvailableAdminActionsFor(event))).toList();
		}
	}

	public Optional<DltEventWithAdminActions> getDltEventByDltEventId(String dltEventId) {
		return dltEventPersistenceAdapter.findDltEventById(dltEventId).map(dltEvent ->
			new DltEventWithAdminActions(dltEvent, getAvailableAdminActionsFor(dltEvent))
		);
	}

	public boolean resendPapierantrag(String dltEventId, String userName) {
		log.info("resending application for DltEventId {} to PapierantragEingang", dltEventId);
		Optional<DltEvent> dltEventOpt = dltEventPersistenceAdapter.findDltEventById(dltEventId);
		if (dltEventOpt.isEmpty()) {
			return false;
		}

		// Currently, the payload of DltEvents is always of type `Vorgang`
		final Vorgang vorgang = decodeDltEventPayload(dltEventOpt.get(), Vorgang.class);
		final String rohdatenUuidId = vorgang.metadaten().rohdatenUuid();
		if (rohdatenUuidId == null) {
			final var errorTxt = "Cannot resend application to PapierantragEingang: metadaten.rohdatenUuid of DltEvent with id %s is null"
				.formatted(dltEventId);
			log.error(errorTxt);
			throw new DltEventAdminServiceException(errorTxt);
		}
		resendPapierantragImpl(dltEventId, rohdatenUuidId, userName);
		return true;
	}

	void resendPapierantragImpl(String dltEventId, String rohdatenUuidId, String userName) {
		final var adminActionItemBuilder = AdminActionHistoryItem.builder()
			.actionName(AdminAction.RESEND_TO_PAPIERANTRAG_EINGANG.name())
			.timestamp(LocalDateTime.now())
			.userName(userName);

		try {
			papierantragEingangAdapter.resendPapierantrag(rohdatenUuidId);
			log.info("resent application for dltEventId {} to papierantrag-eingang (rohdatenUuidId: {})", dltEventId, rohdatenUuidId);
			dltEventPersistenceAdapter.updateLastAdminActionForDltEvent(dltEventId,
				adminActionItemBuilder.status(DltEventAction.Status.TRIGGERED.name()).build()
			);
		} catch (FeignException e) {
			log.info("failed to resend application for dltEventId {} to papierantrag-eingang (rohdatenUuidId: {})", dltEventId, rohdatenUuidId, e);
			dltEventPersistenceAdapter.updateLastAdminActionForDltEvent(dltEventId,
				adminActionItemBuilder
					.status(DltEventAction.Status.FAILED.name())
					.statusError(e.getMessage())
					.build()
			);
		}
	}

	public boolean deleteDltEvent(String dltEventId, String userName) {
		log.info("deleting DltEvent with dltEventId {}", dltEventId);
		try {
			return dltEventPersistenceAdapter.deleteByDltEventId(dltEventId);
		} catch (Exception e) {
			dltEventPersistenceAdapter.updateLastAdminActionForDltEvent(dltEventId, AdminActionHistoryItem.builder()
				.actionName(AdminAction.DELETE_DLT_EVENT.name())
				.timestamp(LocalDateTime.now())
				.userName(userName)
				.status(DltEventAction.Status.FAILED.name())
				.statusError(e.getMessage())
				.build()
			);
			throw e;
		}
	}

	<T> T decodeDltEventPayload(DltEvent dltEvent, Class<T> targetType) {
		try {
			return objectMapper.readValue(dltEvent.payload(), targetType);
		} catch (JacksonException e) {
			throw new DltEventAdminServiceException("failed to decode payload of DltEvent id " + dltEvent.dltEventId(), e);
		}
	}

	/**
	 * Creates the list of actions available in the UI.
	 */
	@SuppressWarnings("unused")
	public List<DltEventAction> getAvailableAdminActionsFor(DltEvent dltEvent) {
		return List.of(
			new DltEventAction("Retry", "Triggers a re-processing of the event that caused the DLT-event in papierantrag-eingang."),
			new DltEventAction("Delete", "Deletes an DltEvent from the the dlt-manager database.")
		);
	}
}
