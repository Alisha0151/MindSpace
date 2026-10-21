package com.mindspace.checkin;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

/** A value in check_in_tags (composite key: check-in id + type + label). Ownership is inherited from the check-in. */
@Embeddable
public class CheckInTag {

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false, length = 10)
    private TagType tagType;

    @Column(name = "label", nullable = false, length = 50)
    private String label;

    protected CheckInTag() {
    }

    public CheckInTag(TagType tagType, String label) {
        this.tagType = tagType;
        this.label = label;
    }

    public TagType getTagType() { return tagType; }
    public String getLabel() { return label; }

    @Override
    public boolean equals(Object o) {
        return o instanceof CheckInTag other && tagType == other.tagType && Objects.equals(label, other.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagType, label);
    }
}
