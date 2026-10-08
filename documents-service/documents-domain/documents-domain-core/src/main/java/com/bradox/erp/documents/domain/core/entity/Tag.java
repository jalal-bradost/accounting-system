package com.bradox.erp.documents.domain.core.entity;

import com.bradox.erp.documents.domain.core.rule.FolderTreeRules;
import com.bradox.erp.documents.domain.core.valueobject.TagFacetId;
import com.bradox.erp.documents.domain.core.valueobject.TagId;
import com.bradox.erp.domain.entity.AggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;

public class Tag extends AggregateRoot<TagId> {

    private CompanyId companyId;
    private TagFacetId facetId;
    private String name;
    private String color;
    private int sequence;

    private Tag() {
    }

    public static Tag create(TagId id, CompanyId companyId, TagFacetId facetId, String name, String color, int sequence) {
        FolderTreeRules.validateName(name);
        Tag t = new Tag();
        t.setId(id);
        t.companyId = companyId;
        t.facetId = facetId;
        t.name = name.trim();
        t.color = color == null || color.isBlank() ? "#64748b" : color.trim();
        t.sequence = sequence;
        return t;
    }

    public void update(String name, String color, int sequence) {
        FolderTreeRules.validateName(name);
        this.name = name.trim();
        this.color = color == null || color.isBlank() ? this.color : color.trim();
        this.sequence = sequence;
    }

    public CompanyId getCompanyId() { return companyId; }
    public TagFacetId getFacetId() { return facetId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public int getSequence() { return sequence; }
}
