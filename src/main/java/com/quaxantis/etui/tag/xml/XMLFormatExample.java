package com.quaxantis.etui.tag.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.quaxantis.etui.TagDescriptor;

@JsonIgnoreProperties(ignoreUnknown = true, value = {""})
public final class XMLFormatExample implements TagDescriptor.Example {
    @JacksonXmlProperty(isAttribute = true)
    private String pattern;

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
    public String toString() {
        return "XMLFormatExample[" +
               "pattern=" + pattern + ", " +
               "example=" + text + ']';
    }
}
