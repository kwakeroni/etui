package com.quaxantis.etui.tag.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.quaxantis.etui.HasDescription;
import com.quaxantis.etui.HasLabel;
import com.quaxantis.etui.TagFamily;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@JsonIgnoreProperties({"homePage", "reference"})
public final class XMLTagFamily implements TagFamily, HasLabel, HasDescription {
    private String name;
    private String group;
    private String label;
    @JsonProperty("readonly")
    private Boolean readonly;

    @JsonDeserialize(using = DescriptionDeserializer.class)
    private String description;
    @JsonManagedReference
    private List<XMLTag> tags;

    public XMLTagFamily() {
    }

    public String name() {
        return name;
    }

    public String group() {
        return (StringUtils.isEmpty(group))? null : group;
    }

    @Override
    public String label() {
        return (label != null)? label : name;
    }

    public String description() {
        return description;
    }

    @Override
    @JsonManagedReference
    public List<XMLTag> tags() {
        return (tags != null)? tags : List.of();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTags(List<XMLTag> tags) {
        this.tags = tags;
    }

    Boolean getReadonly() {
        return this.readonly;
    }

    @Override
    public Optional<String> defaultGroup() {
        return Optional.ofNullable(this.group);
    }

    @Override
    public boolean defaultReadOnly() {
        return Boolean.TRUE.equals(this.readonly);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (XMLTagFamily) obj;
        return Objects.equals(this.name, that.name) &&
               Objects.equals(this.group, that.group) &&
               Objects.equals(this.label, that.label) &&
               Objects.equals(this.description, that.description) &&
               Objects.equals(this.tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, group, label, description, tags);
    }

    @Override
    public String toString() {
        return "XMLTagFamily[" +
               "name=" + name + ", " +
               "group=" + group + ", " +
               "label=" + label + ", " +
               "description=" + description + ", " +
               "tags=" + tags + ']';
    }

}
