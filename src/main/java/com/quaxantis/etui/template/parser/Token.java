package com.quaxantis.etui.template.parser;

sealed interface Token<Expression> {

    // TODO define custom pattern
    @SuppressWarnings("unused")
    String token();

    <E extends Expression> Token<E> anyCast();

    record AlphaNumeric(String token) implements Token<Object> {
        @Override
        public <E> Token<E> anyCast() {
            return (Token<E>) this;
        }
    }

    record Whitespace(String token) implements Token<Object> {
        @Override
        public <E> Token<E> anyCast() {
            return (Token<E>) this;
        }
    }

    record Operator<Expression>(SimpleParser.Language.Op<Expression> operator,
                                String token) implements Token<Expression> {
        @Override
        public <E extends Expression> Token<E> anyCast() {
            throw new UnsupportedOperationException("Cannot anycast Operator token");
        }
    }

    record Other(String token) implements Token<Object> {
        @Override
        public <E> Token<E> anyCast() {
            return (Token<E>) this;
        }
    }

    record EOF() implements Token<Object> {
        @Override
        public String token() {
            throw new UnsupportedOperationException("Reached end of input");
        }

        @Override
        public <E> Token<E> anyCast() {
            return (Token<E>) this;
        }

    }
}
