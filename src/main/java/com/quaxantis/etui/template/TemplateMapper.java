package com.quaxantis.etui.template;

import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.TagValue;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.Template.Variable;
import com.quaxantis.etui.TemplateValues;
import com.quaxantis.etui.template.expression.Binding;
import com.quaxantis.etui.template.expression.ExpressionEvaluator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.filtering;
import static java.util.stream.Collectors.toMap;

public class TemplateMapper {
    private final com.quaxantis.etui.Template template;

    public TemplateMapper(Template template) {
        this.template = template;
    }

    public TemplateValues newValues() {
        return new TemplateValuesSupport(new HashMap<>());
    }

    public TemplateValues fromTags(TagSet tagSet) {
        var specifiedValues = new HashMap<String, TemplateValues.Entry>();
        var unspecifiedVariables = new ArrayList<Variable>();

        for (Variable variable : template.variables()) {
            Optional<TemplateValues.Entry> optEntry = template.tags(variable)
                    .stream()
                    .map(tagSet::getTag)
                    .flatMap(Optional::stream)
                    .collect(TemplateValuesSupport.toTemplateValue());
            optEntry.ifPresentOrElse(entry -> specifiedValues.put(variable.name(), entry),
                                     () -> unspecifiedVariables.add(variable));
        }

        var unspecifiedValues = analyzeUnspecifiedVariables(unspecifiedVariables, specifiedValues);
        var result = new HashMap<String, TemplateValues.Entry>();
        result.putAll(unspecifiedValues);
        result.putAll(specifiedValues);

        return new TemplateValuesSupport(result);
    }

    private Map<String, TemplateValues.Entry> analyzeUnspecifiedVariables(Collection<Variable> variables, Map<String, TemplateValues.Entry> specifiedVariables) {
        Map<String, String> boundVariables = specifiedVariables.entrySet().stream()
                .collect(filtering((Map.Entry<String, TemplateValues.Entry> entry) -> entry.getValue().value().isPresent(),
                                   toMap(Map.Entry::getKey, entry -> entry.getValue().value().orElse(null))));

        ExpressionEvaluator evaluator = this.template.expressionEvaluator();
        Collection<Binding> bindings = this.template.variables()
                .stream()
                .filter(variable -> variable.hasExpression() && boundVariables.get(variable.name()) != null)
                .map(variable -> evaluator.deinterpolate(variable.expression(), boundVariables, boundVariables.get(variable.name())))
                .flatMap(Collection::stream)
                .toList();

        return variables.stream()
                .map(variable -> Map.entry(variable.name(),
                                           analyzeUnspecifiedVariable(variable, bindings)
                                                   .orElseGet(() -> TemplateValues.Entry.of(""))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Optional<TemplateValues.Entry> analyzeUnspecifiedVariable(Variable variable, Collection<Binding> bindings) {
        return bindings.stream()
                .filter(binding -> binding.valueOf(variable.name()).isPresent())
                .sorted(Comparator.comparing(Binding::score).reversed())
                .flatMap(binding -> binding.valueOf(variable.name()).stream())
                .findFirst()
                .map(value -> TemplateValues.Entry.of(value, List.of(), "Variable value was reconstituted from other expressions in the template."));
    }

    public TagSet toTags(TemplateValues values) {
        return template.variables()
                .stream()
                .flatMap(variable -> tagValues(variable, values))
                .collect(TagSet.toTagSet());
    }

    private Stream<TagValue> tagValues(Variable variable, TemplateValues values) {
        return values.getValue(variable)
                .stream()
                .filter(not(String::isEmpty))
                .flatMap(value -> tagValue(variable, value));
    }

    private Stream<TagValue> tagValue(Variable variable, String value) {
        return template.tags(variable)
                .stream()
                .map(tag -> TagValue.of(tag, value));
    }
}
