package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes a text expression")
class EELExpressionAnalyzerTextExpressionTest {

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

    // TODO: multiple partial matches
}
