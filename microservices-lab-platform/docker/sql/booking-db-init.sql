-- ============================================================
-- booking_db Schema — Booking Service Database
-- PostgreSQL DDL
-- Database: booking_db
-- ============================================================

-- Extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Sequence for Booking IDs
CREATE SEQUENCE IF NOT EXISTS booking_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- ============================================================
-- BOOKINGS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS bookings
(
    id               BIGINT                   DEFAULT nextval('booking_sequence') PRIMARY KEY,
    equipment_id     BIGINT                   NOT NULL,
    user_id          VARCHAR(100)             NOT NULL,
    start_time       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status           VARCHAR(20)              NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    notes            TEXT,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

    -- Business rule: end_time must be after start_time
    CONSTRAINT chk_booking_time_window CHECK (end_time > start_time),

    -- Business rule: booking cannot be in the past (enforced at application layer too)
    CONSTRAINT chk_booking_start_future CHECK (start_time > created_at - INTERVAL '1 minute')
);

-- ============================================================
-- INDEXES
-- ============================================================

-- Fast lookup by equipment for overlap detection
CREATE INDEX IF NOT EXISTS idx_booking_equipment_id
    ON bookings (equipment_id);

-- Fast lookup by user
CREATE INDEX IF NOT EXISTS idx_booking_user_id
    ON bookings (user_id);

-- Status filtering
CREATE INDEX IF NOT EXISTS idx_booking_status
    ON bookings (status);

-- Composite index for overlap detection query
-- (equipment_id, start_time, end_time) — covers the JPQL overlap query
CREATE INDEX IF NOT EXISTS idx_booking_overlap_detection
    ON bookings (equipment_id, start_time, end_time)
    WHERE status NOT IN ('CANCELLED', 'COMPLETED');

-- ============================================================
-- TRIGGER: Auto-update updated_at timestamp
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER bookings_updated_at_trigger
    BEFORE UPDATE ON bookings
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- SEED DATA — Sample bookings for development
-- ============================================================
INSERT INTO bookings (equipment_id, user_id, start_time, end_time, status, notes)
VALUES
    (1, 'student', NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day' + INTERVAL '2 hours', 'CONFIRMED', 'Oscilloscope for lab 3 experiment'),
    (2, 'researcher', NOW() + INTERVAL '2 days', NOW() + INTERVAL '2 days' + INTERVAL '4 hours', 'CONFIRMED', 'Drone calibration session'),
    (3, 'student', NOW() + INTERVAL '3 days', NOW() + INTERVAL '3 days' + INTERVAL '1 hour', 'PENDING', 'FPGA programming project')
ON CONFLICT DO NOTHING;

-- ============================================================
-- ERD Summary:
--
-- bookings
-- ├── id (PK, sequence)
-- ├── equipment_id (FK reference to equipment_db — no FK constraint, cross-DB)
-- ├── user_id (FK reference to auth system — no FK constraint)
-- ├── start_time
-- ├── end_time
-- ├── status (ENUM: PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED)
-- ├── notes
-- ├── created_at (audit)
-- └── updated_at (audit, auto-updated by trigger)
-- ============================================================
