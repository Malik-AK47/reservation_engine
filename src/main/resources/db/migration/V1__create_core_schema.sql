CREATE TABLE app_users (
                           id UUID PRIMARY KEY,
                           name VARCHAR(120) NOT NULL,
                           normalized_email VARCHAR(254) NOT NULL,
                           password_hash VARCHAR(255) NOT NULL,
                           role VARCHAR(20) NOT NULL,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT uq_app_users_normalized_email
                               UNIQUE (normalized_email),
                           CONSTRAINT ck_app_users_role
                               CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE events (
                        id UUID PRIMARY KEY,
                        name VARCHAR(200) NOT NULL,
                        description TEXT,
                        location VARCHAR(255) NOT NULL,
                        start_time TIMESTAMPTZ NOT NULL,
                        end_time TIMESTAMPTZ NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        cancellation_cutoff_minutes INTEGER NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT ck_events_time_range
                            CHECK (end_time > start_time),
                        CONSTRAINT ck_events_cancellation_cutoff
                            CHECK (cancellation_cutoff_minutes >= 0),
                        CONSTRAINT ck_events_status
                            CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'))
);

CREATE TABLE resources (
                           id UUID PRIMARY KEY,
                           event_id UUID NOT NULL,
                           resource_code VARCHAR(100) NOT NULL,
                           status VARCHAR(20) NOT NULL,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_resources_event
                               FOREIGN KEY (event_id)
                                   REFERENCES events (id) ON DELETE RESTRICT,
                           CONSTRAINT uq_resources_event_code
                               UNIQUE (event_id, resource_code),
                           CONSTRAINT ck_resources_status
                               CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED'))
);

CREATE INDEX ix_resources_event_id
    ON resources (event_id);

CREATE TABLE reservations (
                              id UUID PRIMARY KEY,
                              user_id UUID NOT NULL,
                              resource_id UUID NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              held_at TIMESTAMPTZ NOT NULL,
                              expires_at TIMESTAMPTZ NOT NULL,
                              confirmed_at TIMESTAMPTZ,
                              cancelled_at TIMESTAMPTZ,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_reservations_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES app_users (id) ON DELETE RESTRICT,
                              CONSTRAINT fk_reservations_resource
                                  FOREIGN KEY (resource_id)
                                      REFERENCES resources (id) ON DELETE RESTRICT,
                              CONSTRAINT ck_reservations_status
                                  CHECK (status IN ('HELD', 'CONFIRMED', 'EXPIRED', 'CANCELLED')),
                              CONSTRAINT ck_reservations_expiry_after_hold
                                  CHECK (expires_at > held_at)
);

-- Only one active reservation may exist for a resource.
-- Expired holds must be transitioned to EXPIRED; time-dependent
-- predicates do not belong in this partial index.
CREATE UNIQUE INDEX uq_reservations_one_active_per_resource
    ON reservations (resource_id)
    WHERE status IN ('HELD', 'CONFIRMED');

CREATE INDEX ix_reservations_user_created_at
    ON reservations (user_id, created_at);

CREATE INDEX ix_reservations_held_expires_at
    ON reservations (expires_at)
    WHERE status = 'HELD';

CREATE TABLE idempotency_records (
                                     id UUID PRIMARY KEY,
                                     user_id UUID NOT NULL,
                                     operation VARCHAR(50) NOT NULL,
                                     key VARCHAR(255) NOT NULL,
                                     payload_hash VARCHAR(64) NOT NULL,
                                     result_status INTEGER NOT NULL,
                                     result_reservation_status VARCHAR(20) NOT NULL,
                                     result_expires_at TIMESTAMPTZ NOT NULL,
                                     reservation_id UUID NOT NULL,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     expires_at TIMESTAMPTZ NOT NULL,

                                     CONSTRAINT fk_idempotency_records_user
                                         FOREIGN KEY (user_id)
                                             REFERENCES app_users (id) ON DELETE RESTRICT,
                                     CONSTRAINT fk_idempotency_records_reservation
                                         FOREIGN KEY (reservation_id)
                                             REFERENCES reservations (id) ON DELETE RESTRICT,
                                     CONSTRAINT uq_idempotency_records_scope
                                         UNIQUE (user_id, operation, key),
                                     CONSTRAINT ck_idempotency_records_payload_hash
                                         CHECK (payload_hash ~ '^[0-9a-fA-F]{64}$'),
    CONSTRAINT ck_idempotency_records_result_status
        CHECK (result_status BETWEEN 100 AND 599),
    CONSTRAINT ck_idempotency_records_result_reservation_status
        CHECK (result_reservation_status = 'HELD'),
    CONSTRAINT ck_idempotency_records_expiry
        CHECK (expires_at > created_at)
);

CREATE INDEX ix_idempotency_records_expires_at
    ON idempotency_records (expires_at);
