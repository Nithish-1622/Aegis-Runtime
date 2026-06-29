CREATE TABLE IF NOT EXISTS runtime_anomalies (
    anomaly_id UUID PRIMARY KEY,
    trace_id VARCHAR(255),
    service_name VARCHAR(255),
    anomaly_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    description TEXT,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    metadata_json TEXT
);
