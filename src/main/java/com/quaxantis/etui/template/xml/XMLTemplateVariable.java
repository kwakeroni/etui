package com.quaxantis.etui.template.xml;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.quaxantis.etui.HasLabel;
import com.quaxantis.etui.Tag;
import com.quaxantis.etui.Template;

import java.util.*;

public class XMLTemplateVariable implements Template.Variable, HasLabel {
    @JacksonXmlProperty
    private String name;
    @JacksonXmlProperty
    private String label;
    @JacksonXmlProperty
    private String expression;
    @JsonIgnore
    private final Set<String> tags = new HashSet<>();

    @JsonAnySetter
    public void setTagAttribute(String name, Object value) {
        if ("tag".equals(name)) {
            if (value instanceof String stringValue) {
                this.tags.add(stringValue);
            } else {
                throw new IllegalArgumentException("Field \"%s\" expected to have a String value but was %s"
                                                           .formatted(name, (value==null)? "null" : value.getClass()));
            }
        } else {
            throw new IllegalArgumentException("Unrecognized field \"%s\"".formatted(name));
        }
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String label() {
        return this.label;
    }

    public Collection<String> tags() {
        return Set.copyOf(this.tags);
    }

    @Override
    public String expression() {
        return this.expression;
    }

    @Override
    public boolean hasExpression() {
        return this.expression != null;
    }
}
