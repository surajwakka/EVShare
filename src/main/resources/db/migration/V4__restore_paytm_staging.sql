-- Restore payment metadata after the earlier no-payment V3 release.
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_order_id VARCHAR(80);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS paytm_txn_id VARCHAR(100);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_status VARCHAR(32) NOT NULL DEFAULT 'NOT_INITIATED';
-- Existing reservations remain confirmed; only new bookings require Paytm checkout.
