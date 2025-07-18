package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.SimpleParser.Language;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.quaxantis.etui.template.parser.ParserAction.*;

final class ParserStates<E> {
    private final Language<E> language;

    private ParserStates(Language<E> language) {
        this.language = language;
    }

    static <E> ParserState<E> root(Language<E> language) {
        return new ParserStates<>(language).new Root();
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

    static IllegalStateException unexpectedToken(Token<?> token, Object expected) {
        return new IllegalStateException("Expected %s but encountered %s".formatted(expected, token));
    }

    // Text = <any character>+ | Group(expression)
    private abstract class Text implements ParserState<E> {
        private final List<E> expressions = new ArrayList<>();

        @Nonnull
        @Override
        public ParserAction<E> offer(@Nonnull Token<E> token) {
            return switch (token) {
                case Token.Operator(Language.GroupStart op, _) when op.matchesAny(language.expressionGroups()) ->
                        push(new Group(op));
                case Token.Operator(_, var delimiter) -> proceed(addText(delimiter));
                case Token.AlphaNumeric(var text) -> proceed(addText(text));
                case Token.Whitespace(var whitespace) -> proceed(addText(whitespace));
                case Token.Other(var op) -> proceed(addText(op));
                case Token.EOF() -> throw unexpectedToken(token, "text");
            };
        }

        @Nonnull
        @Override
        public ParserAction<E> offer(@Nonnull E expression) {
            return proceed(add(expression));
        }

        protected Void add(@Nonnull E expression) {
            expressions.add(expression);
            return null;
        }

        protected Void addText(@Nonnull java.lang.String text) {
            return add(language.createLiteral(text));
        }

        @Nonnull
        protected E concat() {
            if (this.expressions.size() == 1) {
                return this.expressions.getFirst();
            } else {
                return language.concatenate(this.expressions);
            }
        }

        @Override
        public java.lang.String toString() {
            return getClass().getSimpleName();
        }
    }

    // Expr = Expr [Whitespace] [<infix operator> Expr]
    private class ExtensibleExpr implements ParserState<E> {

        private final E expression;

        private ExtensibleExpr(E expression) {
            this.expression = expression;
        }

        @Nonnull
        @Override
        public ParserAction<E> offer(@Nonnull Token<E> token) {
            return switch (token) {
                case Token.Whitespace(_) -> proceed();
                case Token.Operator<E>(Language.Infix<E> infix, _) -> replaceWith(new Infix(expression, infix));
                default -> yieldExpression(expression, false);
            };
        }

        @Override
        public java.lang.String toString() {
            return getClass().getSimpleName();
        }
    }

    // Expr = [Whitespace] (Identifier | String) [Whitespace]
    private abstract class Expr implements ParserState<E> {
        @Nonnull
        @Override
        public ParserAction<E> offer(@Nonnull Token<E> token) {
            return switch (token) {
                case Token.Whitespace(_) -> proceed();
                case Token.AlphaNumeric(var identifier) ->
                        push(new ExtensibleExpr(language.createIdentifier(identifier)));
                case Token.Operator(Language.GroupStart op, _) when op.matchesAny(language.literalGroups()) ->
                        push(new String(op));
                case Token.Operator(Language.GroupStart op, _) when op.matchesAny(language.subExpressions()) ->
                        push(new Group(op));
                case Token.Operator(_, _), Token.Other(_), Token.EOF() -> throw unexpectedToken(token, "expression");
            };
        }

        @Nonnull
        @Override
        public abstract ParserAction<E> offer(@Nonnull E expression);

    }

    // String = '"' Text '"'
    private class String extends Text {

        private final Language.GroupStart startOperator;

        String(Language.GroupStart startOperator) {
            this.startOperator = startOperator;
        }

        @Nonnull
        @Override
        public ParserAction<E> offer(@Nonnull Token<E> token) {
            if (token instanceof Token.Operator(Language.GroupEnd op, _) && op.matches(startOperator)) {
                return replaceWith(new ExtensibleExpr(concat()));
            } else {
                return super.offer(token);
            }
        }


    }

    // Group = GroupStart Expr GroupEnd
    private class Group extends Expr {
        private final Language.GroupStart startOperator;

        private Group(Language.GroupStart startOperator) {
            this.startOperator = startOperator;
        }

        @Nonnull
        @Override
        public ParserAction<E> offer(@Nonnull Token<E> token) {
            if (token instanceof Token.Operator(Language.GroupEnd op, _) && op.matches(startOperator)) {
                return finish();
            } else {
                return super.offer(token);
            }
        }

        @Nonnull
        @Override
        public ParserAction<E> offer(@Nonnull E expression) {
            return replaceWith(new EndGroupState(startOperator, expression));
        }

        @Override
        public java.lang.String toString() {
            return getClass().getSimpleName() + "=" + startOperator;
        }
    }

    //     GroupEnd = [Whitespace] <group end operator>
    private class EndGroupState implements ParserState<E> {
        private final Language.GroupStart startOperator;
        private final E expression;

        private EndGroupState(Language.GroupStart startOperator, E expression) {
            this.startOperator = startOperator;
            this.expression = expression;
        }

        @Nonnull
        @Override
        public ParserAction<E> offer(@Nonnull Token<E> token) {
            return switch (token) {
                case Token.Whitespace(_) -> proceed();
                case Token.Operator(Language.GroupEnd op, _) when op.matches(startOperator) ->
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
    private class Infix extends Expr {

        private final E leftExpression;
        private final Language.Infix<E> operator;

        public Infix(E leftExpression, Language.Infix<E> operator) {
            this.leftExpression = leftExpression;
            this.operator = operator;
        }

        @Nonnull
        @Override
        public ParserAction<E> offer(@Nonnull E rightExpression) {
            E combined = operator.combine(leftExpression, rightExpression);
            return replaceWith(new ExtensibleExpr(combined));
        }

    }

    // Root = Text EOF
    private class Root extends Text {
        @Nonnull
        @Override
        public ParserAction<E> offer(@Nonnull Token<E> token) {
            // RootText = Text | EOF
            if (Objects.requireNonNull(token) instanceof Token.EOF()) {
                return yieldExpression(concat(), true);
            } else {
                return super.offer(token);
            }
        }

    }
}
