package com.quaxantis.etui.template.expression.eel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

class EELExpressionResolver {
    private final Function<String, String> identifierResolver;

    public EELExpressionResolver(Function<String, String> identifierResolver) {
        this.identifierResolver = identifierResolver;
    }

    public String resolve(Expression expression) {
        return switch (expression) {
            case Expression.Text(var text) -> text;
            case Expression.Identifier(var identifier) -> resolveIdentifier(identifier);
            case Expression.Concat(var expressions) -> concatenate(expressions);
            case Expression.Elvis(var main, var orElse) -> resolveElvis(main, orElse);
            case Expression.OptPrefix(var prefix, var main) -> resolveOptPrefix(prefix, main);
            case Expression.OptSuffix(var main, var suffix) -> resolveOptSuffix(main, suffix);
            case Expression.Delegate(var delegate) -> resolve(delegate);
        };
    }

    private String resolveElvis(Expression main, Expression orElse) {
        return Optional.ofNullable(resolve(main))
                .filter(not(String::isEmpty))
                .orElseGet(() -> resolve(orElse));
    }

    private String resolveOptSuffix(Expression main, Expression suffix) {
        String mainValue = resolve(main);
        if (mainValue == null || mainValue.isEmpty()) {
            return mainValue;
        } else {
            String suffixValue = resolve(suffix);
            return (suffixValue == null) ? mainValue : mainValue + suffixValue;
        }
    }

    private String resolveOptPrefix(Expression prefix, Expression main) {
        String mainValue = resolve(main);
        if (mainValue == null || mainValue.isEmpty()) {
            return mainValue;
        } else {
            String prefixValue = resolve(prefix);
            return (prefixValue == null) ? mainValue : prefixValue + mainValue;
        }
    }

    private String resolveIdentifier(String identifier) {
        return identifierResolver.apply(identifier);
    }

    private String concatenate(List<Expression> expressions) {
        return expressions.stream().map(this::resolve).filter(Objects::nonNull).collect(Collectors.joining());
    }

}
