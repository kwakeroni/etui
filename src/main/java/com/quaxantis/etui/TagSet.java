package com.quaxantis.etui;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TagSet implements Iterable<TagValue> {
    private final List<TagValue> tags;

    private TagSet(List<TagValue> tags) {
        this.tags = tags;
    }

    @Override
    public Iterator<TagValue> iterator() {
        return tags.iterator();
    }

    public Stream<TagValue> stream() {
        return tags.stream();
    }

    public int size() {
        return tags.size();
    }

    public Optional<TagValue> getTag(Tag tag) {
        return getTag(tag.groupName(), tag.tagName());
    }

    public Optional<String> getValue(Tag tag) {
        return getTag(tag).map(TagValue::value);
    }

    public Optional<TagValue> getTag(String group, String tag) {
        return tags.stream().filter(tagValue -> tagValue.isSameTagAs(group, tag)).findAny();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TagSet.class.getSimpleName() + "[", "]")
                .add(String.valueOf(tags))
                .toString();
    }

    public static Collector<TagValue, ?, TagSet> toTagSet() {
        return Collectors.collectingAndThen(Collectors.toUnmodifiableList(), TagSet::new);
    }

    public static TagSet of(TagValue... values) {
        return new TagSet(List.of(values));
    }

    public boolean isEmpty() {
        return tags.isEmpty();
    }
}
