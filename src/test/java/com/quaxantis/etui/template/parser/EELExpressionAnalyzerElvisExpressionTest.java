package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes an Elvis expression")
class EELExpressionAnalyzerElvisExpressionTest {
    @Test
    @DisplayName("providing no match if the main expression cannot be empty")
    void noMatchIfMainIsNotEmpty() {
        var elvis = new Expression.Elvis(new Expression.Text("my text"), new Expression.Text("my fallback"));
        assertThat(elvis).matching("my fallback")
                .isNoMatch()
                .hasNoBoundVariables();
    }

    @Test
    @DisplayName("providing no match when the main expression is empty and the fallback does not match")
    void noMatchIfFallbackDoesNotMatch() {
        var elvis = new Expression.Elvis(new Expression.Text(""), new Expression.Text("my fallback"));
        assertThat(elvis).matching("something else")
                .isNoMatch()
                .hasNoBoundVariables();
    }

    @Test
    @DisplayName("providing a match for a literal expression")
    void matchLiteral() {
        var elvis = new Expression.Elvis(new Expression.Text("my text"), new Expression.Text("my fallback"));
        assertThat(elvis).matching("my text")
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]my text[]}");
    }

    @Test
    @DisplayName("providing a match for a fallback expression if the main expression is empty")
    void matchLiteralFallback() {
        var elvis = new Expression.Elvis(new Expression.Text(""), new Expression.Text("my fallback"));
        assertThat(elvis).matching("my fallback")
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]my fallback[]}")
                .hasOnlyBindingsMatching(elvis);
    }

    @Test
    @DisplayName("providing a match for a variable")
    void matchVariable() {
        var elvis = new Expression.Elvis(new Expression.Identifier("var1"), new Expression.Text("my fallback"));
        MatchAssert matchAssert = assertThat(elvis).matching("my value")
                .isFullMatch();
        matchAssert.hasBindings(Map.of("var1", "my value"))
                .hasMatchRepresentation("{[[my value]]}")
                .hasOnlyBindingsMatching(elvis);
    }

    @Test
    @DisplayName("providing a match for a fallback variable if the main expression is empty")
    void matchVariableWhenMainIsEmpty() {
        var elvis = new Expression.Elvis(new Expression.Text(""), new Expression.Identifier("var1"));
        assertThat(elvis).matching("my value")
                .isFullMatch()
                .satisfies(match -> {
                    List<Map<String, String>> list = match.bindings().map(Binding::asMap).toList();
                    System.out.println(list);
                }).hasBindings(Map.of("var1", "my value"))
                .hasMatchRepresentation("{[][my value]}")
                .hasOnlyBindingsMatching(elvis);
    }

    @Test
    @DisplayName("providing a match which is a choice between two variables")
    void matchVariableWithChoice() {
        var elvis = new Expression.Elvis(new Expression.Identifier("var1"), new Expression.Identifier("var2"));
        assertThat(elvis).matching("my value")
                .isFullMatch()
                .hasMatchRepresentation("{[[my value]]}")
                .hasBindings(
                        Map.of("var1", "my value"),
                        Map.of("var1", "", "var2", "my value")
                )
                .hasOnlyBindingsMatching(elvis);
    }

    @Test
    @DisplayName("providing a match which is a choice between multiple variables")
    void matchMultiVariableWithChoice() {
        var elvis = new Expression.Elvis(new Expression.Elvis(new Expression.Identifier("var1"), new Expression.Identifier("var2")), new Expression.Identifier("var3"));
        assertThat(elvis).matching("my value")
                .isFullMatch()
                .hasMatchRepresentation("{[[my value]]}")
                .hasBindings(
                        Map.of("var1", "my value"),
                        Map.of("var1", "", "var2", "my value"),
                        Map.of("var1", "", "var2", "", "var3", "my value")
                )
                .hasOnlyBindingsMatching(elvis);
    }
}
