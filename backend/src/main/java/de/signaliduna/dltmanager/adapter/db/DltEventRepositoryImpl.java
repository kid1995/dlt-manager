package de.signaliduna.dltmanager.adapter.db;

import de.signaliduna.dltmanager.adapter.db.model.AdminActionHistoryItemEntity;
import de.signaliduna.dltmanager.adapter.db.model.DltEventEntity;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;
import org.springframework.stereotype.Repository;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
public class DltEventRepositoryImpl implements DltEventRepositoryCustom {

	private final MongoOperations mongoOperations;

	public DltEventRepositoryImpl(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
	}

	@Override
	public boolean updateLastAdminActionForDltEvent(String dltEventId, AdminActionHistoryItemEntity adminActionHistoryItem) {
		final UpdateResult result = mongoOperations.updateFirst(
			new Query().addCriteria(where(DltEventEntity.FIELD_id).is(dltEventId)),
			new Update().set(DltEventEntity.FIELD_lastAdminAction, adminActionHistoryItem),
			DltEventEntity.class
		);
		return result.getMatchedCount() > 0;
	}
}
