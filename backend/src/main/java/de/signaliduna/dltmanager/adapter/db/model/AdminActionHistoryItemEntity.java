package de.signaliduna.dltmanager.adapter.db.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_action_history")
public class AdminActionHistoryItemEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dlt_event_id", nullable = false)
    private DltEventEntity dltEvent;
    
    @Column(name = "user_name", nullable = false)
    private String userName;
    
    @Column(name = "performed_at", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "action_name", nullable = false)
    private String actionName;
    
    @Column(name = "action_details", columnDefinition = "TEXT")
    private String actionDetails;
    
    @Column(name = "action_status", nullable = false, length = 50)
    private String status;
    
    @Column(name = "status_error", columnDefinition = "TEXT")
    private String statusError;
    
    protected AdminActionHistoryItemEntity() {
    }
    
    private AdminActionHistoryItemEntity(Builder builder) {
        this.dltEvent = builder.dltEvent;
        this.userName = builder.userName;
        this.timestamp = builder.timestamp;
        this.actionName = builder.actionName;
        this.actionDetails = builder.actionDetails;
        this.status = builder.status;
        this.statusError = builder.statusError;
    }
    
    // --- Getters ---
    
    public Long getId() { return id; }
    public DltEventEntity getDltEvent() { return dltEvent; }
    public String getUserName() { return userName; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getActionName() { return actionName; }
    @Nullable public String getActionDetails() { return actionDetails; }
    public String getStatus() { return status; }
    @Nullable public String getStatusError() { return statusError; }
    
    // --- Setters ---
    
    public void setDltEvent(DltEventEntity dltEvent) { this.dltEvent = dltEvent; }
    
    public boolean isSuccess() {
        return statusError == null || statusError.isEmpty();
    }
    
    /**
     * PII-safe: statusError may contain exception messages with payload fragments.
     */
    @Override
    public String toString() {
        return "AdminActionHistoryItemEntity{id=%d, actionName=%s, status=%s}"
                .formatted(id, actionName, status);
    }
    
    // Builder
    
    public static Builder builder() { return new Builder(); }
    public Builder toBuilder() { return new Builder(this); }
    
    public static final class Builder {
        private DltEventEntity dltEvent;
        private String userName;
        private LocalDateTime timestamp;
        private String actionName;
        @Nullable private String actionDetails;
        private String status;
        @Nullable private String statusError;
        
        private Builder() {}
        
        private Builder(AdminActionHistoryItemEntity source) {
            this.dltEvent = source.dltEvent;
            this.userName = source.userName;
            this.timestamp = source.timestamp;
            this.actionName = source.actionName;
            this.actionDetails = source.actionDetails;
            this.status = source.status;
            this.statusError = source.statusError;
        }
        
        public Builder dltEvent(DltEventEntity value) { this.dltEvent = value; return this; }
        public Builder userName(String value) { this.userName = value; return this; }
        public Builder timestamp(LocalDateTime value) { this.timestamp = value; return this; }
        public Builder actionName(String value) { this.actionName = value; return this; }
        public Builder actionDetails(String value) { this.actionDetails = value; return this; }
        public Builder status(String value) { this.status = value; return this; }
        public Builder statusError(String value) { this.statusError = value; return this; }
        
        public AdminActionHistoryItemEntity build() { return new AdminActionHistoryItemEntity(this); }
    }
}
