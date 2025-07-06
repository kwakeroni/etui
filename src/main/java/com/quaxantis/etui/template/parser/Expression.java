package com.quaxantis.etui.template.parser;

import com.quaxantis.support.util.ANSI;

import java.util.List;
import java.util.stream.Collectors;

public sealed interface Expression {

    String representationString();

    String emphasisString();

    private static String emphasis(String string) {
        return ANSI.YELLOW_BRIGHT + string + ANSI.DEFAULT_COLOR;
    }

    final record Concat(List<Expression> parts) implements Expression {

        Concat(Expression... expressions) {
            this(List.of(expressions));
        }

        @Override
        public String representationString() {
            return parts.stream().map(Expression::representationString).collect(Collectors.joining(") + (", "(", ")"));
        }

        @Override
        public String emphasisString() {
            return parts.stream().map(Expression::representationString).collect(Collectors.joining(") " + emphasis("+") + " (", "(", ")"));
        }

    }

    final record Text(String literal) implements Expression {
        @Override
        public String representationString() {
            return "'" + literal + "'";
        }

        @Override
        public String emphasisString() {
            return emphasis(representationString());
        }
    }

    final record Identifier(String name) implements Expression {
        @Override
        public String representationString() {
            return "${" + name + "}";
        }

        @Override
        public String emphasisString() {
            return emphasis(representationString());
        }
    }

    final record Elvis(Expression expression, Expression orElse) implements Expression {
        @Override
        public String representationString() {
            return "(" + expression.representationString() + ")?:(" + orElse.representationString() + ")";
        }

        @Override
        public String emphasisString() {
            return "(" + expression.representationString() + ")" + emphasis("?:") + "(" + orElse.representationString() + ")";
        }
    }

    final record OptSuffix(Expression expression, Expression suffix) implements Expression {
        @Override
        public String representationString() {
            return "(" + expression.representationString() + ")?+(" + suffix.representationString() + ")";
        }

        @Override
        public String emphasisString() {
            return "(" + expression.representationString() + ")" + emphasis("?+") + "(" + suffix.representationString() + ")";
        }
    }

    final record OptPrefix(Expression prefix, Expression expression) implements Expression {
        @Override
        public String representationString() {
            return "(" + prefix.representationString() + ")+?(" + expression.representationString() + ")";
        }

        @Override
        public String emphasisString() {
            return "(" + prefix.representationString() + ")" + emphasis("+?") + "(" + expression.representationString() + ")";
        }
    }

    final record Delegate(Expression delegate) implements Expression {
        @Override
        public String representationString() {
            return delegate.representationString();
        }

        @Override
        public String emphasisString() {
            return emphasis(representationString());
        }
    }
}
