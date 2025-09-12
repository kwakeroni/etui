package com.quaxantis.etui.tag;

import com.quaxantis.etui.TagFamily;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class TagGroupCollector {
    private TagGroupCollector() {
        throw new UnsupportedOperationException();
    }

    public static Collector<Map.Entry<String, ? extends TagFamily>, ?, List<TagGroup>> collector() {
        return new Impl();
    }

    private static class Impl implements Collector<Map.Entry<String, ? extends TagFamily>, MutableTagGroup, List<TagGroup>> {
        @Override
        public Supplier<MutableTagGroup> supplier() {
            return () -> new MutableTagGroup(null);
        }

        @Override
        public BiConsumer<MutableTagGroup, Map.Entry<String, ? extends TagFamily>> accumulator() {
            return (group, entry) -> {
                String[] keys = entry.getKey().split("/");
                Deque<String> groupNames = new ArrayDeque<>(Arrays.asList(keys));
                group.addTagFamily(groupNames, entry.getValue());
            };
        }

        @Override
        public BinaryOperator<MutableTagGroup> combiner() {
            return null;
        }

        @Override
        public Function<MutableTagGroup, List<TagGroup>> finisher() {
            return group -> group.subGroups.values().stream()
                    .map(MutableTagGroup::toImmutableGroup)
                    .toList();
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }

    record MutableTagGroup(String name,
                           SequencedMap<String, MutableTagGroup> subGroups,
                           List<TagFamily> families) {
        public MutableTagGroup(String name) {
            this(name, new LinkedHashMap<>(), new ArrayList<>());
        }

        void addTagFamily(TagFamily tagFamily) {
            this.families.add(tagFamily);
        }

        void addTagFamily(Deque<String> groupNames, TagFamily tagFamily) {
            if (groupNames.isEmpty()) {
                addTagFamily(tagFamily);
            } else {
                subGroups.computeIfAbsent(groupNames.removeFirst().trim(), MutableTagGroup::new).addTagFamily(groupNames, tagFamily);
            }
        }

        TagGroup toImmutableGroup() {
            List<TagGroup> groups = this.subGroups.values().stream().map(MutableTagGroup::toImmutableGroup).toList();
            return new TagGroup(this.name, groups, this.families);
        }
    }
}
