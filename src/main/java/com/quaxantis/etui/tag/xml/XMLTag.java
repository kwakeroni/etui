package com.quaxantis.etui.tag.xml;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagDescriptor;

import java.util.List;
import java.util.Optional;

public final class XMLTag implements Tag, TagDescriptor {
    @JsonProperty("name")
    private String tagName;
    @JsonProperty("readonly")
    private boolean readOnly = false;
    private String label;
    @JsonDeserialize(using = DescriptionDeserializer.class)
    private String description;
    private XMLTagFormat format;
    @JsonBackReference
    private XMLTagFamily family;

    @Override
    public String groupName() {
        return (family == null) ? null : family().group();
    }

    @Override
    public String tagName() {
        return tagName;
    }

    @Override
    public Tag tag() {
        return this;
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public XMLTagFamily family() {
        return family;
    }

    public void setFamily(XMLTagFamily family) {
        this.family = family;
    }

    public XMLTagFormat format() {
        return format;
    }

    public void setFormat(XMLTagFormat format) {
        this.format = format;
    }

    @Override
    public String formatDescription() {
        return (this.format == null)? null : this.format.description();
    }

    @Override
    public List<? extends Example> examples() {
        return Optional.ofNullable(this.format)
                .map(XMLTagFormat::examples)
                .orElse(List.of());
    }

    @Override
    public String toString() {
        return "XMLTag[" +
               "[" + asString() + "] " +
               "label=" + label + ", " +
               "description=" + description +
               ']';
    }

}
