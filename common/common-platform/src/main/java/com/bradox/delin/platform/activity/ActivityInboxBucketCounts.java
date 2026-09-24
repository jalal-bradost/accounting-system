package com.bradox.delin.platform.activity;

public record ActivityInboxBucketCounts(int late, int today, int future) {

    public static ActivityInboxBucketCounts empty() {
        return new ActivityInboxBucketCounts(0, 0, 0);
    }

    public int total() {
        return late + today + future;
    }

    public ActivityInboxBucketCounts increment(ActivityInboxBucket bucket) {
        return switch (bucket) {
            case LATE -> new ActivityInboxBucketCounts(late + 1, today, future);
            case TODAY -> new ActivityInboxBucketCounts(late, today + 1, future);
            case FUTURE -> new ActivityInboxBucketCounts(late, today, future + 1);
        };
    }
}
