package com.quaxantis.etui.template.parser;

sealed interface Token {

    // TODO define custom pattern
    @SuppressWarnings("unused")
    String token();

    record AlphaNumeric(String token) implements Token {
    }

    record Whitespace(String token) implements Token {
    }

    record Operator(Object operator, String token) implements Token {

    }

    record Other(String token) implements Token {
    }

    record EOF() implements Token {
        @Override
        public String token() {
            throw new UnsupportedOperationException("Reached end of input");
        }
    }
}
