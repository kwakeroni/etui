package com.quaxantis.etui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Objects;

import static java.util.Comparator.*;

public interface Tag {
    @Nullable
    String groupName();

    @Nonnull
    String tagName();

    default boolean isSameTagAs(Tag tag) {
        return isSameTagAs(tag.groupName(), tag.tagName());
    }

    default boolean isSameTagAs(String otherGroup, String otherTag) {
        return equalsIgnoreCase(groupName(), otherGroup)
               && equalsIgnoreCase(tagName(), otherTag);
    }

    default String qualifiedName() {
        return "%s%s".formatted(
                groupName() == null ? "" : (groupName() + ":"),
                tagName()
        );
    }


    default String asString() {
        return "[%s]".formatted(qualifiedName());
    }

    /*
     * @deprecated Fetch tags from repository
     */
    @Deprecated
    static Tag of(String groupName, String tagName) {
        Objects.requireNonNull(tagName, "tagName");
        return new Tag() {
            @Override
            public String groupName() {
                return groupName;
            }

            @Nonnull
            @Override
            public String tagName() {
                return tagName;
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    /*
     * @deprecated Fetch tags from repository
     */
    @Deprecated
    static Tag ofQualifiedName(String qualifiedName) {
        String[] parts = qualifiedName.split(":");
        if (parts.length == 1) {
            return Tag.of(null, parts[0]);
        } else if (parts.length == 2) {
            return Tag.of(parts[0], parts[1]);
        } else {
            throw new IllegalArgumentException("Incorrect qualified name: " + qualifiedName);
        }
    }


    @SuppressWarnings("StringEquality")
    private static boolean equalsIgnoreCase(String a, String b) {
        return (a == b) || (a != null && a.equalsIgnoreCase(b));
    }

    static Comparator<Tag> COMPARATOR = comparing(Tag::groupName, nullsFirst(naturalOrder()))
            .thenComparing(Tag::tagName, nullsFirst(naturalOrder()));

}
