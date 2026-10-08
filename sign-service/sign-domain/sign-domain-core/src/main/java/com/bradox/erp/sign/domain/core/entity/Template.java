package com.bradox.erp.sign.domain.core.entity;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import com.bradox.erp.sign.domain.core.valueobject.FieldType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A reusable PDF with signer roles and placed fields (SIG-01). */
public class Template {

    private final UUID id;
    private final CompanyId companyId;
    private String name;
    private UUID documentId;
    private UUID documentVersionId;
    private String documentSha256;
    private int pageCount;
    private boolean active;
    private int defaultValidityDays;
    private String defaultMessage;
    private String category;
    private List<TemplateRole> roles = new ArrayList<>();
    private List<TemplateField> fields = new ArrayList<>();
    private final Instant createdAt;
    private final String createdBy;

    public Template(UUID id, CompanyId companyId, String name, UUID documentId, UUID documentVersionId, String documentSha256,
                    int pageCount, boolean active, int defaultValidityDays, String defaultMessage, String category,
                    Instant createdAt, String createdBy) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.documentId = documentId;
        this.documentVersionId = documentVersionId;
        this.documentSha256 = documentSha256;
        this.pageCount = pageCount;
        this.active = active;
        this.defaultValidityDays = defaultValidityDays;
        this.defaultMessage = defaultMessage;
        this.category = category;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    /** Roles and fields replace the whole layout in one edit; existing requests keep their own copies (BR-SIG-14). */
    public void update(String name, int defaultValidityDays, String defaultMessage, String category, List<TemplateRole> roles,
                       List<TemplateField> fields, boolean active) {
        if (name == null || name.isBlank()) {
            throw new SignDomainException("error.sign.template.name", null, "A template needs a name");
        }
        if (defaultValidityDays < 1 || defaultValidityDays > SignRequestLimits.MAX_VALIDITY_DAYS) {
            throw new SignDomainException("error.sign.validity", new Object[]{SignRequestLimits.MAX_VALIDITY_DAYS},
                    "Validity must be between 1 and " + SignRequestLimits.MAX_VALIDITY_DAYS + " days");
        }
        this.name = name.trim();
        this.defaultValidityDays = defaultValidityDays;
        this.defaultMessage = defaultMessage;
        this.category = category;
        this.roles = new ArrayList<>(roles);
        this.fields = new ArrayList<>(fields);
        this.active = false;
        if (active) {
            validateForActivation();
            this.active = true;
        } else {
            validateLayout();
        }
    }

    /** Replaces the PDF. Fields are kept only when the page count is unchanged (SIG-01 #7), otherwise they are dropped. */
    public boolean replaceDocument(UUID documentId, UUID versionId, String sha256, int pageCount) {
        boolean keep = pageCount == this.pageCount;
        this.documentId = documentId;
        this.documentVersionId = versionId;
        this.documentSha256 = sha256;
        this.pageCount = pageCount;
        if (!keep) {
            this.fields = new ArrayList<>();
            this.active = false;
        }
        return keep;
    }

    /** Loading from storage only: no validation, because an archived or half-built template may be incomplete. */
    public void restoreLayout(List<TemplateRole> roles, List<TemplateField> fields) {
        this.roles = new ArrayList<>(roles);
        this.fields = new ArrayList<>(fields);
    }

    public void archive() {
        this.active = false;
    }

    /** Cheap structural checks that hold even for a draft layout. */
    public void validateLayout() {
        Set<String> names = new HashSet<>();
        Set<UUID> roleIds = new HashSet<>();
        for (TemplateRole r : roles) {
            if (r.getName() == null || r.getName().isBlank()) {
                throw new SignDomainException("error.sign.role.name", null, "Every role needs a name");
            }
            if (!names.add(r.getName().trim().toLowerCase())) {
                throw new SignDomainException("error.sign.role.duplicate", new Object[]{r.getName()},
                        "Two roles are called " + r.getName());
            }
            roleIds.add(r.getId());
        }
        for (TemplateField f : fields) {
            if (!roleIds.contains(f.getRoleId())) {
                throw new SignDomainException("error.sign.field.role", null, "A field belongs to a role that does not exist");
            }
            if (!f.getGeometry().within(pageCount)) {
                throw new SignDomainException("error.sign.field.pageRange", new Object[]{pageCount},
                        "A field is on a page the document does not have (it has " + pageCount + " pages)");
            }
        }
    }

    /** BR-SIG-02: every role needs a required signature, unless the role only approves. */
    public void validateForActivation() {
        validateLayout();
        if (roles.isEmpty()) {
            throw new SignDomainException("error.sign.template.noRoles", null, "Add at least one signer role first");
        }
        for (TemplateRole r : roles) {
            boolean hasSignature = fields.stream().anyMatch(f -> f.getRoleId().equals(r.getId()) && f.isRequired()
                    && (f.getType() == FieldType.SIGNATURE));
            if (!r.isApproverOnly() && !hasSignature) {
                throw new SignDomainException("error.sign.role.noSignature", new Object[]{r.getName()},
                        "Role " + r.getName() + " needs a required signature field");
            }
        }
    }

    public UUID getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public String getName() { return name; }
    public UUID getDocumentId() { return documentId; }
    public UUID getDocumentVersionId() { return documentVersionId; }
    public String getDocumentSha256() { return documentSha256; }
    public int getPageCount() { return pageCount; }
    public boolean isActive() { return active; }
    public int getDefaultValidityDays() { return defaultValidityDays; }
    public String getDefaultMessage() { return defaultMessage; }
    public String getCategory() { return category; }
    public List<TemplateRole> getRoles() { return List.copyOf(roles); }
    public List<TemplateField> getFields() { return List.copyOf(fields); }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
