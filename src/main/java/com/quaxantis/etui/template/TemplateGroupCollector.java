package com.quaxantis.etui.template;

import com.quaxantis.etui.Template;
import com.quaxantis.etui.swing.template.TemplateGroup;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class TemplateGroupCollector {

    public static Collector<Map.Entry<String, ? extends Template>, ?, List<TemplateGroup>> collector() {
        return new Impl();
    }

    private static class Impl implements Collector<Map.Entry<String, ? extends Template>, MutableTemplateGroup, List<TemplateGroup>> {

        @Override
        public Supplier<MutableTemplateGroup> supplier() {
            return () -> new MutableTemplateGroup(null);
        }

        @Override
        public BiConsumer<MutableTemplateGroup, Map.Entry<String, ? extends Template>> accumulator() {
            return (group, entry) -> {
                String[] keys = entry.getKey().split("/");
                Deque<String> groupNames = new ArrayDeque<>(Arrays.asList(keys));
                group.addTemplate(groupNames, entry.getValue());
            };
        }

        @Override
        public BinaryOperator<MutableTemplateGroup> combiner() {
            return null;
        }

        @Override
        public Function<MutableTemplateGroup, List<TemplateGroup>> finisher() {
            return group -> group.subGroups().values().stream()
                    .map(MutableTemplateGroup::toImmutableGroup)
                    .toList();
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }

    record MutableTemplateGroup(String name,
                                SequencedMap<String, MutableTemplateGroup> subGroups,
                                List<Template> templates) {
        MutableTemplateGroup(String name) {
            this(name, new LinkedHashMap<>(), new ArrayList<>());
        }

        void addTemplate(Template template) {
            this.templates.add(template);
        }

        void addTemplate(Deque<String> groupNames, Template template) {
            if (groupNames.isEmpty()) {
                addTemplate(template);
            } else {
                subGroups.computeIfAbsent(groupNames.removeFirst().trim(), MutableTemplateGroup::new).addTemplate(groupNames, template);
            }
        }

        TemplateGroup toImmutableGroup() {
            List<TemplateGroup> groups = this.subGroups.values().stream().map(MutableTemplateGroup::toImmutableGroup).toList();
            return new TemplateGroup(this.name, groups, this.templates);
        }
    }

}
