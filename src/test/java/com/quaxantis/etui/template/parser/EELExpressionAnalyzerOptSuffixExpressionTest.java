package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;
import static com.quaxantis.etui.template.parser.MatchAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes an OptSuffix expression")
class EELExpressionAnalyzerOptSuffixExpressionTest {

    @Test
    @DisplayName("providing a match for a literal with a literal suffix")
    void matchLiteralWithLiteralSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal"), new Expression.Text(". with a suffix"));
        assertThat(optSuffix).matching("a literal. with a suffix")
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]a literal. with a suffix[]}")
                .hasOnlyBindingsMatching(optSuffix);
    }

    @Test
    @DisplayName("providing a partial match for a literal with a literal suffix")
    void partialMatchLiteralWithLiteralSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal"), new Expression.Text(". with a suffix"));
        assertThat(optSuffix).matching("also a literal. with a suffix")
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("also {[]a literal. with a suffix[]}")
                .hasOnlyBindingsPartiallyMatching(optSuffix);
    }

    @Test
    @DisplayName("providing no match if the prefix doesn't match")
    void nomatchForPrefix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a$literal"), new Expression.Text(". with a suffix"));
        assertThat(optSuffix).matching("a literal. with a suffix").isNoMatch();
    }

    @Test
    @DisplayName("providing no match if the suffix doesn't match")
    void nomatchForSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal"), new Expression.Text(". with a$suffix"));
        assertThat(optSuffix).matching("a literal. with a suffix").isNoMatch();
    }

    @Test
    @DisplayName("providing no match if there is a gap")
    void nomatchForGap() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal"), new Expression.Text(" with a suffix"));
        assertThat(optSuffix).matching("a literal. with a suffix").isNoMatch();
    }

    @Test
    @DisplayName("providing no match if there is a negative gap")
    void nomatchForNegativeGap() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal."), new Expression.Text(". with a suffix"));
        assertThat(optSuffix).matching("a literal. with a suffix").isNoMatch();
    }


    @Test
    @DisplayName("providing a match if the main expression is a variable")
    void matchMainVariable() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.Text(". with suffix"));
        assertThat(optSuffix).matching("my test value. with suffix")
                .isFullMatch()
                .hasBoundVariables()
                .hasMatchRepresentation("{[[my test value. with suffix]]}")
                .hasBindings(Map.of("var1", "my test value"),
                             Map.of("var1", ""))
                .hasOnlyBindingsMatchingOrEmpty(optSuffix)
                .isChoiceOfSatisfying(choices -> assertThat(choices)
                        .satisfiesExactlyInAnyOrder(
                                matchWithVar -> assertThat(matchWithVar)
                                        .hasMatchRepresentation("{[my test value]. with suffix[]}")
                                        .hasBindings(Map.of("var1", "my test value")),
                                emptyMatch -> assertThat(emptyMatch)
                                        .hasMatchRepresentation("{[[my test value. with suffix]]}", "0<0")
                                        .hasBindings(Map.of("var1", ""))
                        )
                );
    }

    @Test
    @DisplayName("providing a match if the suffix is a variable")
    void matchSuffixVariable() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("prefix with "), new Expression.Identifier("var1"));
        assertThat(optSuffix).matching("prefix with my test value")
                .isFullMatch()
                .hasBoundVariables()
                .hasBindings(Map.of("var1", "my test value"))
                .hasOnlyBindingsMatching(optSuffix)
                .hasMatchRepresentation("{[]prefix with [my test value]}");
    }

    @Test
    @DisplayName("providing a match for an identifier with an identifier suffix")
    void matchIdentifierWithIdentifierSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.Identifier("var2"));
        assertThat(optSuffix).matching("my test value")
                .isFullMatch()
                .hasBoundVariables()
                .hasBindings(Map.of("var1", "my test value", "var2", "my test value"),
                             Map.of("var1", ""))
                .hasOnlyBindingsMatchingOrEmpty(optSuffix)
                .hasMatchRepresentation("{[[my test value]]}");
    }

    @Test
    @DisplayName("providing no match for an empty identifier with a literal suffix")
    void nomatchForEmptyIdentifierWithLiteralSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.Text(". with suffix"));
        assertThat(optSuffix).matching(". with suffix")
                .isNotFullMatch()
                .hasBoundVariables()
                .hasBindings(Map.of("var1", ""))
                .hasOnlyBindingsMatchingOrEmpty(optSuffix);
    }


    @Test
    @DisplayName("providing a match for an identifier with a complex suffix")
    void matchIdentifierWithComplexSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.OptSuffix(new Expression.Text(" with suffix "), new Expression.Identifier("var2")));
        assertThat(optSuffix).matching("my test value with suffix another value")
                .isFullMatch()
                .hasBoundVariables()
                .hasBindings(Map.of("var1", "my test value", "var2", "another value"),
                             Map.of("var1", ""))
                .hasOnlyBindingsMatchingOrEmpty(optSuffix)
                .hasMatchRepresentation("{[[my test value with suffix another value]]}");
    }
}
