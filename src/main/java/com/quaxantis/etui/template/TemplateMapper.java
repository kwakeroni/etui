package com.quaxantis.etui.template;

import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.TagValue;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.Template.Variable;
import com.quaxantis.etui.TemplateValues;
import com.quaxantis.etui.template.parser.Binding;

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
        var map = new HashMap<String, TemplateValues.Entry>();
        var unspecifiedVariables = new ArrayList<Variable>();

        for (Variable variable : template.variables()) {
            Optional<TemplateValues.Entry> optEntry = template.tags(variable)
                    .stream()
                    .map(tagSet::getTag)
                    .flatMap(Optional::stream)
                    .collect(TemplateValuesSupport.toTemplateValue());
            optEntry.ifPresentOrElse(entry -> map.put(variable.name(), entry),
                                     () -> unspecifiedVariables.add(variable));
        }

        analyzeUnspecifiedVariables(unspecifiedVariables, map);
        unspecifiedVariables.forEach(variable -> map.computeIfAbsent(variable.name(), _ -> TemplateValues.Entry.of("")));

        return new TemplateValuesSupport(map);
    }

    private void analyzeUnspecifiedVariables(Collection<Variable> variables, Map<String, TemplateValues.Entry> specifiedVariables) {
        Map<String, String> boundVariables = specifiedVariables.entrySet().stream()
                .collect(filtering((Map.Entry<String, TemplateValues.Entry> entry) -> entry.getValue().value().isPresent(),
                                   toMap(Map.Entry::getKey, entry -> entry.getValue().value().orElse(null))));

        System.out.println(specifiedVariables.entrySet().stream()
                                   .filter(entry -> entry.getValue().value().isPresent())
                                   .map(entry -> "entry(\"%s\", \"%s\")".formatted(entry.getKey(), entry.getValue().value().orElse("")))
                                   .collect(Collectors.joining("," + System.lineSeparator(), "Map.ofEntries(" + System.lineSeparator(), ");"))
        );

        ExpressionEvaluator evaluator = this.template.expressionEvaluator();
        Collection<Binding> bindings = this.template.variables()
                .stream()
                .filter(variable -> variable.hasExpression() && boundVariables.get(variable.name()) != null)
                .peek(variable -> System.out.printf("Deinterpolating variable %s with value '%s' for expression %s%n", variable.name(), boundVariables.get(variable.name()), variable.expression()))
                .map(variable -> evaluator.deinterpolate(variable.expression(), boundVariables, boundVariables.get(variable.name())))
                .flatMap(Collection::stream)
                .toList();

        for (Variable variable : variables) {
            analyzeUnspecifiedVariable(variable, bindings);
        }

    }

    private void analyzeUnspecifiedVariable(Variable variable, Collection<Binding> bindings) {
        List<String> values = bindings.stream()
                .map(binding -> binding.valueOf(variable.name()).map(value -> value + " (" + binding.score() + ")"))
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        System.out.printf("--- possible values for variable %s: %s%n", variable.name(), values);
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
