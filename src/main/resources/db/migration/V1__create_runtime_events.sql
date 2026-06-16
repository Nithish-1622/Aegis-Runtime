CREATE TABLE runtime_events (
    event_id VARCHAR(255) PRIMARY KEY,
    trace_id VARCHAR(255),
    span_id VARCHAR(255),
    parent_span_id VARCHAR(255),
    service_name VARCHAR(255),
    instance_id VARCHAR(255),
    event_type VARCHAR(100) NOT NULL,
    "timestamp" TIMESTAMP WITH TIME ZONE,
    thread_name VARCHAR(255),
    thread_id BIGINT,
    latency INTERVAL,
    status VARCHAR(50),
    payload_json TEXT
);
