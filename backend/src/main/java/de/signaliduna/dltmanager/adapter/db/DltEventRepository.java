package de.signaliduna.dltmanager.adapter.db;

import de.signaliduna.dltmanager.adapter.db.model.DltEventEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DltEventRepository extends JpaRepository<DltEventEntity, UUID> {

    @EntityGraph(attributePaths = "adminActions")
    @Query("""
        SELECT e FROM DltEventEntity e
        ORDER BY (
            SELECT MAX(a.timestamp)
            FROM AdminActionHistoryItemEntity a
            WHERE a.dltEvent.dltEventId = e.dltEventId
        ) DESC NULLS LAST
        """)
    List<DltEventEntity> findAllOrderedByLastAdminActionDesc();

    long deleteByDltEventId(UUID dltEventId);
}
