package com.quaxantis.etui.template.expression.eel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.quaxantis.etui.template.expression.eel.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes a text expression")
class EELExpressionAnalyzerTextExpressionTest {

    @Test
    @DisplayName("providing a match of an empty literal")
    void emptyLiteralMatch() {
        var text = new Expression.Text("");
        assertThat(text).matching("my literal value")
                .singleElement()
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[[my literal value]]}")
                .bindings()
                .allFullyMatchExpressionOrEmpty();
    }

    @Test
    @DisplayName("providing a full match")
    void fullMatch() {
        var text = new Expression.Text("my literal value");
        assertThat(text).matching("my literal value")
                .singleElement()
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]my literal value[]}")
                .bindings()
                .allFullyMatchExpressionOrEmpty();
    }

    @Test
    @DisplayName("providing no match")
    void noMatch() {
        var text = new Expression.Text("my literal value");
        assertThat(text).matching("my other value")
                .isEmpty();
    }

    @Test
    @DisplayName("providing a partial match from the left")
    void partialMatchLeft() {
        var text = new Expression.Text("my lite");
        assertThat(text).matching("my literal value")
                .singleElement()
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]my lite[]}ral value")
                .bindings()
                .allPartiallyMatchExpression();
    }

    @Test
    @DisplayName("providing a partial match from the right")
    void partialMatchRight() {
        var text = new Expression.Text("al value");
        assertThat(text).matching("my literal value")
                .singleElement()
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("my liter{[]al value[]}")
                .bindings()
                .allPartiallyMatchExpression();
    }


    @Test
    @DisplayName("providing a partial match in the middle")
    void partialMatchMid() {
        var text = new Expression.Text("eral val");
        assertThat(text).matching("my literal value")
                .singleElement()
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("my lit{[]eral val[]}ue")
                .bindings()
                .allPartiallyMatchExpression();
    }

    @Test
    @DisplayName("providing multiple matches")
    void multipleMatches() {
        var text = new Expression.Text("my");
        assertThat(text).matching("my match my match")
                .hasOnlyEmptyBindings()
                .assertAll(match -> match.isNotFullMatch()
                        .hasNoBoundVariables())
                .assertExactlyInAnyOrder(
                        match -> match.hasMatchRepresentation("{[]my[]} match my match"),
                        match -> match.hasMatchRepresentation("my match {[]my[]} match")
                ).bindings()
                .allPartiallyMatchExpression();
    }

    @Test
    @DisplayName("providing multiple matches with repeated pattern")
    void multipleMatchesWithRepeatedPattern() {
        var text = new Expression.Text("mymy");
        assertThat(text).matching("mymymymy")
                .hasOnlyEmptyBindings()
                .assertAll(match -> match.isNotFullMatch()
                        .hasNoBoundVariables())
                .assertExactlyInAnyOrder(
                        match -> match.hasMatchRepresentation("{[]mymy[]}mymy"),
                        match -> match.hasMatchRepresentation("my{[]mymy[]}my"),
                        match -> match.hasMatchRepresentation("mymy{[]mymy[]}")
                ).bindings()
                .allPartiallyMatchExpression();
    }
}
