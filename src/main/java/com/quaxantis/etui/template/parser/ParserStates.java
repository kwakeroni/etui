package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.Operators.InfixOp;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.quaxantis.etui.template.parser.Operators.GroupOp;
import static com.quaxantis.etui.template.parser.ParserAction.*;

final class ParserStates {
    private ParserStates() {
        throw new UnsupportedOperationException();
    }

    static ParserState root() {
        return new Root();
    }

    // Whitespace = <whitespace character>+
    // Text = <any character>+ | Group(expression)
    // Expr = Expr [Whitespace] Infix
    //   Infix = [<infix operator> Expr]
    // Expr = [Whitespace] (Identifier | String) [Whitespace]
    //     Identifier = <alphanumeric character>+
    //     String = '"' Text '"'
    //     Group = GroupStart Expr GroupEnd
    //       GroupStart = <group start operator>
    //       GroupEnd = [Whitespace] <group end operator>
    // Root = Text EOF

    static IllegalStateException unexpectedToken(Token token, Object expected) {
        return new IllegalStateException("Expected %s but encountered %s".formatted(expected, token));
    }

    // Text = <any character>+ | Group(expression)
    private abstract static class Text implements ParserState {

        private final List<Expression> expressions = new ArrayList<>();

        @Nonnull
        @Override
        public ParserAction offer(@Nonnull Token token) {
            return switch (token) {
                case Token.Operator(GroupOp.Start op, _) when op.matches(GroupOp.EXPRESSION) -> push(new Group(op));
                case Token.Operator(_, var delimiter) -> proceed(addText(delimiter));
                case Token.AlphaNumeric(var text) -> proceed(addText(text));
                case Token.Whitespace(var whitespace) -> proceed(addText(whitespace));
                case Token.Other(var op) -> proceed(addText(op));
                case Token.EOF() -> throw unexpectedToken(token, "text");
            };
        }

        @Nonnull
        @Override
        public ParserAction offer(@Nonnull Expression expression) {
            return proceed(add(expression));
        }

        protected Void add(@Nonnull Expression expression) {
            expressions.add(expression);
            return null;
        }

        protected Void addText(@Nonnull java.lang.String text) {
            return add(new Expression.Text(text));
        }

        @Nonnull
        protected Expression concat() {
            if (this.expressions.size() == 1) {
                return this.expressions.get(0);
            } else {
                return new Expression.Concat(this.expressions);
            }
        }

        @Override
        public java.lang.String toString() {
            return getClass().getSimpleName();
        }
    }

    // Expr = Expr [Whitespace] [<infix operator> Expr]
    private static class ExtensibleExpr implements ParserState {

        private final Expression expression;

        private ExtensibleExpr(Expression expression) {
            this.expression = expression;
        }

        @Nonnull
        @Override
        public ParserAction offer(@Nonnull Token token) {
            return switch (token) {
                case Token.Whitespace(_) -> proceed();
                case Token.Operator(InfixOp infix, _) -> replaceWith(new Infix(expression, infix));
                default -> yieldExpression(expression, false);
            };
        }

        @Override
        public java.lang.String toString() {
            return getClass().getSimpleName();
        }

        static ExtensibleExpr identifier(java.lang.String identifier) {
            return new ExtensibleExpr(new Expression.Identifier(identifier));
        }
    }

    // Expr = [Whitespace] (Identifier | String) [Whitespace]
    private abstract static class Expr implements ParserState {

        @Nonnull
        @Override
        public ParserAction offer(@Nonnull Token token) {
            return switch (token) {
                case Token.Whitespace(_) -> proceed();
                case Token.AlphaNumeric(var identifier) -> push(ExtensibleExpr.identifier(identifier));
                case Token.Operator(GroupOp.Start op, _)
                        when op.matches(GroupOp.STRING_DOUBLE) || op.matches(GroupOp.STRING_SINGLE) ->
                        push(new String(op));
                case Token.Operator(GroupOp.Start op, _)
                        when op.matches(GroupOp.PARENTHESES) -> push(new Group(op));
                case Token.Operator(_, _), Token.Other(_), Token.EOF() -> throw unexpectedToken(token, "expression");
            };
        }

        @Nonnull
        @Override
        public abstract ParserAction offer(@Nonnull Expression expression);

    }

    // String = '"' Text '"'
    private static class String extends Text {

        private final GroupOp.Start startOperator;

        String(GroupOp.Start startOperator) {
            this.startOperator = startOperator;
        }

        @Nonnull
        @Override
        public ParserAction offer(@Nonnull Token token) {
            if (token instanceof Token.Operator(GroupOp.End op, _) && op.matches(startOperator)) {
                return replaceWith(new ExtensibleExpr(concat()));
            } else {
                return super.offer(token);
            }
        }


    }

    // Group = GroupStart Expr GroupEnd
    private static class Group extends Expr {
        private final GroupOp.Start startOperator;

        private Group(GroupOp.Start startOperator) {
            this.startOperator = startOperator;
        }

        @Nonnull
        @Override
        public ParserAction offer(@Nonnull Token token) {
            if (token instanceof Token.Operator(GroupOp.End op, _) && op.matches(startOperator)) {
                return finish();
            } else {
                return super.offer(token);
            }
        }

        @Nonnull
        @Override
        public ParserAction offer(@Nonnull Expression expression) {
            return replaceWith(new EndGroupState(startOperator, expression));
        }

        @Override
        public java.lang.String toString() {
            return getClass().getSimpleName() + "=" + startOperator;
        }
    }

    //     GroupEnd = [Whitespace] <group end operator>
    private static class EndGroupState implements ParserState {
        private final GroupOp.Start startOperator;
        private final Expression expression;

        private EndGroupState(GroupOp.Start startOperator, Expression expression) {
            this.startOperator = startOperator;
            this.expression = expression;
        }

        @Nonnull
        @Override
        public ParserAction offer(@Nonnull Token token) {
            return switch (token) {
                case Token.Whitespace(_) -> proceed();
                case Token.Operator(GroupOp.End op, _) when op.matches(startOperator) ->
                        (startOperator.group().isExtensible()) ?
                                replaceWith(new ExtensibleExpr(expression))
                                : yieldExpression(expression, true);
                default -> throw unexpectedToken(token, startOperator.group().endDelimiter());
            };
        }

        @Override
        public java.lang.String toString() {
            return getClass().getSimpleName() + "=" + startOperator;
        }
    }

    //   Infix = [<infix operator> Expr]
    private static class Infix extends Expr {

        private final Expression leftExpression;
        private final InfixOp operator;

        public Infix(Expression leftExpression, InfixOp operator) {
            this.leftExpression = leftExpression;
            this.operator = operator;
        }

        @Nonnull
        @Override
        public ParserAction offer(@Nonnull Expression rightExpression) {
            Expression combined = operator.combine(leftExpression, rightExpression);
            return replaceWith(new ExtensibleExpr(combined));
        }

    }

    // Root = Text EOF
    private static class Root extends Text {

        @Nonnull
        @Override
        public ParserAction offer(@Nonnull Token token) {
            // RootText = Text | EOF
            if (Objects.requireNonNull(token) instanceof Token.EOF()) {
                return yieldExpression(concat(), true);
            } else {
                return super.offer(token);
            }
        }

    }
}
