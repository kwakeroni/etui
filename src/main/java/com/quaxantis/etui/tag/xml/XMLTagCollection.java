package com.quaxantis.etui.tag.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;

@JsonIgnoreProperties("schemaLocation")
public record XMLTagCollection(
        @JsonProperty("collection")
        String collection,

        @JsonProperty("family")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<XMLTagFamily> families
) {

}
