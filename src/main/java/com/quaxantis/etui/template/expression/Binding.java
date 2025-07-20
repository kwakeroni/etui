package com.quaxantis.etui.template.expression;

import java.util.Optional;

public interface Binding {
    double score();

    Optional<String> valueOf(String variable);
}
