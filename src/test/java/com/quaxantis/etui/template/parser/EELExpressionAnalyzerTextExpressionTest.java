package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;
import static com.quaxantis.etui.template.parser.MatchAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes a text expression")
class EELExpressionAnalyzerTextExpressionTest {

    @Test
    @DisplayName("providing a match of an empty literal")
    void emptyLiteralMatch() {
        var text = new Expression.Text("");
        assertThat(text).matching("my literal value")
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[[my literal value]]}")
                .hasOnlyBindingsMatchingExpressionOrEmpty();
    }

    @Test
    @DisplayName("providing a full match")
    void fullMatch() {
        var text = new Expression.Text("my literal value");
        assertThat(text).matching("my literal value")
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]my literal value[]}")
                .hasOnlyBindingsMatching(text);
    }

    @Test
    @DisplayName("providing no match")
    void noMatch() {
        var text = new Expression.Text("my literal value");
        assertThat(text).matching("my other value")
                .isNoMatch()
                .isNotFullMatch()
                .hasNoBoundVariables();
    }

    @Test
    @DisplayName("providing a partial match from the left")
    void partialMatchLeft() {
        var text = new Expression.Text("my lite");
        assertThat(text).matching("my literal value")
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]my lite[]}ral value")
                .hasOnlyBindingsPartiallyMatching(text);
    }

    @Test
    @DisplayName("providing a partial match from the right")
    void partialMatchRight() {
        var text = new Expression.Text("al value");
        assertThat(text).matching("my literal value")
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("my liter{[]al value[]}")
                .hasOnlyBindingsPartiallyMatching(text);
    }


    @Test
    @DisplayName("providing a partial match in the middle")
    void partialMatchMid() {
        var text = new Expression.Text("eral val");
        assertThat(text).matching("my literal value")
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("my lit{[]eral val[]}ue")
                .hasOnlyBindingsPartiallyMatching(text);
    }

    @Test
    @DisplayName("providing a choice if multiple matches")
    void multipleMatches() {
        var text = new Expression.Text("my");
        assertThat(text).matching("my match my match")
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[[my match my]]} match")
                .hasChoicesSatisfying(first -> assertThat(first).hasMatchRepresentation("{[]my[]} match my match"),
                                      second -> assertThat(second).hasMatchRepresentation("my match {[]my[]} match")
                ).hasOnlyBindingsPartiallyMatching(text);
    }

    @Test
    @DisplayName("providing a choice if multiple matches with repeated pattern")
    void multipleMatchesWithRepeatedPattern() {
        var text = new Expression.Text("mymy");
        assertThat(text).matching("mymymymy")
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[[mymymymy]]}")
                .hasChoicesSatisfying(first -> assertThat(first).hasMatchRepresentation("{[]mymy[]}mymy"),
                                      second -> assertThat(second).hasMatchRepresentation("my{[]mymy[]}my"),
                                      third -> assertThat(third).hasMatchRepresentation("mymy{[]mymy[]}")
                ).hasOnlyBindingsPartiallyMatching(text);
    }
}
