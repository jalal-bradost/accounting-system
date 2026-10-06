-- Optional map coordinates on partner addresses
ALTER TABLE contacts_partner_address
    ADD COLUMN latitude DECIMAL(10, 7) NULL,
    ADD COLUMN longitude DECIMAL(10, 7) NULL;
