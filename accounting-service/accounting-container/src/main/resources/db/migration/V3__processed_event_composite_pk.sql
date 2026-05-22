ALTER TABLE platform_processed_event DROP PRIMARY KEY;
ALTER TABLE platform_processed_event ADD PRIMARY KEY (event_id, consumer_name);
