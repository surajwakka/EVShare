CREATE TABLE IF NOT EXISTS chargers (
 id BIGSERIAL PRIMARY KEY,
 title VARCHAR(160) NOT NULL,
 host_name VARCHAR(120) NOT NULL,
 address VARCHAR(500) NOT NULL,
 city VARCHAR(120),
 charger_type VARCHAR(80) NOT NULL,
 power_kw NUMERIC(8,2) NOT NULL CHECK (power_kw > 0),
 price_per_kwh NUMERIC(10,2) NOT NULL CHECK (price_per_kwh >= 1),
 connector_type VARCHAR(80),
 available_from VARCHAR(5),
 available_to VARCHAR(5),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 latitude DOUBLE PRECISION NOT NULL DEFAULT 0,
 longitude DOUBLE PRECISION NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_chargers_city_active ON chargers(city, active);
CREATE TABLE IF NOT EXISTS bookings (
 id BIGSERIAL PRIMARY KEY,
 charger_id BIGINT NOT NULL REFERENCES chargers(id),
 customer_name VARCHAR(120) NOT NULL,
 customer_email VARCHAR(254) NOT NULL,
 booking_date DATE NOT NULL,
 start_time TIME NOT NULL,
 end_time TIME NOT NULL,
 estimated_units NUMERIC(10,2) NOT NULL,
 amount NUMERIC(12,2) NOT NULL,
 status VARCHAR(32) NOT NULL,
 payment_order_id VARCHAR(64) NOT NULL UNIQUE
);
CREATE INDEX IF NOT EXISTS idx_bookings_charger_slot ON bookings(charger_id, booking_date, status);
