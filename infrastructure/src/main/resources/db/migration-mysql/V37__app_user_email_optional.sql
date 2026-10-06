-- Allow creating salespeople (and other users) without an email address.
ALTER TABLE platform_app_user MODIFY COLUMN email VARCHAR(255) NULL;
