package com.quaxantis.etui;

import com.quaxantis.etui.template.expression.ExpressionEvaluator;

import java.util.*;

public interface Template {

    String name();

    List<Variable> variables();

    default Optional<Variable> variableByName(String name) {
        return variables().stream()
                .filter(var -> var.name().equals(name))
                .findAny();
    }

    Collection<Tag> tags(Variable variable);

    ExpressionEvaluator expressionEvaluator();


    interface Variable extends HasLabel {
        String name();

        String label();

        String expression();

        boolean hasExpression();

        static Builder withName(String name) {
            Objects.requireNonNull(name, "name");
            return new Builder().setName(name).setLabel(name);
        }

        class Builder implements Variable, HasLabel, HasDescription {
            String name;
            String label;
            String description;
            String expression;

            @Override
            public String description() {
                return description;
            }

            public Builder setDescription(String description) {
                this.description = description;
                return this;
            }

            @Override
            public String expression() {
                return expression;
            }

            @Override
            public boolean hasExpression() {
                return expression != null;
            }

            public Builder setExpression(String expression) {
                this.expression = expression;
                return this;
            }

            @Override
            public String label() {
                return label;
            }

            public Builder setLabel(String label) {
                this.label = label;
                return this;
            }

            @Override
            public String name() {
                return name;
            }

            private Builder setName(String name) {
                this.name = name;
                return this;
            }

            @Override
            public String toString() {
                return new StringJoiner(", ",Builder.class.getSimpleName() + "[", "]")
                        .add("name='" + name + "'")
                        .add("label='" + label + "'")
                        .add("expression='" + expression + "'")
                        .add("description='" + description + "'")
                        .toString();
            }
        }

    }

}
