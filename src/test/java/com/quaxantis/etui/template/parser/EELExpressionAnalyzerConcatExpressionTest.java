package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes a Concat expression")
public class EELExpressionAnalyzerConcatExpressionTest {

    @Test
    @DisplayName("providing an empty match for an empty concat")
    void matchEmptyConcat() {
        var concat = new Expression.Concat();
        assertThat(concat).matching("an expression")
                .isNotInstanceOf(Match.NoMatch.class)
                .isNotFullMatch()
                .hasNoBoundVariables()
                .hasOnlyBindingsMatchingOrEmpty(concat)
                .hasMatchRepresentation("{[][]}an expression");
    }


    @Test
    @DisplayName("providing a match for a single part")
    void matchConcatSingle() {
        var concat = new Expression.Concat(new Expression.Text("an expression"));
        assertThat(concat).matching("an expression")
                .isFullMatch()
                .hasNoBoundVariables()
                .hasOnlyBindingsMatching(concat)
                .hasMatchRepresentation("{[]an expression[]}");
    }

    @Test
    @DisplayName("providing a match for two matching expressions")
    void matchIfTwoExpressionsConcat() {
        var concat = new Expression.Concat(new Expression.Text("one "), new Expression.Text("expression"));
        assertThat(concat).matching("one expression")
                .isFullMatch()
                .hasNoBoundVariables()
                .hasOnlyBindingsMatching(concat)
                .hasMatchRepresentation("{[]one expression[]}");
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
                .isFullMatch()
                .hasNoBoundVariables()
                .hasOnlyBindingsMatching(concat)
                .hasMatchRepresentation("{[]one two three four five expressions[]}");
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
                .isFullMatch()
                .hasBoundVariables()
                .hasMatchRepresentation("{[]one two three four five expressions[]}")
                .hasBindings(Map.of("var1", "three"))
                .hasOnlyBindingsMatching(concat);
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
                .isFullMatch()
                .hasBoundVariables()
                .hasMatchRepresentation("{[]one two three four five expressions[]}")
                .hasBindings(Map.of("var1", "three", "var2", "five"))
                .hasOnlyBindingsMatching(concat);
    }

    @Test
    @DisplayName("providing no match")
    void noMatch() {
        var concat = new Expression.Concat(new Expression.Text("one "),
                                           new Expression.Text("two "),
                                           new Expression.Text("three "),
                                           new Expression.Text("expressions"));
        assertThat(concat).matching("one deux three expressions")
                .isInstanceOf(Match.NoMatch.class)
                .hasNoBoundVariables()
                .hasMatchRepresentation("{[][]}one deux three expressions");
    }


    @Test
    @DisplayName("providing a match with multiple matching literals")
    void matchWithDotConcat() {
        var concat = new Expression.Concat(new Expression.Identifier("title"), new Expression.Text(". "), new Expression.Identifier("author"), new Expression.Text(". "), new Expression.Identifier("publisher"), new Expression.Text("."));

        assertThat(concat).matching("A Title. An Author. Publisher's.")
                .isFullMatch()
                .hasBindings(Map.of("title", "A Title", "author", "An Author", "publisher", "Publisher's"))
                .hasOnlyBindingsMatchingExpression();

    }

}
