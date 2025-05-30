package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Nested
@DisplayName("EELExpressionAnalyzer analyzes a text expression")
class EELExpressionAnalyzerTextExpressionTest extends AbstractEELExpressionAnalyzerTest {
    @Test
    @DisplayName("providing no bindings")
    void noBinding() {
        var text = new Expression.Text("my literal value");
        var bindings = analyzer.detectBindings(text, "my literal value");
        assertThat(bindings).isEmpty();
    }

    @Test
    @DisplayName("providing a full match")
    void fullMatch() {
        var text = new Expression.Text("my literal value");
        assertThat(match(text, "my literal value"))
                .returns(true, Match::isFullMatch)
                .returns(false, Match::hasBoundVariables)
                .returns("{[]my literal value[]}", Match::matchRepresentation);
    }

    @Test
    @DisplayName("providing no match")
    void noMatch() {
        var text = new Expression.Text("my literal value");
        var match = match(text, "my other value");
        assertThat(match).isInstanceOf(Match.NoMatch.class)
                .returns(false, Match::isFullMatch)
                .returns(false, Match::hasBoundVariables);
    }

    @Test
    @DisplayName("providing a partial match from the left")
    void partialMatchLeft() {
        var text = new Expression.Text("my lite");
        var match = match(text, "my literal value");
        assertThat(match)
                .returns(false, Match::isFullMatch)
                .returns(false, Match::hasBoundVariables)
                .returns("{[]my lite[]}ral value", Match::matchRepresentation);
    }

    @Test
    @DisplayName("providing a partial match from the right")
    void partialMatchRight() {
        var text = new Expression.Text("al value");
        var match = match(text, "my literal value");
        assertThat(match)
                .returns(false, Match::isFullMatch)
                .returns(false, Match::hasBoundVariables)
                .returns("my liter{[]al value[]}", Match::matchRepresentation);
    }


    @Test
    @DisplayName("providing a partial match in the middle")
    void partialMatchMid() {
        var text = new Expression.Text("eral val");
        var match = match(text, "my literal value");
        assertThat(match)
                .returns(false, Match::isFullMatch)
                .returns(false, Match::hasBoundVariables)
                .returns("my lit{[]eral val[]}ue", Match::matchRepresentation);
    }

    // TODO: multiple partial matches
}
