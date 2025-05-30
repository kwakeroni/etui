package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Nested
@DisplayName("EELExpressionAnalyzer analyzes an OptSuffix expression")
class EELExpressionAnalyzerOptSuffixExpressionTest extends AbstractEELExpressionAnalyzerTest {

    @Test
    @DisplayName("providing a match for a literal with a literal suffix")
    void matchLiteralWithLiteralSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal"), new Expression.Text(". with a suffix"));
        var match = match(optSuffix, "a literal. with a suffix");
        assertThat(match)
                .returns(true, Match::isFullMatch)
                .returns(false, Match::hasBoundVariables)
                .returns("{[]a literal. with a suffix[]}", Match::matchRepresentation);
    }

    @Test
    @DisplayName("providing a partial match for a literal with a literal suffix")
    void partialMatchLiteralWithLiteralSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal"), new Expression.Text(". with a suffix"));
        var match = match(optSuffix, "also a literal. with a suffix");
        assertThat(match)
                .returns(false, Match::isFullMatch)
                .returns(false, Match::hasBoundVariables)
                .returns("also {[]a literal. with a suffix[]}", Match::matchRepresentation);
    }

    @Test
    @DisplayName("providing no match if the prefix doesn't match")
    void nomatchForPrefix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a$literal"), new Expression.Text(". with a suffix"));
        var match = match(optSuffix, "a literal. with a suffix");
        assertThat(match).isInstanceOf(Match.NoMatch.class);
    }

    @Test
    @DisplayName("providing no match if the suffix doesn't match")
    void nomatchForSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal"), new Expression.Text(". with a$suffix"));
        var match = match(optSuffix, "a literal. with a suffix");
        assertThat(match).isInstanceOf(Match.NoMatch.class);
    }

    @Test
    @DisplayName("providing no match if there is a gap")
    void nomatchForGap() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal"), new Expression.Text(" with a suffix"));
        var match = match(optSuffix, "a literal. with a suffix");
        assertThat(match).isInstanceOf(Match.NoMatch.class);
    }

    @Test
    @DisplayName("providing no match if there is a negative gap")
    void nomatchForNegativeGap() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("a literal."), new Expression.Text(". with a suffix"));
        var match = match(optSuffix, "a literal. with a suffix");
        assertThat(match).isInstanceOf(Match.NoMatch.class);
    }


    @Test
    @DisplayName("providing a match for an identifier with a literal suffix")
    void matchIdentifierWithLiteralSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.Text(". with suffix"));
        var match = match(optSuffix, "my test value. with suffix");
        assertThat(match).as(match.toString())
                .returns(true, Match::isFullMatch)
                .returns(true, Match::hasBoundVariables)
                .returns("my test value", valueOf("var1"))
                .returns("{[my test value]. with suffix[]}", Match::matchRepresentation);
    }

    @Test
    @DisplayName("providing a match for a literal with an identifier suffix")
    void matchLiteralWithIdentifierSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Text("prefix with "), new Expression.Identifier("var1"));
        var match = match(optSuffix, "prefix with my test value");
        assertThat(match).as(match.toString())
                .returns(true, Match::isFullMatch)
                .returns(true, Match::hasBoundVariables)
                .returns("my test value", valueOf("var1"))
                .returns("{[]prefix with [my test value]}", Match::matchRepresentation);
    }

    @Test
    @DisplayName("providing a match for an identifier with an identifier suffix")
    void matchIdentifierWithIdentifierSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.Identifier("var2"));
        var match = match(optSuffix, "my test value");
        assertThat(match).as(match.toString())
                .returns(true, Match::isFullMatch)
                .returns(true, Match::hasBoundVariables)
                .returns("my test value", valueOf("var1"))
                .returns("my test value", valueOf("var2"))
                .returns("{[[my test value]]}", Match::matchRepresentation);
    }

    @Test
    @DisplayName("providing no match for an empty identifier with a literal suffix")
    void nomatchForEmptyIdentifierWithLiteralSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.Text(". with suffix"));
        var match = match(optSuffix, ". with suffix");
        assertThat(match).isInstanceOf(Match.NoMatch.class);
    }


    @Test
    @DisplayName("providing a match for an identifier with a complex suffix")
    void matchIdentifierWithComplexSuffix() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.OptSuffix(new Expression.Text(" with suffix "), new Expression.Identifier("var2")));
        var match = match(optSuffix, "my test value with suffix another value");
        assertThat(match).as(match.toString())
                .returns(true, Match::isFullMatch)
                .returns(true, Match::hasBoundVariables)
                .returns("my test value", valueOf("var1"))
                .returns("another value", valueOf("var2"))
                .returns("{[my test value] with suffix [another value]}", Match::matchRepresentation);
    }
}
