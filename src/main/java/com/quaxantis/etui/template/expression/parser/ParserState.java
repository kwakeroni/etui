package com.quaxantis.etui.template.expression.parser;

import javax.annotation.Nonnull;

interface ParserState<Expression> {
    @Nonnull
    default ParserAction<Expression> offer(@Nonnull Token<Expression> token) {
        throw new UnsupportedOperationException("Current state cannot accept a token: " + this);
    }

    @Nonnull
    default ParserAction<Expression> offer(@Nonnull Expression expression) {
        throw new UnsupportedOperationException("Current state cannot accept an expression: " + this);
    }
}
