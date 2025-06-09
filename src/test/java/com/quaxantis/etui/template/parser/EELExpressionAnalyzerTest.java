package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.quaxantis.etui.template.parser.ExpressionAssert.assertThat;

@DisplayName("EELExpressionAnalyzer analyzes complex expressions")
class EELExpressionAnalyzerTest {

    @Test
    @DisplayName("providing multiple bindings for an ambiguous expression at a concat point")
    void matchAmbiguousAtConcat() {
        var concat = new Expression.Concat(
                new Expression.OptPrefix(new Expression.Text("prefix"), new Expression.Identifier("var1")),
                new Expression.OptSuffix(new Expression.Identifier("var2"), new Expression.Text("suffix")));

        assertThat(concat).matching("prefixsuffix")
                .satisfies(match -> {
                    match.bindings()
                            .forEach(binding -> {
                                System.out.println("-- " + binding + " (" + binding.matchRange().format("prefixsuffix") + ") " + (binding.matchRange().isEmpty() ? "empty" : "not empty"));
                                System.out.println(new ExpressionResolver(var -> binding.valueOf(var).orElse(""))
                                                           .resolve(concat));
                            });
                })
                .isFullMatch()
                .hasOnlyBindingsMatchingOrEmpty(concat)
                .hasBindings(Map.of("var1", "suffix", "var2", ""),
                             Map.of("var1", "", "var2", "prefix"),
                             Map.of("var1", "", "var2", ""))
        ;
    }

    @Test
    @DisplayName("providing multiple bindings for an ambiguous expression at the second part of a concat point")
    void matchAmbiguousHalfAtConcat() {
        var concat = new Expression.Concat(
                new Expression.OptPrefix(new Expression.Text("prefix"), new Expression.Identifier("var1")),
                new Expression.OptSuffix(new Expression.Identifier("var2"), new Expression.Text("suffix")),
                new Expression.Text("stop"));

        assertThat(concat).matching("prefixandstop").debugBindings()
                .isFullMatch()
                .hasOnlyBindingsMatchingExpressionOrEmpty()
                .hasBindings(Map.of("var1", "and", "var2", ""),
                             Map.of("var1", "", "var2", "")) // not happy about this one (reporting as full match)
        ;
    }

    @Test
    @DisplayName("providing only empty bindings for a collapsed section")
    void emptyMatchWithCollapsedSections() {
        String start = "prefix";
        String end = "suffix";
        var concat = new Expression.Concat(
                new Expression.Text(start),
                new Expression.OptPrefix(new Expression.Text("prefix"), new Expression.Identifier("var1")),
                new Expression.OptSuffix(new Expression.Identifier("var2"), new Expression.Text("suffix")),
                new Expression.Text(end));

        assertThat(concat).matching(start + end)
                .isFullMatch()
                .hasBindings(Map.of("var1", "", "var2", ""))
                .hasOnlyBindingsMatchingExpression();
    }

    @Test
    @Disabled
    @DisplayName("providing bindings for dot concat")
    void matchWithDotConcat() {
        var concat = new Expression.Concat(new Expression.Identifier("title"), new Expression.Text(". "), new Expression.Identifier("author"), new Expression.Text(". "), new Expression.Identifier("publisher"), new Expression.Text("."));

        assertThat(concat).matching("A Title. An Author. Publisher's.")
                .isFullMatch()
                .hasBindings(Map.of("title", "A Title", "author", "An Author", "publisher", "Publisher's"))
                .hasOnlyBindingsMatchingExpression();

    }

    @Test
    @Disabled
    @DisplayName("providing bindings for dot concat with optional element")
    void matchWithDotConcatWithOptionalElement() {
        var concat = new Expression.Concat(new Expression.Identifier("title"), new Expression.Text("."), new Expression.Identifier("author"), new Expression.Text("."), new Expression.Identifier("publisher"), new Expression.Text("."));

        assertThat(concat).matching("A Title. An Author. Publisher's.")
                .isFullMatch()
                .hasBindings(Map.of("title", "A Title", "author", "An Author", "publisher", "Publisher's"))
                .hasOnlyBindingsMatchingExpression();

    }

}
