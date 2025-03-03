package com.quaxantis.etui.template.parser;

import java.util.List;

public sealed interface Expression {

    final record Concat(List<Expression> parts) implements Expression {
        Concat(Expression... expressions) {
            this(List.of(expressions));
        }
    }

    final record Text(String literal) implements Expression {

    }

    final record Identifier(String name) implements Expression {

    }

    final record Elvis(Expression expression, Expression orElse) implements Expression {

    }

    final record OptSuffix(Expression expression, Expression suffix) implements Expression {

    }

    final record OptPrefix(Expression prefix, Expression expression) implements Expression {

    }
}
