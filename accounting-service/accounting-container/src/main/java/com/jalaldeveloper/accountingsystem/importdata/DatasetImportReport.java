package com.jalaldeveloper.accountingsystem.importdata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatasetImportReport {

    private final Map<String, Integer> created = new LinkedHashMap<>();
    private final Map<String, Integer> skipped = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public void created(String type) {
        created.merge(type, 1, Integer::sum);
    }

    public void skipped(String type, String reason) {
        skipped.merge(type, 1, Integer::sum);
        warnings.add(reason);
    }

    public void warn(String message) {
        warnings.add(message);
    }

    public void error(String message) {
        errors.add(message);
    }

    public Map<String, Integer> getCreated() { return created; }
    public Map<String, Integer> getSkipped() { return skipped; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getErrors() { return errors; }
}
