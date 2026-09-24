package com.bradox.delin.platform.activity;

import java.time.LocalDate;
import java.util.List;

public record ActivityInboxSummaryResponse(
        LocalDate asOfDate,
        ActivityInboxBucketCounts totals,
        List<ActivityInboxModelGroup> groups) {}
