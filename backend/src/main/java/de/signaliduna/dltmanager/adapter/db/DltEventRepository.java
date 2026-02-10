package de.signaliduna.dltmanager.adapter.db;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import de.signaliduna.dltmanager.adapter.db.model.AdminActionHistoryItemEntity;
import de.signaliduna.dltmanager.adapter.db.model.DltEventEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
public class DltEventRepository {

	private final MongoOperations mongoOperations;

	public DltEventRepository(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
	}

	public DltEventEntity save(DltEventEntity dltEntity) {
		return mongoOperations.save(dltEntity);
	}

	public Stream<DltEventEntity> streamAll() {
		return mongoOperations.query(DltEventEntity.class).matching(new Query().with(
			Sort.by(new Sort.Order(Sort.Direction.DESC, DltEventEntity.FIELD_lastAdminAction))
		)).stream();
	}

	public Optional<DltEventEntity> findByDltEventId(String id) {
		return Optional.ofNullable(mongoOperations.findById(id, DltEventEntity.class));
	}

	/** @param dltEventId database id of an {@code DltEventEntity} */
	public boolean updateLastAdminActionForDltEvent(String dltEventId, AdminActionHistoryItemEntity adminActionHistoryItem) {
		final UpdateResult result = mongoOperations.updateFirst(
			new Query().addCriteria(where(DltEventEntity.FIELD_id).is(dltEventId)),
			new Update().set(DltEventEntity.FIELD_lastAdminAction, adminActionHistoryItem),
			DltEventEntity.class
		);
		return result.getMatchedCount() > 0;
	}

	public boolean deleteByDltEventId(String dltEventId) {
		final DeleteResult result = mongoOperations.remove(new Query().addCriteria(where(DltEventEntity.FIELD_id).is(dltEventId)), DltEventEntity.class);
		return result.getDeletedCount() > 0;
	}
}
