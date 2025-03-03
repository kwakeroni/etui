package com.quaxantis.etui;

import javax.annotation.Nullable;

public interface HasDescription {
    String description();

    @Nullable
    static String descriptionOf(Object o) {
        return (o instanceof HasDescription hasDescription) ? hasDescription.description() : null;
    }
}
