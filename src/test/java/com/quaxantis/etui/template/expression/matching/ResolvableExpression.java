package com.quaxantis.etui.template.expression.matching;

import java.util.function.BiFunction;

record ResolvableExpression<E>(E expression, BiFunction<E, Binding, String> resolver) {

    String resolve(Binding binding) {
        return resolver.apply(expression, binding);
    }
}
