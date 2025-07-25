package com.quaxantis.etui.template;

import com.quaxantis.etui.TagValue;
import com.quaxantis.etui.TemplateValues;
import com.quaxantis.support.util.MoreCollectors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

class TemplateValuesSupport implements TemplateValues {
    private final Map<String, Entry> values;

    public TemplateValuesSupport(Map<String, Entry> map) {
        this.values = new HashMap<>(map);
    }

    static Collector<TagValue, ?, Optional<Entry>> toTemplateValue() {
        return Collectors.teeing(
                Collectors.mapping(
                        TagValue::value,
                        MoreCollectors.singleDistinctValueOrElseNull()),
                Collectors.mapping(
                        tagValue -> Source.of(tagValue.tag().qualifiedName(), tagValue.value()),
                        Collectors.toList()),
                (value, sources) -> (value == null && sources.isEmpty()) ? Optional.empty() : Optional.of(Entry.of(value, sources))
        );
    }


    @Override
    public void set(com.quaxantis.etui.Template.Variable var, String value) {
        Entry valueObj = get(var).orElse(Entry.EMPTY);
        set(var, valueObj.withValue(value));
    }

    public void set(com.quaxantis.etui.Template.Variable var, Entry value) {
        values.put(var.name(), value);
    }

    @Override
    public Optional<Entry> get(com.quaxantis.etui.Template.Variable var) {
        return Optional.ofNullable(values.get(var.name()));
    }

}
