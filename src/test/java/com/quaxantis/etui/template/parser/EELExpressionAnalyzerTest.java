package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EELExpressionAnalyzer")
class EELExpressionAnalyzerTest {

    @Test
    @DisplayName("Detects no match for an unknown expression")
    void unknownExpressionNoMatch() {
        var concat = new Expression.Concat();
        assertThat(concat).matching("test value")
                .hasNoBoundVariables()
                .isNoMatchSatisfying(
                        noMatch -> assertThat(noMatch.reason()).contains("unknown expression", "Concat"))
        ;
    }
}
