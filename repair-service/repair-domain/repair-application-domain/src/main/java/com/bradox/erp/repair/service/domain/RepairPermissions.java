package com.bradox.erp.repair.service.domain;

/** Permission codes of the Repair module (see the permission table of EPIC-REP). */
public final class RepairPermissions {
    public static final String ORDER_VIEW = "rep.order.view";
    public static final String DIAGNOSIS_EDIT = "rep.diagnosis.edit";
    public static final String LINE_EDIT = "rep.line.edit";
    public static final String PRICE_VIEW = "rep.price.view";
    public static final String PACKAGE_MANAGE = "rep.package.manage";
    public static final String LABOR_GUIDE_MANAGE = "rep.labor_guide.manage";
    public static final String DISCOUNT_APPROVE = "rep.discount.approve";
    public static final String SETTINGS_MANAGE = "rep.settings.manage";

    private RepairPermissions() {
    }
}
