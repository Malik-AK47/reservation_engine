
CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_app_users_email_lower ON app_users (LOWER(email));

CREATE TABLE events (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_events_time
    CHECK (ends_at > starts_at)
);

CREATE TABLE resources (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id),
    code VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_resources_event_code
    UNIQUE (event_id, code),

    CONSTRAINT chk_resources_status
    CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED'))
);

CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    resource_id UUID NOT NULL REFERENCES resources(id),
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_reservations_status
    CHECK (status IN ('HELD', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),

    CONSTRAINT chk_held_has_expiry
    CHECK (status <> 'HELD' OR expires_at IS NOT NULL)
);

CREATE UNIQUE INDEX uq_active_reservation_per_resource
    ON reservations (resource_id)
    WHERE status IN ('HELD', 'CONFIRMED');

CREATE INDEX idx_reservations_user_created
    ON reservations (user_id, created_at DESC);

CREATE INDEX idx_reservations_expiry
    ON reservations (expires_at)
    WHERE status = 'HELD';

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    operation VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INTEGER,
    response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_idempotency_scope_key
    UNIQUE (user_id, operation, idempotency_key)
);
