package com.quaxantis.etui.template.parser;

import java.util.List;
import java.util.stream.Collectors;

public sealed interface Expression {

    String representationString();

    final record Concat(List<Expression> parts) implements Expression {
        Concat(Expression... expressions) {
            this(List.of(expressions));
        }

        @Override
        public String representationString() {
            return parts.stream().map(Expression::representationString).collect(Collectors.joining(") + (", "(", ")"));
        }
    }

    final record Text(String literal) implements Expression {
        @Override
        public String representationString() {
            return "'" + literal + "'";
        }
    }

    final record Identifier(String name) implements Expression {
        @Override
        public String representationString() {
            return "${" + name + "}";
        }
    }

    final record Elvis(Expression expression, Expression orElse) implements Expression {
        @Override
        public String representationString() {
            return "(" + expression.representationString() + ")?:(" + orElse.representationString() + ")";
        }
    }

    final record OptSuffix(Expression expression, Expression suffix) implements Expression {
        @Override
        public String representationString() {
            return "(" + expression.representationString() + ")?+(" + suffix.representationString() + ")";
        }
    }

    final record OptPrefix(Expression prefix, Expression expression) implements Expression {
        @Override
        public String representationString() {
            return "(" + prefix.representationString() + ")+?(" + expression.representationString() + ")";
        }
    }
}
