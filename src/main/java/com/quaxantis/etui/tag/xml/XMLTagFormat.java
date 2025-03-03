package com.quaxantis.etui.tag.xml;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.StringJoiner;

public record XMLTagFormat(
        @JsonDeserialize(using = DescriptionDeserializer.class)
        String description,
        List<XMLFormatExample> examples
) {

    @Override
    public String toString() {
        return new StringJoiner(", ", XMLTagFormat.class.getSimpleName() + "[", "]")
                .add("description='" + description + "'")
                .toString();
    }
}
