package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes an OptSuffix expression")
class EELExpressionAnalyzerOptSuffixExpressionTest {

    @Test
    @DisplayName("providing a match for a literal with a literal suffix")
    void matchLiteralWithLiteralSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal"), new Expression.Text(". with a suffix"));
        assertThat(optSuffix).matching("a literal. with a suffix")
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]a literal. with a suffix[]}");
    }

    @Test
    @DisplayName("providing a partial match for a literal with a literal suffix")
    void partialMatchLiteralWithLiteralSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal"), new Expression.Text(". with a suffix"));
        assertThat(optSuffix).matching("also a literal. with a suffix")
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("also {[]a literal. with a suffix[]}");
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
    @DisplayName("providing a match for an identifier with a literal suffix")
    void matchIdentifierWithLiteralSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.Text(". with suffix"));
        assertThat(optSuffix).matching("my test value. with suffix")
                .isFullMatch()
                .hasBoundVariables()
                .hasVariableValue("var1", "my test value")
                .hasMatchRepresentation("{[my test value]. with suffix[]}");
    }

    @Test
    @DisplayName("providing a match for a literal with an identifier suffix")
    void matchLiteralWithIdentifierSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("prefix with "), new Expression.Identifier("var1"));
        assertThat(optSuffix).matching("prefix with my test value")
                .isFullMatch()
                .hasBoundVariables()
                .hasVariableValue("var1", "my test value")
                .hasMatchRepresentation("{[]prefix with [my test value]}");
    }

    @Test
    @DisplayName("providing a match for an identifier with an identifier suffix")
    void matchIdentifierWithIdentifierSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.Identifier("var2"));
        assertThat(optSuffix).matching("my test value")
                .isFullMatch()
                .hasBoundVariables()
                .hasVariableValue("var1", "my test value")
                .hasVariableValue("var2", "my test value")
                .hasMatchRepresentation("{[[my test value]]}");
    }

    @Test
    @DisplayName("providing no match for an empty identifier with a literal suffix")
    void nomatchForEmptyIdentifierWithLiteralSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.Text(". with suffix"));
        assertThat(optSuffix).matching(". with suffix").isNoMatch();
    }


    @Test
    @DisplayName("providing a match for an identifier with a complex suffix")
    void matchIdentifierWithComplexSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.OptSuffix(new Expression.Text(" with suffix "), new Expression.Identifier("var2")));
        assertThat(optSuffix).matching("my test value with suffix another value")
                .isFullMatch()
                .hasBoundVariables()
                .hasVariableValue("var1", "my test value")
                .hasVariableValue("var2", "another value")
                .hasMatchRepresentation("{[my test value] with suffix [another value]}");
    }
}
