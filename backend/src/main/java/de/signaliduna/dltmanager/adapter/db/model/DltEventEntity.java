package de.signaliduna.dltmanager.adapter.db.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dlt_event")
public class DltEventEntity {
    
    @Id
    @Column(name = "dlt_event_id", nullable = false)
    private String dltEventId;
    
    @Column(name = "original_event_id", nullable = false)
    private String originalEventId;
    
    @Column(name = "service_name", nullable = false)
    private String serviceName;
    
    @Column(name = "add_to_dlt_timestamp", nullable = false)
    private LocalDateTime addToDltTimestamp;
    
    @Column(name = "kafka_topic")
    private String topic;
    
    @Column(name = "kafka_partition")
    private String partition;
    
    @Column(name = "trace_id")
    private String traceId;
    
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(name = "payload_media_type", nullable = false)
    private String payloadMediaType;
    
    @Column(name = "error", columnDefinition = "TEXT")
    private String error;
    
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;
    
    @OneToMany(mappedBy = "dltEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp DESC")
    private List<AdminActionHistoryItemEntity> adminActions = new ArrayList<>();
    
    protected DltEventEntity() {
        // no-arg constructor for hibernate
    }
    
    private DltEventEntity(Builder builder) {
        this.dltEventId = builder.dltEventId;
        this.originalEventId = builder.originalEventId;
        this.serviceName = builder.serviceName;
        this.addToDltTimestamp = builder.addToDltTimestamp;
        this.topic = builder.topic;
        this.partition = builder.partition;
        this.traceId = builder.traceId;
        this.payload = builder.payload;
        this.payloadMediaType = builder.payloadMediaType;
        this.error = builder.error;
        this.stackTrace = builder.stackTrace;
    }
    
    // Getters
    public String getDltEventId() { return dltEventId; }
    public String getOriginalEventId() { return originalEventId; }
    public String getServiceName() { return serviceName; }
    public LocalDateTime getAddToDltTimestamp() { return addToDltTimestamp; }
    public String getTopic() { return topic; }
    public String getPartition() { return partition; }
    @Nullable public String getTraceId() { return traceId; }
    public String getPayload() { return payload; }
    public String getPayloadMediaType() { return payloadMediaType; }
    @Nullable public String getError() { return error; }
    @Nullable public String getStackTrace() { return stackTrace; }
    public List<AdminActionHistoryItemEntity> getAdminActions() { return adminActions; }
    
    @Nullable
    public AdminActionHistoryItemEntity getLastAdminAction() {
        return adminActions.isEmpty() ? null : adminActions.getFirst();
    }
    
    // Setters
    public void setError(String error) { this.error = error; }
    
    public void addAdminAction(AdminActionHistoryItemEntity action) {
        action.setDltEvent(this);
        adminActions.addFirst(action);
    }
    
    /**
     * PII-safe: only show metadata to avoid exposing payload data
     */
    @Override
    public String toString() {
        return "DltEventEntity{dltEventId=%s, serviceName=%s, topic=%s, addToDltTimestamp=%s}"
                .formatted(dltEventId, serviceName, topic, addToDltTimestamp);
    }
    
    // Builder
    public static Builder builder() { return new Builder(); }
    public Builder toBuilder() { return new Builder(this); }
    
    public static final class Builder {
        private String dltEventId;
        private String originalEventId;
        private String serviceName;
        private LocalDateTime addToDltTimestamp;
        private String topic;
        private String partition;
        @Nullable private String traceId;
        private String payload;
        private String payloadMediaType;
        @Nullable private String error;
        @Nullable private String stackTrace;
        
        private Builder() {}
        
        private Builder(DltEventEntity source) {
            this.dltEventId = source.dltEventId;
            this.originalEventId = source.originalEventId;
            this.serviceName = source.serviceName;
            this.addToDltTimestamp = source.addToDltTimestamp;
            this.topic = source.topic;
            this.partition = source.partition;
            this.traceId = source.traceId;
            this.payload = source.payload;
            this.payloadMediaType = source.payloadMediaType;
            this.error = source.error;
            this.stackTrace = source.stackTrace;
        }
        
        public Builder dltEventId(String value) { this.dltEventId = value; return this; }
        public Builder originalEventId(String value) { this.originalEventId = value; return this; }
        public Builder serviceName(String value) { this.serviceName = value; return this; }
        public Builder addToDltTimestamp(LocalDateTime value) { this.addToDltTimestamp = value; return this; }
        public Builder topic(String value) { this.topic = value; return this; }
        public Builder partition(String value) { this.partition = value; return this; }
        public Builder traceId(String value) { this.traceId = value; return this; }
        public Builder payload(String value) { this.payload = value; return this; }
        public Builder payloadMediaType(String value) { this.payloadMediaType = value; return this; }
        public Builder error(String value) { this.error = value; return this; }
        public Builder stackTrace(String value) { this.stackTrace = value; return this; }
        
        public DltEventEntity build() { return new DltEventEntity(this); }
    }
}
