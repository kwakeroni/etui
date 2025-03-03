package com.quaxantis.etui.template.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public class XMLTemplate {
    @JsonProperty("label")
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String evaluator;

    @JsonProperty("variable")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<XMLTemplateVariable> variables;

    @JsonProperty("mapping")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<XMLTagMapping> mappings;

    public String name() {
        return this.name;
    }

    public Optional<String> evaluator() {
        return Optional.ofNullable(evaluator);
    }

    public List<XMLTemplateVariable> variables() {
        return (this.variables == null) ? Collections.emptyList() : Collections.unmodifiableList(this.variables);
    }

    public List<XMLTagMapping> mappings() {
        return (this.mappings == null) ? Collections.emptyList() : Collections.unmodifiableList(this.mappings);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", XMLTemplate.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .toString();
    }
}
