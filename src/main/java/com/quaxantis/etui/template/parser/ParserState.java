package com.quaxantis.etui.template.parser;

import javax.annotation.Nonnull;

interface ParserState {
    @Nonnull
    default ParserAction offer(@Nonnull Token token) {
        throw new UnsupportedOperationException("Current state cannot accept a token: " + this);
    }

    @Nonnull
    default ParserAction offer(@Nonnull Expression expression) {
        throw new UnsupportedOperationException("Current state cannot accept an expression: " + this);
    }
}
