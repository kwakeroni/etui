package com.quaxantis.etui.template.expression.eel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.quaxantis.etui.template.expression.eel.ExpressionAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes an Identifier expression")
class EELExpressionAnalyzerIdentifierExpressionTest {

    @Test
    @DisplayName("providing a binding match when there is no existing binding")
    void match() {
        var identifier = new Expression.Identifier("testVar");
        assertThat(identifier).matching("my test value")
                .singleElement()
                .isFullMatch()
                .hasBoundVariables()
                .hasMatchRepresentation("{[[my test value]]}")
                .bindings()
                .containValues(Map.of("testVar", "my test value"))
                .allFullyMatchExpression();
    }

    @Test
    @DisplayName("providing a literal match when there is a matching existing binding")
    void matchWithExistingBinding() {
        var identifier = new Expression.Identifier("testVar");
        assertThat(identifier).matching("my test value", Map.of("testVar", "my test value", "otherVar", "my other value"))
                .singleElement()
                .isFullMatch()
                .hasBoundVariables()
                .hasMatchRepresentation("{[[my test value]]}")
                .bindings()
                .containValues(Map.of("testVar", "my test value"))
                .allFullyMatchExpression();
    }


    @Test
    @DisplayName("providing a match when there is a conflicting existing binding")
    void noMatchWithConflictingBinding() {
        var identifier = new Expression.Identifier("testVar");
        assertThat(identifier).matching("my test value", Map.of("testVar", "my original value"))
                .singleElement()
                .isFullMatch()
                .hasBindings(Map.of("testVar", "my test value"))
                .satisfies(match -> assertThat(match.score()).isLessThan(0.3));
    }
}
