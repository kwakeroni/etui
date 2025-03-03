package com.quaxantis.etui;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public interface HasLabel {
    String label();

    @Nullable
    static String labelOf(Object o) {
        return (o instanceof HasLabel hasLabel) ? hasLabel.label() : null;
    }

    static String labelOf(Object o, Supplier<String> defaultSupplier) {
        if (o instanceof HasLabel hasLabel && hasLabel.label() != null) {
            return hasLabel.label();
        } else {
            return defaultSupplier.get();
        }
    }
}
