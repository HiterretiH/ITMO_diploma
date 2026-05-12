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

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    short_name TEXT NOT NULL,
    full_name TEXT,
    phone TEXT,
    requisites TEXT
);

CREATE INDEX idx_customers_owner ON customers (owner_id);

CREATE TABLE customer_route_hints (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    kind VARCHAR(16) NOT NULL,
    place_key VARCHAR(64) NOT NULL,
    place_text TEXT NOT NULL,
    contact_text TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (customer_id, kind, place_key)
);

CREATE INDEX idx_route_hints_customer_kind ON customer_route_hints (customer_id, kind);

CREATE TABLE performers (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    short_name TEXT NOT NULL,
    full_name TEXT,
    phone TEXT,
    bank_name TEXT,
    inn TEXT,
    bik TEXT,
    kpp TEXT,
    payment_account TEXT,
    corr_account TEXT,
    requisites TEXT
);

CREATE INDEX idx_performers_owner ON performers (owner_id);

CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES performers (id) ON DELETE CASCADE,
    brand_model TEXT,
    plate_number TEXT,
    type TEXT
);

CREATE INDEX idx_vehicles_owner ON vehicles (owner_id);

CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    employer_id BIGINT NOT NULL REFERENCES performers (id) ON DELETE CASCADE,
    full_name TEXT,
    phone TEXT
);

CREATE INDEX idx_drivers_employer ON drivers (employer_id);

CREATE TABLE user_trip_defaults (
    user_id BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    last_performer_id BIGINT REFERENCES performers (id) ON DELETE SET NULL,
    last_driver_id BIGINT REFERENCES drivers (id) ON DELETE SET NULL,
    last_vehicle_id BIGINT REFERENCES vehicles (id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES customers (id),
    performer_id BIGINT NOT NULL REFERENCES performers (id),
    vehicle_id BIGINT REFERENCES vehicles (id),
    driver_id BIGINT REFERENCES drivers (id),
    order_number INTEGER NOT NULL,
    order_date DATE NOT NULL,
    loading_place TEXT NOT NULL,
    loading_contact TEXT,
    unloading_place TEXT NOT NULL,
    unloading_contact TEXT,
    trip_count INTEGER NOT NULL DEFAULT 1,
    price_per_trip NUMERIC(10, 2),
    total_price NUMERIC(10, 2),
    template_version INTEGER NOT NULL DEFAULT 1,
    completed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_orders_owner ON orders (owner_id);
CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_performer ON orders (performer_id);
CREATE INDEX idx_orders_vehicle ON orders (vehicle_id);
CREATE INDEX idx_orders_driver ON orders (driver_id);

CREATE TABLE generated_documents (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    document_type VARCHAR(32) NOT NULL,
    file_format VARCHAR(16) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    content_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (order_id, document_type, file_format)
);

CREATE INDEX idx_generated_documents_order ON generated_documents (order_id);

CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users (id),
    order_id BIGINT REFERENCES orders (id) ON DELETE SET NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_order ON audit_events (order_id);
CREATE INDEX idx_audit_user ON audit_events (user_id);
