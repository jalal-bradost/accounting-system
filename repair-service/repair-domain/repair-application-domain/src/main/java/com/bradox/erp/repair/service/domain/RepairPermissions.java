package com.bradox.erp.repair.service.domain;

/** Permission codes of the Repair module. */
public final class RepairPermissions {
    public static final String ORDER_VIEW = "rep.order.view";
    /** Create, change, confirm and cancel repair orders. */
    public static final String ORDER_EDIT = "rep.line.edit";

    private RepairPermissions() {
    }
}
