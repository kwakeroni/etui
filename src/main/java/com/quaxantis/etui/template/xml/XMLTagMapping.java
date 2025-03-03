package com.quaxantis.etui.template.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quaxantis.etui.Tag;

import java.util.Optional;

public class XMLTagMapping {
    @JsonProperty
    private String tag;
    @JsonProperty
    private String variable;
    @JsonProperty
    private String expression;

    public String tag() {
        return tag;
    }

    public Optional<String> variable() {
        return Optional.ofNullable(variable);
    }

    public Optional<String> expression() {
        return Optional.ofNullable(expression);
    }
}
