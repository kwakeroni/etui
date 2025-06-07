package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes an Identifier expression")
class EELExpressionAnalyzerIdentifierExpressionTest {

    @Test
    @DisplayName("providing a match")
    void match() {
        var identifier = new Expression.Identifier("testVar");
        assertThat(identifier).matching("my test value")
                .isFullMatch()
                .hasBoundVariables()
                .hasBindings(Map.of("testVar", "my test value"))
                .hasMatchRepresentation("{[[my test value]]}")
                .hasOnlyBindingsMatching(identifier);
    }
}
