package com.quaxantis.etui.template.parser.eel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.quaxantis.etui.template.parser.eel.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes an Elvis expression")
class EELExpressionAnalyzerElvisExpressionTest {
    @Test
    @DisplayName("providing no match if the main expression cannot be empty")
    void noMatchIfMainIsNotEmpty() {
        var elvis = new Expression.Elvis(new Expression.Text("my text"), new Expression.Text("my fallback"));
        assertThat(elvis).matching("my fallback")
                .isEmpty();
    }

    @Test
    @DisplayName("providing no match when the main expression is empty and the fallback does not match")
    void noMatchIfFallbackDoesNotMatch() {
        var elvis = new Expression.Elvis(new Expression.Text(""), new Expression.Text("my fallback"));
        assertThat(elvis).matching("something else")
                .isEmpty();
    }

    @Test
    @DisplayName("providing a match for a literal expression")
    void matchLiteral() {
        var elvis = new Expression.Elvis(new Expression.Text("my text"), new Expression.Text("my fallback"));
        assertThat(elvis).matching("my text")
                .singleElement()
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]my text[]}");
    }

    @Test
    @DisplayName("providing a match for a fallback expression if the main expression is empty")
    void matchLiteralFallback() {
        var elvis = new Expression.Elvis(new Expression.Text(""), new Expression.Text("my fallback"));
        assertThat(elvis).matching("my fallback")
                .singleElement()
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]my fallback[]}")
                .bindings()
                .allFullyMatchExpression();
    }

    @Test
    @DisplayName("providing a match for a variable")
    void matchVariable() {
        var elvis = new Expression.Elvis(new Expression.Identifier("var1"), new Expression.Text("my fallback"));
        assertThat(elvis).matching("my value")
                .singleElement()
                .isFullMatch()
                .hasMatchRepresentation("{[[my value]]}")
                .bindings()
                .containValues(Map.of("var1", "my value"))
                .allFullyMatchExpression();
    }

    @Test
    @DisplayName("providing a match for a fallback variable if the main expression is empty")
    void matchVariableWhenMainIsEmpty() {
        var elvis = new Expression.Elvis(new Expression.Text(""), new Expression.Identifier("var1"));
        assertThat(elvis).matching("my value")
                .singleElement()
                .isFullMatch()
                .hasMatchRepresentation("{[[my value]]}")
                .bindings()
                .containValues(Map.of("var1", "my value"))
                .allFullyMatchExpression();
    }

    @Test
    @DisplayName("providing matches choosing between two variables")
    void matchesChoosingBetween2Variables() {
        var elvis = new Expression.Elvis(new Expression.Identifier("var1"), new Expression.Identifier("var2"));
        assertThat(elvis).matching("my value")
                .assertAll(match -> match.isFullMatch()
                        .hasMatchRepresentation("{[[my value]]}"))
                .bindings()
                .containValues(
                        Map.of("var1", "my value"),
                        Map.of("var1", "", "var2", "my value")
                )
                .allFullyMatchExpression();
    }

    @Test
    @DisplayName("providing multiple matches choosing between multiple variables")
    void matchesChoosingBetweenMultipleVariables() {
        var elvis = new Expression.Elvis(new Expression.Elvis(new Expression.Identifier("var1"), new Expression.Identifier("var2")), new Expression.Identifier("var3"));
        assertThat(elvis).matching("my value")
                .assertAll(match -> match.isFullMatch()
                        .hasMatchRepresentation("{[[my value]]}"))
                .bindings()
                .containValues(
                        Map.of("var1", "my value"),
                        Map.of("var1", "", "var2", "my value"),
                        Map.of("var1", "", "var2", "", "var3", "my value")
                )
                .allFullyMatchExpression();
    }

    @Test
    @DisplayName("providing multiple matches choosing variables in nested expressions")
    void matchChoosingWithNestedExpressions() {
        var elvis = new Expression.Elvis(new Expression.Identifier("var1"), new Expression.Elvis(new Expression.Identifier("var2"), new Expression.Text("my")));
        assertThat(elvis).matching("my value my")
                .hasBindings(Map.of("var1", "my value my"),
                             Map.of("var1", "", "var2", "my value my"),
                             Map.of("var1", "", "var2", ""),
                             Map.of("var1", "", "var2", "") // duplicate binding in case of partial match
                );
    }
}
