package com.bradox.erp.documents.domain.core.entity;

import com.bradox.erp.documents.domain.core.rule.FolderTreeRules;
import com.bradox.erp.documents.domain.core.valueobject.TagFacetId;
import com.bradox.erp.domain.entity.AggregateRoot;
import com.bradox.erp.domain.valueobject.CompanyId;

/** A tag category, for example "Status" or "Type". Tags in one facet are OR-ed, facets are AND-ed. */
public class TagFacet extends AggregateRoot<TagFacetId> {

    private CompanyId companyId;
    private String name;
    private int sequence;

    private TagFacet() {
    }

    public static TagFacet create(TagFacetId id, CompanyId companyId, String name, int sequence) {
        FolderTreeRules.validateName(name);
        TagFacet f = new TagFacet();
        f.setId(id);
        f.companyId = companyId;
        f.name = name.trim();
        f.sequence = sequence;
        return f;
    }

    public void update(String name, int sequence) {
        FolderTreeRules.validateName(name);
        this.name = name.trim();
        this.sequence = sequence;
    }

    public CompanyId getCompanyId() { return companyId; }
    public String getName() { return name; }
    public int getSequence() { return sequence; }
}
