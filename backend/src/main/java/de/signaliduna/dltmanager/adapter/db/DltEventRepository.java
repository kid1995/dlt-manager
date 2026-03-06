package de.signaliduna.dltmanager.adapter.db;

import de.signaliduna.dltmanager.adapter.db.model.DltEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DltEventRepository extends JpaRepository<DltEventEntity, String> {
    
    @Query("""
		SELECT e FROM dlt_event e
		LEFT JOIN e.adminActions a
		GROUP BY e
		ORDER BY MAX(a.timestamp) DESC NULLS LAST
		""")
    List<DltEventEntity> findAllOrderedByLastAdminActionDesc();
    
    long deleteByDltEventId(String dltEventId);
    
    boolean findAllByOrderByLastAdminActionDesc();
}
