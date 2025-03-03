package com.quaxantis.etui.template.parser;

sealed interface ParserAction {
    static ParserAction proceed(Void... ignored) {
        return Proceed.INSTANCE;
    }

    static ParserAction push(ParserState state) {
        return new PushState(state);
    }

    static ParserAction replaceWith(ParserState state) {
        return new ReplaceState(state);
    }

    static ParserAction finish() {
        return FinishState.INSTANCE;
    }

    static ParserAction yieldExpression(Expression expression, boolean consumedToken) {
        return new Yield(expression, consumedToken);
    }

    record Proceed() implements ParserAction {
        private static final Proceed INSTANCE = new Proceed();
    }

    record PushState(ParserState state) implements ParserAction {
    }

    record ReplaceState(ParserState state) implements ParserAction {
    }

    record FinishState() implements ParserAction {
        private static final FinishState INSTANCE = new FinishState();
    }

    record Yield(Expression expression, boolean consumedToken) implements ParserAction {

    }

}
