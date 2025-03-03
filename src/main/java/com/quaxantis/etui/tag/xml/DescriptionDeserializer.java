package com.quaxantis.etui.tag.xml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DescriptionDeserializer extends StringDeserializer {
    private static final Pattern MULTILINE_WHITESPACE = Pattern.compile("\\v\\V*\\v");
    private static final Pattern WHITESPACE = Pattern.compile("\\s{2,}");

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String string = super.deserialize(p, ctxt);
        StringBuilder builder = new StringBuilder();

        int index = 0;
        Matcher m = WHITESPACE.matcher(string);

        // Add segments before each match found
        while (m.find()) {
            builder.append(string, index, m.start());
            builder.append(normalizeWhitespace(string.substring(m.start(), m.end())));
            index = m.end();
        }

        // If no match was found, return this
        if (index == 0)
            return string;

        // Add remaining segment
        builder.append(string.substring(index));
        return builder.toString();
    }

    private String normalizeWhitespace(String whitespace) {
        if (MULTILINE_WHITESPACE.matcher(whitespace).find()) {
            return System.lineSeparator();
        } else {
            return " ";
        }
    }

}
