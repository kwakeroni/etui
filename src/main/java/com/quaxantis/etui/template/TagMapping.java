package com.quaxantis.etui.template;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.TemplateValues;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Deprecated
public interface TagMapping {

    @Deprecated
    List<Tag> tags();

    Optional<Template.Variable> variable();

    @Deprecated
    String apply(Context context, TemplateValues values);

    @Deprecated
    Optional<Map.Entry<String, String>> toValueOf(Context context, Template.Variable variable, TagSet tagSet);

    @Deprecated
    interface Context {
        ExpressionEvaluator evaluator();
    }

    static TagMapping between(Template.Variable variable, Tag tag) {
        return new SimpleMapping(tag, variable);
    }

    static TagMapping evaluating(String expression, Tag tag) {
        return new InterpolationMapping(expression, tag);
    }

    class SimpleMapping implements TagMapping {
        private final Tag tag;
        private final Template.Variable variable;

        public SimpleMapping(Tag tag, Template.Variable variable) {
            this.tag = tag;
            this.variable = variable;
        }

        @Override
        public List<Tag> tags() {
            return List.of(tag);
        }

        @Override
        public Optional<Template.Variable> variable() {
            return Optional.of(this.variable);
        }

        @Override
        public String apply(Context context, TemplateValues values) {
            return values.getValue(variable).orElse(null);
        }

        @Override
        public Optional<Map.Entry<String, String>> toValueOf(Context context, Template.Variable otherVar, TagSet tagSet) {
            if (otherVar.name().equals(variable.name())) {
                return tagSet.getValue(tag).map(value -> Map.entry(tag.qualifiedName(), value));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public String toString() {
            return "[" + variable.label() + "=>" + tag.qualifiedName() + "]";
        }
    }

    class InterpolationMapping implements TagMapping {
        private final String expression;
        private final Tag tag;

        private InterpolationMapping(String expression, Tag tag) {
            this.expression = expression;
            this.tag = tag;
        }

        @Override
        public List<Tag> tags() {
            return List.of(tag);
        }

        @Override
        public Optional<Template.Variable> variable() {
            return Optional.empty();
        }

        @Override
        public String apply(Context context, TemplateValues values) {
            return context.evaluator().evaluate(this.expression, values);
        }

        @Override
        public Optional<Map.Entry<String, String>> toValueOf(Context context, Template.Variable variable, TagSet tagSet) {
            return Optional.empty();
        }
    }
}
