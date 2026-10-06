-- Allow creating salespeople (and other users) without an email address.
ALTER TABLE platform_app_user ALTER COLUMN email DROP NOT NULL;
