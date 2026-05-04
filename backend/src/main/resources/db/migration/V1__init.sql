CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE counterparties (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(512) NOT NULL,
    inn VARCHAR(12),
    legal_address VARCHAR(1024),
    phone VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_counterparties_owner ON counterparties (owner_id);

CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    license_number VARCHAR(64) NOT NULL,
    license_category VARCHAR(16),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_drivers_owner ON drivers (owner_id);

CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plate_number VARCHAR(32) NOT NULL,
    model VARCHAR(255),
    load_capacity_kg INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vehicles_owner ON vehicles (owner_id);

CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    shipper_id BIGINT REFERENCES counterparties (id),
    consignee_id BIGINT REFERENCES counterparties (id),
    cargo_description VARCHAR(2048),
    cargo_weight_kg NUMERIC(14, 3),
    route_from VARCHAR(512),
    route_to VARCHAR(512),
    load_date DATE,
    unload_date DATE,
    driver_id BIGINT REFERENCES drivers (id),
    vehicle_id BIGINT REFERENCES vehicles (id),
    price_amount NUMERIC(14, 2),
    currency VARCHAR(8) DEFAULT 'RUB',
    snapshot_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_trips_owner_status ON trips (owner_id, status);

CREATE TABLE generated_documents (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    document_type VARCHAR(32) NOT NULL,
    file_format VARCHAR(16) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    content_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (trip_id, document_type, file_format)
);

CREATE INDEX idx_generated_documents_trip ON generated_documents (trip_id);

CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users (id),
    trip_id BIGINT REFERENCES trips (id),
    event_type VARCHAR(64) NOT NULL,
    payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_trip ON audit_events (trip_id);
CREATE INDEX idx_audit_user ON audit_events (user_id);
