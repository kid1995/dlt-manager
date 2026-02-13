package de.signaliduna.dltmanager.adapter.db;

import de.signaliduna.dltmanager.adapter.db.model.DltEventEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.stream.Stream;

public interface DltEventRepository extends MongoRepository<DltEventEntity, String>, DltEventRepositoryCustom {
	Stream<DltEventEntity> findAllByOrderByLastAdminActionDesc();
	long deleteByDltEventId(String dltEventId);
}
