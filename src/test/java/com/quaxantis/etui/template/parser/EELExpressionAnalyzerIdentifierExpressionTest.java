package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes an Identifier expression")
class EELExpressionAnalyzerIdentifierExpressionTest {

    @Test
    @DisplayName("providing a binding match when there is no existing binding")
    void match() {
        var identifier = new Expression.Identifier("testVar");
        assertThat(identifier).matching("my test value")
                .isFullMatch()
                .hasBoundVariables()
                .hasBindings(Map.of("testVar", "my test value"))
                .hasMatchRepresentation("{[[my test value]]}")
                .hasOnlyBindingsMatching(identifier);
    }

    @Test
    @DisplayName("providing a literal match when there is a matching existing binding")
    void matchWithExistingBinding() {
        var identifier = new Expression.Identifier("testVar");
        assertThat(identifier).matching("my test value", Map.of("testVar", "my test value", "otherVar", "my other value"))
                .isFullMatch()
                .hasBoundVariables()
                .hasBindings(Map.of("testVar", "my test value"))
                .hasMatchRepresentation("{[[my test value]]}")
                .hasOnlyBindingsMatching(identifier);
    }


    @Test
    @DisplayName("providing no match when there is a conflicting existing binding")
    void noMatchWithConflictingBinding() {
        var identifier = new Expression.Identifier("testVar");
        assertThat(identifier).matching("my test value", Map.of("testVar", "my original value"))
                .isMatch()
                .hasBindings(Map.of("testVar", "my test value"));
    }
}
