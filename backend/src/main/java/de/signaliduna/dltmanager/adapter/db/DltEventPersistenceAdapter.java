package de.signaliduna.dltmanager.adapter.db;

import de.signaliduna.dltmanager.adapter.db.mapper.EntityMapper;
import de.signaliduna.dltmanager.adapter.db.model.DltEventEntity;
import de.signaliduna.dltmanager.core.model.AdminActionHistoryItem;
import de.signaliduna.dltmanager.core.model.DltEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Stream;

@Service
public class DltEventPersistenceAdapter {

	private static final Logger log = LoggerFactory.getLogger(DltEventPersistenceAdapter.class);

	private final DltEventRepository dltEventRepository;

	public DltEventPersistenceAdapter(DltEventRepository dltEventRepository) {
		this.dltEventRepository = dltEventRepository;
	}

	public Stream<DltEvent> streamAll() {
		return dltEventRepository.findAllByOrderByLastAdminActionDesc().map(EntityMapper::fromDltEventEntity);
	}

	public Optional<DltEvent> findDltEventById(String dltEventId) {
		return dltEventRepository.findById(dltEventId).map(EntityMapper::fromDltEventEntity);
	}

	public void save(DltEvent dltEvent) {
		final DltEventEntity saved = dltEventRepository.save(EntityMapper.toDltEventEntity(dltEvent));
		EntityMapper.fromDltEventEntity(saved);
	}

	public boolean updateLastAdminActionForDltEvent(String dltEventId, AdminActionHistoryItem adminActionHistoryItem) {
		return dltEventRepository.updateLastAdminActionForDltEvent(dltEventId,
			EntityMapper.toAdminHistoryItemEntity(adminActionHistoryItem));
	}

	public boolean deleteByDltEventId(String dltEventId) {
		log.debug("deleting DLT event with dltEventId: {}", dltEventId);
		return dltEventRepository.deleteByDltEventId(dltEventId) > 0;
	}
}
