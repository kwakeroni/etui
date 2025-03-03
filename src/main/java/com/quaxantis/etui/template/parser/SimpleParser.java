package com.quaxantis.etui.template.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class SimpleParser {
    private static final Logger log = LoggerFactory.getLogger(SimpleParser.class);
    private static final Logger logToken = LoggerFactory.getLogger(SimpleParser.class.getName() + ".Token");
    private static final Token.EOF EOF = new Token.EOF();

    private final Tokenizer tokenizer = new Tokenizer(Operators.BY_DELIMITER);

    public Expression parse(String string) {
        return new Context(tokenizer.tokenize(string)).process();
    }

    private static class Context {
        private final Iterator<Token> tokens;
        private final Deque<ParserState> stack;

        public Context(Iterator<Token> tokens) {
            this.tokens = tokens;
            this.stack = new ArrayDeque<>();
            push(ParserStates.root());
        }

        @Nonnull
        private ParserState peek() {
            ParserState state = this.stack.peek();
            if (state == null) {
                throw new IllegalStateException("Out of parsing state");
            }
            return state;
        }

        private void push(@Nonnull ParserState state) {
            stack.push(state);
            log.debug("Push {}", stack);
        }

        private void pop() {
            Object removed = stack.pop();
            log.debug("Pop {} <-- {}", removed, stack);
        }

        @Nonnull
        private ParserAction popWith(@Nonnull Expression expression) {
            pop();
            log.debug("Offering {} to {}", expression, peek());
            return peek().offer(expression);
        }

        private void popAndPush(@Nonnull ParserState state) {
            pop();
            push(state);
        }

        private void proceed() {
            // Do nothing
        }

        @Nonnull
        private ParserAction offer(@Nonnull Token token) {
            logToken.debug("Offering {} to {}", token, peek());
            return peek().offer(token);
        }

        public Expression process() {
            while (tokens.hasNext()) {
                handle(tokens.next());
            }
            ParserAction result = offer(EOF);
            if (result instanceof ParserAction.Yield(var expression, _)) {
                pop();
                if (!this.stack.isEmpty()) {
                    throw new IllegalStateException("Expected stack to be empty after EOF but was " + this.stack);
                }
                return expression;
            } else {
                throw new IllegalStateException("Expected finished state after EOF but encountered " + result);
            }

        }

        @SuppressWarnings("StatementWithEmptyBody")
        private void handle(@Nonnull Token token) {
            while (!tryHandle(token)) {
                // token was not consumed
            }
        }

        @SuppressWarnings("SimplifiableConditionalExpression")
        private boolean tryHandle(@Nonnull Token token) {
            ParserAction result = offer(token);
            handle(result);
            return (result instanceof ParserAction.Yield(_, var consumed)) ? consumed : true;
        }

        private void handle(@Nonnull ParserAction result) {
            switch (result) {
                case ParserAction.Proceed() -> proceed();
                case ParserAction.PushState(var state) -> push(state);
                case ParserAction.ReplaceState(var state) -> popAndPush(state);
                case ParserAction.FinishState() -> pop();
                case ParserAction.Yield(var exp, var _) -> handle(popWith(exp));
            }
        }
    }

}
