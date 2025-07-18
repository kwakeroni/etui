package com.quaxantis.etui.template.parser.eel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.quaxantis.etui.template.parser.eel.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes a Concat expression")
public class EELExpressionAnalyzerConcatExpressionTest {

    @Test
    @DisplayName("providing an empty match for an empty concat")
    void matchEmptyConcat() {
        var concat = new Expression.Concat();
        assertThat(concat).matching("an expression")
                .singleElement()
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[][]}an expression")
                .bindings()
                .allFullyMatchExpressionOrEmpty();
    }


    @Test
    @DisplayName("providing a match for a single part")
    void matchConcatSingle() {
        var concat = new Expression.Concat(new Expression.Text("an expression"));
        assertThat(concat).matching("an expression")
                .singleElement()
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]an expression[]}")
                .bindings()
                .allFullyMatchExpressionOrEmpty();
    }

    @Test
    @DisplayName("providing a match for two matching expressions")
    void matchIfTwoExpressionsConcat() {
        var concat = new Expression.Concat(new Expression.Text("one "), new Expression.Text("expression"));
        assertThat(concat).matching("one expression")
                .singleElement()
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]one expression[]}")
                .bindings()
                .allFullyMatchExpressionOrEmpty();
    }

    @Test
    @DisplayName("providing a match for multiple matching expressions")
    void matchIfMultipleExpressionsConcat() {
        var concat = new Expression.Concat(new Expression.Text("one "),
                                           new Expression.Text("two "),
                                           new Expression.Text("three "),
                                           new Expression.Text("four "),
                                           new Expression.Text("five "),
                                           new Expression.Text("expressions"));
        assertThat(concat).matching("one two three four five expressions")
                .singleElement()
                .isFullMatch()
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[]one two three four five expressions[]}")
                .bindings()
                .allFullyMatchExpressionOrEmpty();
    }


    @Test
    @DisplayName("providing a match for multiple matching expressions and an identifier")
    void matchIfMultipleExpressionsAndVariableConcat() {
        var concat = new Expression.Concat(new Expression.Text("one "),
                                           new Expression.Text("two "),
                                           new Expression.Identifier("var1"),
                                           new Expression.Text(" four "),
                                           new Expression.Text("five "),
                                           new Expression.Text("expressions"));
        assertThat(concat).matching("one two three four five expressions")
                .singleElement()
                .isFullMatch()
                .hasBoundVariables()
                .hasMatchRepresentation("{[]one two three four five expressions[]}")
                .bindings()
                .containValues(Map.of("var1", "three"))
                .allFullyMatchExpressionOrEmpty();
    }

    @Test
    @DisplayName("providing a match for multiple matching expressions and multiple identifiers")
    void matchIfMultipleExpressionsAndMultipleVariablesConcat() {
        var concat = new Expression.Concat(new Expression.Text("one "),
                                           new Expression.Text("two "),
                                           new Expression.Identifier("var1"),
                                           new Expression.Text(" four "),
                                           new Expression.Identifier("var2"),
                                           new Expression.Text(" expressions"));
        assertThat(concat).matching("one two three four five expressions")
                .singleElement()
                .isFullMatch()
                .hasBoundVariables()
                .hasMatchRepresentation("{[]one two three four five expressions[]}")
                .bindings()
                .containValues(Map.of("var1", "three", "var2", "five"))
                .allFullyMatchExpressionOrEmpty();
    }

    @Test
    @DisplayName("providing no match")
    void noMatch() {
        var concat = new Expression.Concat(new Expression.Text("one "),
                                           new Expression.Text("two "),
                                           new Expression.Text("three "),
                                           new Expression.Text("expressions"));
        assertThat(concat).matching("one deux three expressions")
                .isEmpty();
    }


    @Test
    @DisplayName("providing a match with multiple matching literals")
    void matchWithDotConcat() {
        var concat = new Expression.Concat(new Expression.Identifier("title"), new Expression.Text(". "), new Expression.Identifier("author"), new Expression.Text(". "), new Expression.Identifier("publisher"), new Expression.Text("."));

        assertThat(concat).matching("A Title. An Author. Publisher's.")
                .singleElement()
                .isFullMatch()
                .bindings()
                .containValues(Map.of("title", "A Title", "author", "An Author", "publisher", "Publisher's"))
                .allFullyMatchExpressionOrEmpty();
    }
}
