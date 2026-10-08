package com.bradox.erp.sign.service.domain;

/** Permission codes of the Sign module. External signers use none: their access is the link token only. */
public final class SignPermissions {

    public static final String TEMPLATE_VIEW = "sign.template.view";
    public static final String TEMPLATE_MANAGE = "sign.template.manage";
    public static final String REQUEST_CREATE = "sign.request.create";
    public static final String REQUEST_VIEW = "sign.request.view";
    public static final String REQUEST_VIEW_ALL = "sign.request.view_all";
    public static final String REQUEST_CANCEL = "sign.request.cancel";
    public static final String SIGN_SELF = "sign.sign_self";
    public static final String AUDIT_VIEW = "sign.audit.view";
    public static final String SETTINGS_MANAGE = "sign.settings.manage";

    private SignPermissions() {
    }
}
