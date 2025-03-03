package com.quaxantis.etui.template.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@JsonIgnoreProperties("schemaLocation")
public record XMLTemplateCollection(
        @JsonProperty("group")
        String group,

        @JsonProperty("template")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<XMLTemplate> templates
) {

}
