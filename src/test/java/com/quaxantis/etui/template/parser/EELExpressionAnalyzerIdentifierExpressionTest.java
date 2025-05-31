package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes an Identifier expression")
class EELExpressionAnalyzerIdentifierExpressionTest {

    @Test
    @DisplayName("providing a match")
    void match() {
        assertThat(new Expression.Identifier("testVar")).matching("my test value")
                .isFullMatch()
                .hasBoundVariables()
                .hasVariableValue("testVar", "my test value")
                .hasMatchRepresentation("{[[my test value]]}");

    }
}
