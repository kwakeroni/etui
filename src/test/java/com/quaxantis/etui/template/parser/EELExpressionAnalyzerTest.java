package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.Match.NoMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EELExpressionAnalyzer")
class EELExpressionAnalyzerTest extends AbstractEELExpressionAnalyzerTest {

    @Test
    @DisplayName("Detects bindings for an unknown expression")
    void unknownExpressionBinding() {
        var bindings = analyzer.detectBindings(new Expression.Concat(), "");
        assertThat(bindings.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Detects no match for an unknown expression")
    void unknownExpressionNoMatch() {
        var concat = new Expression.Concat();
        assertThat(match(concat, "test value"))
                .isInstanceOfSatisfying(NoMatch.class, noMatch -> {
                    assertThat(noMatch.reason()).contains("unknown expression", "Concat");
                    assertThat(noMatch.hasBoundVariables()).isFalse();
                });
    }
}
