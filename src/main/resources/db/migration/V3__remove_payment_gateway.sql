-- EVShare no longer processes payments. Keep charger pricing as an estimate only.
ALTER TABLE bookings DROP COLUMN IF EXISTS payment_order_id;
ALTER TABLE bookings DROP COLUMN IF EXISTS paytm_txn_id;
ALTER TABLE bookings DROP COLUMN IF EXISTS payment_status;
UPDATE bookings SET status = 'CONFIRMED' WHERE status = 'PENDING_PAYMENT';
