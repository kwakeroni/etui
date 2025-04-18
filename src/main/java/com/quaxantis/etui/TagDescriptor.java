package com.quaxantis.etui;

import javax.annotation.Nonnull;
import java.util.List;

public interface TagDescriptor extends HasLabel, HasDescription {

    Tag tag();
    boolean isReadOnly();
    String formatDescription();
    List<? extends Example> examples();

    static boolean isReadOnly(Tag tag) {
        return tag instanceof TagDescriptor descriptor && descriptor.isReadOnly();
    }

    interface Example {
        String text();
        String pattern();
        String value();
    }

    public static TagDescriptor of(@Nonnull Tag tag) {
        if (tag instanceof TagDescriptor descriptor) {
            return descriptor;
        }

        return new TagDescriptor() {
            @Override
            public Tag tag() {
                return tag;
            }

            @Override
            public boolean isReadOnly() {
                return false;
            }

            @Override
            public String label() {
                return HasLabel.labelOf(tag, tag::tagName);
            }

            @Override
            public String description() {
                return HasDescription.descriptionOf(tag);
            }

            @Override
            public String formatDescription() {
                return null;
            }

            @Override
            public List<? extends Example> examples() {
                return List.of();
            }
        };
    }
}
