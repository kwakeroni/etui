package com.quaxantis.etui.template.expression.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

public class SimpleParser<Expression> {
    private static final Logger log = LoggerFactory.getLogger(SimpleParser.class);
    private static final Logger logToken = LoggerFactory.getLogger(SimpleParser.class.getName() + ".Token");
    private static final Token.EOF EOF = new Token.EOF();

    private final Language<Expression> language;
    private final Tokenizer<Expression> tokenizer;

    public SimpleParser(Language<Expression> language) {
        this.language = language;
        this.tokenizer = new Tokenizer<>(language);
    }

    public Expression parse(String string) {
        return new Context<>(language, tokenizer.tokenize(string)).process();
    }

    private static class Context<Expression> {
        private final Iterator<? extends Token<Expression>> tokens;
        private final Deque<ParserState<Expression>> stack;

        public Context(Language<Expression> language, Iterator<? extends Token<Expression>> tokens) {
            this.tokens = tokens;
            this.stack = new ArrayDeque<>();
            push(ParserStates.root(language));
        }

        @Nonnull
        private ParserState<Expression> peek() {
            ParserState<Expression> state = this.stack.peek();
            if (state == null) {
                throw new IllegalStateException("Out of parsing state");
            }
            return state;
        }

        private void push(@Nonnull ParserState<Expression> state) {
            stack.push(state);
            log.debug("Push {}", stack);
        }

        private void pop() {
            Object removed = stack.pop();
            log.debug("Pop {} <-- {}", removed, stack);
        }

        @Nonnull
        private ParserAction<Expression> popWith(@Nonnull Expression expression) {
            pop();
            log.debug("Offering {} to {}", expression, peek());
            return peek().offer(expression);
        }

        private void popAndPush(@Nonnull ParserState<Expression> state) {
            pop();
            push(state);
        }

        private void proceed() {
            // Do nothing
        }

        @Nonnull
        private ParserAction<Expression> offer(@Nonnull Token<Expression> token) {
            logToken.debug("Offering {} to {}", token, peek());
            return peek().offer(token);
        }

        public Expression process() {
            while (tokens.hasNext()) {
                handle(tokens.next());
            }
            ParserAction<? extends Expression> result = offer(EOF.anyCast());
            if (result instanceof ParserAction.Yield(Expression expression, _)) {
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
        private void handle(@Nonnull Token<Expression> token) {
            while (!tryHandle(token)) {
                // token was not consumed
            }
        }

        @SuppressWarnings("SimplifiableConditionalExpression")
        private boolean tryHandle(@Nonnull Token<Expression> token) {
            ParserAction<Expression> result = offer(token);
            handle(result);
            return (result instanceof ParserAction.Yield<? extends Expression>(_, var consumed)) ? consumed : true;
        }

        private void handle(@Nonnull ParserAction<Expression> result) {
            switch (result) {
                case ParserAction.Proceed() -> proceed();
                case ParserAction.PushState(var state) -> push(state);
                case ParserAction.ReplaceState(var state) -> popAndPush(state);
                case ParserAction.FinishState() -> pop();
                case ParserAction.Yield(var exp, var _) -> handle(popWith(exp));
            }
        }
    }

    public interface Language<Expression> {
        Map<String, Op<Expression>> delimiterToOperatorMap();

        Collection<? extends Group<Expression>> expressionGroups();

        Collection<? extends Group<Expression>> literalGroups();

        Collection<? extends Group<Expression>> subExpressions();

        Expression createLiteral(String text);

        Expression createIdentifier(String name);

        Expression concatenate(List<Expression> expressions);

        interface Op<Expression> {}

        interface Infix<Expression> extends Op<Expression> {
            Expression combine(Expression left, Expression right);
        }

        interface Group<E> {
            // Can the expression represented by this group be extended with an infix following the group
            boolean isExtensible();

            String startDelimiter();

            String endDelimiter();

            GroupStart<E> startOp();

            GroupEnd<E> endOp();
        }

        interface GroupStart<E> extends Op<E> {
            Group group();

            default boolean matchesAny(Collection<Group> groupOperators) {
                return groupOperators.contains(group());
            }
        }

        interface GroupEnd<E> extends Op<E> {
            Group group();

            default boolean matches(GroupStart start) {
                return group().startOp().equals(start);
            }
        }
    }
}
