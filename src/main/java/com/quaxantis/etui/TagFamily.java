package com.quaxantis.etui;

import java.util.List;

public interface TagFamily extends HasLabel {
    List<? extends Tag> tags();

    static TagFamily of(String label, List<? extends Tag> tags) {
        List<? extends Tag> tagsCopy = List.copyOf(tags);
        return new TagFamily() {
            @Override
            public List<? extends Tag> tags() {
                return tagsCopy;
            }

            @Override
            public String label() {
                return label;
            }
        };
    }
}
