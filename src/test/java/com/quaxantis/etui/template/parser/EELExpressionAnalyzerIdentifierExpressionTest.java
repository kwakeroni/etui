package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Nested
@DisplayName("EELExpressionAnalyzer analyzes an Identifier expression")
class EELExpressionAnalyzerIdentifierExpressionTest extends AbstractEELExpressionAnalyzerTest {
    @Test
    @DisplayName("providing a binding for a variable")
    void binding() {
        var bindings = analyzer.detectBindings(new Expression.Identifier("testVar"), "my test value");
        assertThat(bindings).containsExactly(
                new EELExpressionAnalyzer.Binding("testVar", "my test value")
        );
    }

    @Test
    @DisplayName("providing a match")
    void match() {
        var match = match(new Expression.Identifier("testVar"), "my test value");
        assertThat(match)
                .returns(true, Match::isFullMatch)
                .returns(true, Match::hasBoundVariables)
                .returns("my test value", valueOf("testVar"))
                .returns("{[[my test value]]}", Match::matchRepresentation);

    }
}
