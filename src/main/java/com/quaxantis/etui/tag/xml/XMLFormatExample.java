package com.quaxantis.etui.tag.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.quaxantis.etui.TagDescriptor;

import java.util.StringJoiner;

@JsonIgnoreProperties(ignoreUnknown = true, value = {""})
public final class XMLFormatExample implements TagDescriptor.Example {
    @JacksonXmlProperty(isAttribute = true)
    private String pattern;

    @JacksonXmlProperty(isAttribute = true)
    private String value;

    @JacksonXmlText
    private String text;

    @Override
    public String pattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String text() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String value() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", XMLFormatExample.class.getSimpleName() + "[", "]")
                .add("pattern='" + pattern + "'")
                .add("value='" + value + "'")
                .add("text='" + text + "'")
                .toString();
    }
}
