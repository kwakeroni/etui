package com.quaxantis.etui.template.expression.parser;

sealed interface ParserAction<Expression> {
    static <Expression> ParserAction<Expression> proceed(Void... ignored) {
        return Proceed.INSTANCE;
    }

    static <E> ParserAction<E> push(ParserState<E> state) {
        return new PushState<>(state);
    }

    static <E> ParserAction<E> replaceWith(ParserState<E> state) {
        return new ReplaceState<>(state);
    }

    static <E> ParserAction<E> finish() {
        return FinishState.INSTANCE;
    }

    static <E> ParserAction<E> yieldExpression(E expression, boolean consumedToken) {
        return new Yield<>(expression, consumedToken);
    }

    record Proceed<E>() implements ParserAction<E> {
        private static final Proceed INSTANCE = new Proceed<>();
    }

    record PushState<E>(ParserState<E> state) implements ParserAction<E> {
    }

    record ReplaceState<E>(ParserState<E> state) implements ParserAction<E> {
    }

    record FinishState<E>() implements ParserAction<E> {
        private static final FinishState INSTANCE = new FinishState<>();
    }

    record Yield<Expression>(Expression expression, boolean consumedToken) implements ParserAction<Expression> {

    }

}
