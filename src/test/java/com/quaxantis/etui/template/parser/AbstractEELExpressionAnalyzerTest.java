package com.quaxantis.etui.template.parser;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractEELExpressionAnalyzerTest {
    protected EELExpressionAnalyzer analyzer = new EELExpressionAnalyzer();

    protected Match match(Expression text, String string) {
        return analyzer.match(text, string);
    }

    protected static Function<Match, String> valueOf(String variable) {
        return match -> {
            var optionalValue = match.valueOf(variable);
            assertThat(optionalValue).isNotEmpty();
            return optionalValue.orElseThrow();
        };
    }
}
