package de.signaliduna.dltmanager.adapter.db;

import de.signaliduna.dltmanager.adapter.db.model.AdminActionHistoryItemEntity;

public interface DltEventRepositoryCustom {
	boolean updateLastAdminActionForDltEvent(String dltEventId, AdminActionHistoryItemEntity adminActionHistoryItem);
}
