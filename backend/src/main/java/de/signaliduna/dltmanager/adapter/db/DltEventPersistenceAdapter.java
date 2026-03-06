package de.signaliduna.dltmanager.adapter.db;

import de.signaliduna.dltmanager.adapter.db.mapper.EntityMapper;
import de.signaliduna.dltmanager.adapter.db.model.AdminActionHistoryItemEntity;
import de.signaliduna.dltmanager.core.model.AdminActionHistoryItem;
import de.signaliduna.dltmanager.core.model.DltEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DltEventPersistenceAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(DltEventPersistenceAdapter.class);
    
    private final DltEventRepository dltEventRepository;
    
    public DltEventPersistenceAdapter(DltEventRepository dltEventRepository) {
        this.dltEventRepository = dltEventRepository;
    }
    
    @Transactional(readOnly = true)
    public List<DltEvent> findAll() {
        return dltEventRepository.findAllOrderedByLastAdminActionDesc().stream()
                .map(EntityMapper::fromDltEventEntity)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public Optional<DltEvent> findDltEventById(String dltEventId) {
        return dltEventRepository.findById(dltEventId).map(EntityMapper::fromDltEventEntity);
    }
    
    @Transactional
    public boolean addAdminAction(String dltEventId, AdminActionHistoryItem adminActionHistoryItem) {
        return dltEventRepository.findById(dltEventId)
                .map(entity -> {
                    final AdminActionHistoryItemEntity actionEntity = EntityMapper.toAdminHistoryItemEntity(adminActionHistoryItem);
                    entity.addAdminAction(actionEntity);
                    dltEventRepository.save(entity);
                    return true;
                })
                .orElse(false);
    }
    
    public void save(DltEvent dltEvent) {
        dltEventRepository.save(EntityMapper.toDltEventEntity(dltEvent));
    }
    
    public boolean deleteByDltEventId(String dltEventId) {
        log.debug("deleting DLT event with dltEventId: {}", dltEventId);
        return dltEventRepository.deleteByDltEventId(dltEventId) > 0;
    }
    
    
}
