package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Nested
@DisplayName("EELExpressionAnalyzer analyzes an Elvis expression")
class EELExpressionAnalyzerElvisExpressionTest extends AbstractEELExpressionAnalyzerTest {
    @Test
    @DisplayName("providing bindings for two identifiers")
    void twoIdentifiersBinding() {
        var elvis = new Expression.Elvis(new Expression.Identifier("var1"), new Expression.Identifier("var2"));
        var bindings = analyzer.detectBindings(elvis, "my test value");
        assertThat(bindings).containsExactly(
                new EELExpressionAnalyzer.Binding("var1", "my test value"),
                new EELExpressionAnalyzer.Binding("var1", "", EELExpressionAnalyzer.Bindings.of(new EELExpressionAnalyzer.Binding("var2", "my test value")))
        );

    }
}
