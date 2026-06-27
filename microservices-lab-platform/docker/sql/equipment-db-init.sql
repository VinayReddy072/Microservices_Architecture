-- ============================================================
-- equipment_db Schema — Equipment Service Database
-- PostgreSQL DDL
-- Database: equipment_db
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE SEQUENCE IF NOT EXISTS equipment_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- ============================================================
-- EQUIPMENT TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS equipment
(
    id                   BIGINT                      DEFAULT nextval('equipment_sequence') PRIMARY KEY,
    name                 VARCHAR(200)                NOT NULL,
    description          TEXT,
    category             VARCHAR(50)                 NOT NULL
                             CHECK (category IN (
                                 'ELECTRONIC_MEASUREMENT',
                                 'AERIAL_SYSTEMS',
                                 'FPGA_EMBEDDED',
                                 'ROBOTICS',
                                 'OPTICS_MICROSCOPY',
                                 'SENSORS_IOT',
                                 'MECHANICAL',
                                 'COMPUTING',
                                 'OTHER'
                             )),
    status               VARCHAR(30)                 NOT NULL DEFAULT 'AVAILABLE'
                             CHECK (status IN (
                                 'AVAILABLE',
                                 'BOOKED',
                                 'UNDER_MAINTENANCE',
                                 'OUT_OF_SERVICE',
                                 'DECOMMISSIONED'
                             )),
    serial_number        VARCHAR(100)                UNIQUE,
    location             VARCHAR(200),
    usage_count          INTEGER                     NOT NULL DEFAULT 0
                             CHECK (usage_count >= 0),
    maintenance_required BOOLEAN                     NOT NULL DEFAULT FALSE,
    last_maintenance_at  TIMESTAMP WITHOUT TIME ZONE,
    created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_equipment_status
    ON equipment (status);

CREATE INDEX IF NOT EXISTS idx_equipment_category
    ON equipment (category);

CREATE INDEX IF NOT EXISTS idx_equipment_maintenance
    ON equipment (maintenance_required)
    WHERE maintenance_required = TRUE;

CREATE INDEX IF NOT EXISTS idx_equipment_available
    ON equipment (status, maintenance_required)
    WHERE status = 'AVAILABLE' AND maintenance_required = FALSE;

-- ============================================================
-- TRIGGER: Auto-update updated_at
-- ============================================================
CREATE OR REPLACE FUNCTION update_equipment_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER equipment_updated_at_trigger
    BEFORE UPDATE ON equipment
    FOR EACH ROW
EXECUTE FUNCTION update_equipment_updated_at();

-- ============================================================
-- SEED DATA — Real university lab equipment
-- ============================================================
INSERT INTO equipment (name, description, category, status, serial_number, location, usage_count, maintenance_required)
VALUES
    ('Tektronix MDO3054 Oscilloscope',
     '500 MHz 4-channel mixed domain oscilloscope with spectrum analyser',
     'ELECTRONIC_MEASUREMENT', 'AVAILABLE', 'TEK-MDO3054-001', 'Lab 101 - Electronics Bay A', 3, FALSE),

    ('DJI Matrice 300 RTK Drone',
     'Enterprise industrial drone with RTK positioning and 55-min flight time',
     'AERIAL_SYSTEMS', 'AVAILABLE', 'DJI-M300-001', 'Lab 205 - Drone Bay', 7, FALSE),

    ('Xilinx Artix-7 FPGA Kit',
     'Nexys A7 FPGA trainer board — 15,850 logic cells, 256KB BRAM',
     'FPGA_EMBEDDED', 'AVAILABLE', 'XIL-ARTIX7-001', 'Lab 102 - Embedded Systems', 2, FALSE),

    ('Universal Robots UR5e Arm',
     '6-DOF collaborative robot arm, 5kg payload, 850mm reach',
     'ROBOTICS', 'AVAILABLE', 'UR5E-001', 'Lab 301 - Robotics Cell', 9, FALSE),

    ('Zeiss Axio Observer Microscope',
     'Inverted research microscope with phase contrast and fluorescence',
     'OPTICS_MICROSCOPY', 'AVAILABLE', 'ZEISS-AO-001', 'Lab 401 - Biology Lab', 1, FALSE),

    ('Raspberry Pi 4 Sensor Kit',
     'Complete IoT kit with Pi 4, temperature, humidity, motion, and gas sensors',
     'SENSORS_IOT', 'AVAILABLE', 'RPI4-KIT-001', 'Lab 102 - Embedded Systems', 12, TRUE),

    ('Prusa i3 MK3S+ 3D Printer',
     'FDM 3D printer supporting PLA, PETG, ABS, Flex materials',
     'MECHANICAL', 'UNDER_MAINTENANCE', 'PRUSA-MK3S-001', 'Lab 501 - Fabrication', 15, TRUE),

    ('NVIDIA RTX 4090 Workstation',
     'High-performance GPU workstation for ML/DL training (128GB RAM)',
     'COMPUTING', 'AVAILABLE', 'GPU-WS-RTX4090-001', 'Lab 202 - AI Lab', 4, FALSE),

    ('Keysight N9010B Signal Analyzer',
     '26.5 GHz signal analyzer for RF and microwave measurements',
     'ELECTRONIC_MEASUREMENT', 'AVAILABLE', 'KEY-N9010B-001', 'Lab 101 - Electronics Bay B', 0, FALSE),

    ('Boston Dynamics Spot Robot',
     'Agile quadruped robot for autonomous inspection and research',
     'ROBOTICS', 'AVAILABLE', 'BD-SPOT-001', 'Lab 301 - Robotics Cell', 5, FALSE)
ON CONFLICT (serial_number) DO NOTHING;

-- ============================================================
-- ERD Summary:
--
-- equipment
-- ├── id (PK, sequence)
-- ├── name (NOT NULL)
-- ├── description (TEXT)
-- ├── category (ENUM)
-- ├── status (ENUM: AVAILABLE, BOOKED, UNDER_MAINTENANCE, OUT_OF_SERVICE, DECOMMISSIONED)
-- ├── serial_number (UNIQUE)
-- ├── location
-- ├── usage_count (incremented by RabbitMQ consumer)
-- ├── maintenance_required (auto-flagged when usage_count >= threshold)
-- ├── last_maintenance_at
-- ├── created_at (audit)
-- └── updated_at (audit, auto-updated by trigger)
-- ============================================================
