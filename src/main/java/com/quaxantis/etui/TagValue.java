package com.quaxantis.etui;

public interface TagValue {

    Tag tag();

    String value();

    default String groupName() {
        return tag().groupName();
    }

    default String tagName() {
        return tag().tagName();
    }

    default boolean isEqualTo(TagValue other) {
        return value().equals(other.value()) && isSameTagAs(other);
    }

    default boolean isSameTagAs(TagValue other) {
        return tag().isSameTagAs(other.tag());
    }

    default boolean isSameTagAs(String otherGroup, String otherTag) {
        return tag().isSameTagAs(otherGroup, otherTag);
    }

    default String asString() {
        return "[%s%s] %s".formatted(
                groupName() == null ? "" : (groupName() + ":"),
                tagName(),
                value()
        );
    }

    /**
     * @Deprecated use {@link #of(Tag, String)} using a repository-obtained tag
     */
    @Deprecated
    static TagValue of(String group, String tag, String value) {
        return TagValue.of(Tag.of(group, tag), value);
    }

    static TagValue of(Tag tag, String value) {
        record Record(Tag tag, String value) implements TagValue {
            @Override
            public String toString() {
                return asString();
            }
        }
        return new Record(tag, value);
    }
}
