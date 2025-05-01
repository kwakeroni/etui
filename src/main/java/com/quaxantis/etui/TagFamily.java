package com.quaxantis.etui;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface TagFamily extends HasLabel {
    Optional<String> defaultGroup();

    boolean defaultReadOnly();

    List<? extends Tag> tags();

    default TagFamily filter(Predicate<Tag> predicate) {
        List<? extends Tag> filtered = tags().stream().filter(predicate).toList();
        class Filtered extends TagFamilyAdapter {
            private Filtered() {
                super(TagFamily.this);
            }

            @Override
            public List<? extends Tag> tags() {
                return filtered;
            }
        }
        return new Filtered();
    }
}
