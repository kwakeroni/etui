package com.quaxantis.etui.template.expression.eel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.quaxantis.etui.template.expression.eel.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes an OptPrefix expression")
public class EELExpressionAnalyzerOptPrefixExpressionTest {
    @Test
    @DisplayName("providing no match if the main expression does not match")
    void noMatchIfMainDoesNotMatch() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Text("my text"));
        assertThat(optPrefix).matching("a prefix and something else")
                .isEmpty();
    }

    @Test
    @DisplayName("providing no match if the prefix does not match")
    void noMatchIfPrefixDoesNotMatch() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Text("my text"));
        assertThat(optPrefix).matching("something and my text")
                .isEmpty();
    }

    @Test
    @DisplayName("providing an empty match if the main expression is empty and the prefix does not match")
    void noMatchIfMainEmpty() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Text(""));
        assertThat(optPrefix).matching("something else and ")
                .singleElement()
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[[something else and ]]}");
    }

    @Test
    @DisplayName("providing an empty match if the main expression is empty and the prefix matches")
    void emptyMatchIfMainEmptyAndPrefixMatches() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Text(""));
        assertThat(optPrefix).matching("a prefix and ")
                .singleElement()
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[[a prefix and ]]}")
                .bindings()
                .allFullyMatchExpressionOrEmpty();
    }


    @Test
    @DisplayName("providing a match if the prefix and the main expression match")
    void matchIfPrefixAndMainMatch() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Text("my text"));
        assertThat(optPrefix).matching("a prefix and my text")
                .singleElement()
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]a prefix and my text[]}")
                .bindings()
                .allFullyMatchExpression();
    }

    @Test
    @DisplayName("providing a match if the main expression is a variable")
    void matchMainVariable() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Identifier("var1"));
        assertThat(optPrefix).matching("a prefix and my text")
                .assertExactlyInAnyOrder(
                        match -> match.isFullMatch()
                                .hasMatchRepresentation("{[]a prefix and [my text]}")
                                .hasBindings(Map.of("var1", "my text")),
                        match -> match.isNotFullMatch()
                                .hasMatchRepresentation("{[[a prefix and my text]]}", "0<0")
                                .hasBindings(Map.of("var1", ""))
                )
                .bindings()
                .containValues(Map.of("var1", "my text"),
                               Map.of("var1", "")
                )
                .allFullyMatchExpressionOrEmpty();
    }

    @Test
    @DisplayName("providing a match if the prefix is a variable")
    void matchPrefixVariable() {
        var optPrefix = new Expression.OptPrefix(new Expression.Identifier("var1"), new Expression.Text("my text"));
        assertThat(optPrefix).matching("a prefix and my text")
                .singleElement()
                .isFullMatch()
                .hasBoundVariables()
                .hasMatchRepresentation("{[a prefix and ]my text[]}")
                .bindings()
                .containValues(Map.of("var1", "a prefix and "))
                .allFullyMatchExpression();
    }


    @Test
    @DisplayName("providing a match if the main expression and the prefix are variables separated by a text")
    void matchMainVariableAndPrefixVariable() {
        var optPrefix = new Expression.OptPrefix(new Expression.Identifier("var2"), new Expression.OptPrefix(new Expression.Text(" and "), new Expression.Identifier("var1")));
        assertThat(optPrefix).matching("a prefix and my text")
                .assertExactlyInAnyOrder(
                        match -> match.hasMatchRepresentation("{[a prefix] and [my text]}")
                                .hasBindings(Map.of("var1", "my text", "var2", "a prefix")),
                        match -> match.hasMatchRepresentation("{[[a prefix and my text]]}", "0<0")
                                .hasBindings(Map.of("var1", ""))
                )
                .bindings()
                .containValues(Map.of("var1", "my text", "var2", "a prefix"),
                               Map.of("var1", "")
                )
                .allFullyMatchExpressionOrEmpty();
    }
}
