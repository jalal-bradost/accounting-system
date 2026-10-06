package com.bradox.erp.inventory.service.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class OpeningStockReport {
    private int applied;
    private int skipped;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public int getApplied() { return applied; }
    public void setApplied(int applied) { this.applied = applied; }
    public void incrementApplied() { this.applied++; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }
    public void incrementSkipped() { this.skipped++; }

    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }

    public void error(String message) { errors.add(message); }
    public void warning(String message) { warnings.add(message); }
}
