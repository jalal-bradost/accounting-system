-- Optional map coordinates on partner addresses
ALTER TABLE contacts_partner_address ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 7);
ALTER TABLE contacts_partner_address ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7);
