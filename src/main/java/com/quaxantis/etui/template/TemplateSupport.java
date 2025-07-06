package com.quaxantis.etui.template;

import com.quaxantis.etui.HasDescription;
import com.quaxantis.etui.HasLabel;
import com.quaxantis.etui.Tag;
import com.quaxantis.etui.Template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class TemplateSupport implements Template {
    private final String name;
    private final String source;
    private final List<Variable> variables;
    private final List<TagMapping> mappings;
    private final ExpressionEvaluator expressionEvaluator;

    private TemplateSupport(String name, String source, List<Variable> variables, List<TagMapping> mappings) {
        this.name = name;
        this.source = source;
        this.variables = variables;
        this.mappings = mappings;
        this.expressionEvaluator = ExpressionEvaluator.of(ExpressionEvaluator.Context.of(this.variables));
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public List<Variable> variables() {
        return this.variables;
    }

    @Override
    public Collection<Tag> tags(Variable variable) {
        return mappings.stream()
                .filter(mapping -> mapping.variable().map(variable::equals).orElse(false))
                .map(TagMapping::tags)
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public ExpressionEvaluator expressionEvaluator() {
        return this.expressionEvaluator;
    }

    @Override
    public String toString() {
        return "TemplateSupport[" + name() + "]"
//                + "\n" + this.variables
//                + "\n" + this.mappings
                ;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        String name;
        String source;
        List<Variable> variables = new ArrayList<>();
        List<TagMapping> mappings = new ArrayList<>();
        Function<ExpressionEvaluator.Context, ExpressionEvaluator> evaluator = ExpressionEvaluator::of;

        public Builder(String name) {
            this.name = name;
            this.source = new Exception().getStackTrace()[1].toString();
        }

        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

        public Builder addVariable(Variable variable) {
            this.variables.add(variable);
            return this;
        }

        public Builder addVariable(Variable variable, Tag tag) {
            return addVariable(variable)
                    .addMapping(variable, tag);
        }

        public Builder addVariable(String label, Tag tag) {
            Variable.Builder variable;
            if ((tag instanceof HasDescription hasDescription)) {
                String description = hasDescription.description();
                variable = Variable.withName(label).setLabel(label).setDescription(description);
            } else {
                variable = Variable.withName(label);
            }
            return addVariable(variable, tag);
        }

        public Builder addVariable(Tag tag) {
            String label = (tag instanceof HasLabel hasLabel)? hasLabel.label() : tag.asString();
            return addVariable(label, tag);
        }

        public Builder addVariable(Variable variable, Function<Variable, Tag> tagSupplier) {
            return addVariable(variable, tagSupplier.apply(variable));
        }
        public Builder addVariable(String label, Function<Variable, Tag> tagSupplier) {
            return addVariable(label, tagSupplier.apply(Variable.withName(label)));
        }

        public Builder addMapping(Variable variable, Tag tag) {
            return addMapping(new TagMapping.SimpleMapping(tag, variable));
        }

        private Builder addMapping(TagMapping tagMapping) {
            this.mappings.add(tagMapping);
            return this;
        }

        public Builder addExpressionMapping(String expression, Tag tag) {
            this.mappings.add(TagMapping.evaluating(expression, tag));
            return this;
        }

        public Template build() {
            List<Variable> unmodifiableVariables = List.copyOf(this.variables);
            return new TemplateSupport(this.name, this.source, unmodifiableVariables, this.mappings);
        }
    }

}
