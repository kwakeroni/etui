package com.quaxantis.etui.template;

import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.TagValue;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.TemplateValues;

import java.util.*;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
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

        for (com.quaxantis.etui.Template.Variable variable : template.variables()) {
            TemplateValues.Entry entry = template.tags(variable)
                    .stream()
                    .map(tagSet::getTag)
                    .flatMap(Optional::stream)
                    .collect(TemplateValuesSupport.toTemplateValue());

            map.put(variable.name(), entry);
        }

        return new TemplateValuesSupport(map);
    }

    public TagSet toTags(TemplateValues values) {
        return template.variables()
                .stream()
                .flatMap(variable -> tagValues(variable, values))
                .collect(TagSet.toTagSet());
    }

    private Stream<TagValue> tagValues(com.quaxantis.etui.Template.Variable variable, TemplateValues values) {
        return values.getValue(variable)
                .stream()
                .filter(not(String::isEmpty))
                .flatMap(value -> tagValue(variable, value));
    }

    private Stream<TagValue> tagValue(com.quaxantis.etui.Template.Variable variable, String value) {
        return template.tags(variable)
                .stream()
                .map(tag -> TagValue.of(tag, value));
    }
}
