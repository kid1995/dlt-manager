CREATE TABLE IF NOT EXISTS dlt_event (
    dlt_event_id                TEXT PRIMARY KEY,
    original_event_id           TEXT NOT NULL,
    service_name                TEXT NOT NULL,
    add_to_dlt_timestamp        TIMESTAMP NOT NULL,
    kafka_topic                 TEXT,
    kafka_partition             TEXT,
    trace_id                    TEXT,
    payload                     TEXT NOT NULL,
    payload_media_type          TEXT NOT NULL,
    payload_status              VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    payload_validation_error    TEXT,
    error                       TEXT,
    stack_trace                 TEXT
);

CREATE INDEX IF NOT EXISTS idx_dlt_event_service_topic
    ON dlt_event (service_name, kafka_topic);

CREATE INDEX IF NOT EXISTS idx_dlt_event_add_to_dlt_timestamp
    ON dlt_event (add_to_dlt_timestamp);

CREATE INDEX IF NOT EXISTS idx_dlt_event_trace_id
    ON dlt_event (trace_id) WHERE trace_id IS NOT NULL;


CREATE TABLE IF NOT EXISTS admin_action_history (
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY NOT NULL,
    dlt_event_id        TEXT NOT NULL REFERENCES dlt_event(dlt_event_id) ON DELETE CASCADE,
    user_name           TEXT NOT NULL,
    performed_at        TIMESTAMP NOT NULL,
    action_name         TEXT NOT NULL,
    action_details      TEXT,
    action_status       VARCHAR(50) NOT NULL,
    status_error        TEXT
);

CREATE INDEX IF NOT EXISTS idx_admin_action_event_timestamp
    ON admin_action_history (dlt_event_id, performed_at);
