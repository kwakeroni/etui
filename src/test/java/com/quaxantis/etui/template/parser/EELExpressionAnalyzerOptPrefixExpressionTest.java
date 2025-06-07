package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;
import static com.quaxantis.etui.template.parser.MatchAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes an OptPrefix expression")
public class EELExpressionAnalyzerOptPrefixExpressionTest {
    @Test
    @DisplayName("providing no match if the main expression does not match")
    void noMatchIfMainDoesNotMatch() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Text("my text"));
        assertThat(optPrefix).matching("a prefix and something else")
                .isNoMatch()
                .hasNoBoundVariables();
    }

    @Test
    @DisplayName("providing no match if the prefix does not match")
    void noMatchIfPrefixDoesNotMatch() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Text("my text"));
        assertThat(optPrefix).matching("something and my text")
                .isNoMatch()
                .hasNoBoundVariables();
    }

    @Test
    @DisplayName("providing an empty match if the main expression is empty and the prefix does not match")
    void noMatchIfMainEmpty() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Text(""));
        assertThat(optPrefix).matching("something else and ")
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[][]}something else and ");
    }

    @Test
    @DisplayName("providing an empty match if the main expression is empty and the prefix matches")
    void emptyMatchIfMainEmptyAndPrefixMatches() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Text(""));
        assertThat(optPrefix).matching("a prefix and ")
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[][]}a prefix and ")
                .hasOnlyBindingsMatchingOrEmpty(optPrefix);
    }


    @Test
    @DisplayName("providing a match if the prefix and the main expression match")
    void matchIfPrefixAndMainMatch() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Text("my text"));
        assertThat(optPrefix).matching("a prefix and my text")
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]a prefix and my text[]}")
                .hasOnlyBindingsMatching(optPrefix);
    }

    @Test
    @DisplayName("providing a match if the main expression is a variable")
    void matchMainVariable() {
        var optPrefix = new Expression.OptPrefix(new Expression.Text("a prefix and "), new Expression.Identifier("var1"));
        assertThat(optPrefix).matching("a prefix and my text")
                .isFullMatch()
                .hasBoundVariables()
                .hasMatchRepresentation("{[[a prefix and my text]]}")
                .hasBindings(Map.of("var1", "my text"),
                             Map.of("var1", "")
                )
                .hasOnlyBindingsMatchingOrEmpty(optPrefix)
                .isChoiceOfSatisfying(choices -> assertThat(choices)
                        .satisfiesExactlyInAnyOrder(
                                matchWithVar -> assertThat(matchWithVar)
                                        .hasMatchRepresentation("{[]a prefix and [my text]}")
                                        .hasBindings(Map.of("var1", "my text")),
                                emptyMatch -> assertThat(emptyMatch)
                                        .hasMatchRepresentation("{[[a prefix and my text]]}", "0<0")
                                        .hasBindings(Map.of("var1", ""))
                        )
                );
    }

    @Test
    @DisplayName("providing a match if the prefix is a variable")
    void matchPrefixVariable() {
        var optPrefix = new Expression.OptPrefix(new Expression.Identifier("var1"), new Expression.Text("my text"));
        assertThat(optPrefix).matching("a prefix and my text")
                .isFullMatch()
                .hasBoundVariables()
                .hasMatchRepresentation("{[a prefix and ]my text[]}")
                .hasBindings(Map.of("var1", "a prefix and "))
                .hasOnlyBindingsMatching(optPrefix);
    }


    @Test
    @DisplayName("providing a match if the main expression and the prefix are variables separated by a text")
    void matchMainVariableAndPrefixVariable() {
        var optPrefix = new Expression.OptPrefix(new Expression.Identifier("var2"), new Expression.OptPrefix(new Expression.Text(" and "), new Expression.Identifier("var1")));
        assertThat(optPrefix).matching("a prefix and my text")
                .isFullMatch()
                .hasBoundVariables()
                .hasMatchRepresentation("{[[a prefix and my text]]}")
                .hasBindings(Map.of("var1", "my text", "var2", "a prefix"),
                             Map.of("var1", "")
                )
                .hasOnlyBindingsMatchingOrEmpty(optPrefix)
                .isChoiceOfSatisfying(choices -> assertThat(choices)
                        .satisfiesExactlyInAnyOrder(
                                matchWithVar -> assertThat(matchWithVar)
                                        .hasMatchRepresentation("{[a prefix] and [my text]}")
                                        .hasBindings(Map.of("var1", "my text", "var2", "a prefix")),
                                emptyMatch -> assertThat(emptyMatch)
                                        .hasMatchRepresentation("{[[a prefix and my text]]}", "0<0")
                                        .hasBindings(Map.of("var1", ""))
                        )
                );
    }
}
