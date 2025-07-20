package com.quaxantis.etui;

import com.quaxantis.etui.template.expression.ExpressionEvaluator;

import java.util.*;
import java.util.function.Function;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public final class TemplateBuilder {
    private final String name;
    private final List<Template.Variable> variables = new ArrayList<>();
    private final Map<String, List<Tag>> mapping = new HashMap<>();
    private Function<List<Template.Variable>, ExpressionEvaluator> expressionEvaluatorFunction;

    private TemplateBuilder(String name) {
        this.name = name;
    }

    public TemplateBuilder add(Template.Variable variable, Tag... tags) {
        mapping.merge(variable.name(), List.of(tags), (_, _) -> {
            throw new IllegalStateException("Variable '%s' already added".formatted(variable.name()));
        });
        variables.add(variable);
        return this;
    }

    public TemplateBuilder setExpressionEvaluator(Function<List<Template.Variable>, ExpressionEvaluator> expressionEvaluatorFunction) {
        this.expressionEvaluatorFunction = expressionEvaluatorFunction;
        return this;
    }

    public Template build() {
        List<Template.Variable> variablesCopy = List.copyOf(variables);
        Map<Template.Variable, List<Tag>> mappingCopy = variablesCopy.stream().collect(toMap(identity(), var -> mapping.get(var.name())));
        ExpressionEvaluator expressionEvaluator = expressionEvaluatorFunction.apply(variablesCopy);

        return new Template() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<Variable> variables() {
                return (List<Variable>) (List<? extends Variable>) variablesCopy;
            }

            @Override
            public Collection<Tag> tags(Variable variable) {
                return Objects.requireNonNull(mappingCopy.get(variable), () -> "Unknown variable '%s'".formatted(variable.name()));
            }

            @Override
            public ExpressionEvaluator expressionEvaluator() {
                return expressionEvaluator;
            }
        };
    }
}
