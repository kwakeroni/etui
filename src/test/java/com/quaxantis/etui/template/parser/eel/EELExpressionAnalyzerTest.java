package com.quaxantis.etui.template.parser.eel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.quaxantis.etui.template.parser.eel.ExpressionAssert.assertThat;
import static java.util.Map.entry;

@DisplayName("EELExpressionAnalyzer analyzes complex expressions")
class EELExpressionAnalyzerTest {

    @Test
    @DisplayName("providing multiple bindings for an ambiguous expression at a concat point")
    void matchAmbiguousAtConcat() {
        var concat = new Expression.Concat(
                new Expression.OptPrefix(new Expression.Text("prefix"), new Expression.Identifier("var1")),
                new Expression.OptSuffix(new Expression.Identifier("var2"), new Expression.Text("suffix")));

        assertThat(concat).matching("prefixsuffix")
                .bindings()
                .allFullyMatchExpressionOrEmpty()
                .containValues(Map.of("var1", "suffix", "var2", ""),
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

        assertThat(concat).matching("prefixandstop")
                .bindings()
                .allFullyMatchExpressionOrEmpty()
                .containValues(Map.of("var1", "and", "var2", ""),
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
                .bindings()
                .allFullyMatchExpression()
                .containValues(Map.of("var1", "", "var2", ""));
    }

    @Test
    @DisplayName("providing bindings for dot concat with optional element")
    void matchWithDotConcatWithOptionalElement() {
        var concat = new Expression.Concat(new Expression.Identifier("title"), new Expression.Text("."),
                                           new Expression.OptSuffix(new Expression.OptPrefix(new Expression.Text(" "), new Expression.Identifier("author")), new Expression.Text(".")),
                                           new Expression.Text(" "), new Expression.Identifier("publisher"), new Expression.Text("."));

        assertThat(concat).matching("A Title. An Author. Publisher's.")
                .bindings()
                .allPartiallyMatchExpression()
                .containValues(Map.of("title", "A Title", "author", "An Author", "publisher", "Publisher's"),
                               Map.of("title", "A Title. An Author", "publisher", "Publisher's", "author", ""),
                               Map.of("title", "A Title", "publisher", "An Author. Publisher's", "author", ""),
                               Map.of("title", "A Title", "publisher", "An Author", "author", "") // partial match
                );
    }

    @Test
    @DisplayName("providing a combined binding")
    void combinedBinding() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var1"), new Expression.Text(" there"));

        assertThat(optSuffix)
                .matching("Hello there", Map.of("var2", "value"))
                .bindings()
                .filteredOnScore(1.0)
                .allFullyMatchExpression()
                .containValues(Map.of("var1", "Hello"))
        ;
    }

    @Test
    @DisplayName("providing bindings for dot concat with original binding")
    void matchWithDotConcatWithOriginalBinding() {
        var concat = new Expression.Concat(new Expression.Identifier("title"), new Expression.Text("."),
                                           new Expression.OptSuffix(new Expression.OptPrefix(new Expression.Text(" "), new Expression.Identifier("author")), new Expression.Text(".")),
                                           new Expression.Text(" "), new Expression.Identifier("publisher"), new Expression.Text("."));

        assertThat(concat).matching("A Title. An Author. Publisher's.", Map.of("author", "An Author"))
                .bindings()
                .filteredOnScore(1.0)
                .allFullyMatchExpression()
                .containValues(Map.of("title", "A Title", "author", "An Author", "publisher", "Publisher's"));
    }

    @Test
    @DisplayName("providing bindings allowing for an empty variable replacing an original binding")
    void matchWithEmptyReplacingOriginalBinding() {
        var concat = new Expression.Concat(new Expression.Identifier("title"), new Expression.Text("."),
                                           new Expression.OptSuffix(new Expression.OptPrefix(new Expression.Text(" "), new Expression.Identifier("author")), new Expression.Text(".")),
                                           new Expression.Text(" "), new Expression.Identifier("publisher"), new Expression.Text("."));


        assertThat(concat)
                .matching("A Title. Publisher's.",
                          Map.of("author", "An author", "title", "A Title", "publisher", "Publisher's"))
                .hasBindings(Map.of("title", "A Title", "publisher", "Publisher's", "author", ""));
    }

    @Test
    @DisplayName("using an earlier binding as a given binding in a succeeding expression")
    void matchWithEarlierBindingAsGiven() {
        var optSuffix = new Expression.OptSuffix(new Expression.Identifier("var"),
                                                 new Expression.OptPrefix(new Expression.Text(", "), new Expression.Identifier("var")));

        // TODO Could be improved with "delegated binding" to keep track
        assertThat(optSuffix)
                .matching("New York, New York")
                .bindings()
                .filteredOnScore(0.9)
                .containValues(Map.of("var", "New York"));

    }

    @Test
    @DisplayName("does not run out of memory when evaluating a realistic expression")
    void noOutOfMemory() {
        var expression = EELExpressionEvaluator.parser().parse("${name}${' jg. '+?volume}${' nr. '+?number}.${' '+?(coverDisplayDate?:coverDate)?+'.'}${' '+?creatorName?+'.'}${' '+?publisher?+'.'}");
        var variables = Map.ofEntries(
                entry("date", "1988:06:03"),
                entry("creator", "https://www.tue.nl/"),
                entry("subject", "Weekblad voor personeelsleden en studenten van de Technische Universiteit Eindhoven"),
                entry("creatorName", "Technische Universiteit Eindhoven"),
                entry("language", "nl"),
                entry("title", "Cursor jaargang 30 nummer 37"),
                entry("type", "Text"),
                entry("url", "https://www.cursor.tue.nl/magazine-archief/volume/30/"),
                entry("volume", "30"),
                entry("number", "37"),
                entry("issn", "0920-6876"),
                entry("subtitle", "Weekblad voor personeelsleden en studenten van de Technische Universiteit Eindhoven"),
                entry("publisher", "Technische Universiteit Eindhoven"),
                entry("comment", "Cursor jg. 30 nr. 37. 03.06.88. Technische Universiteit Eindhoven."),
                entry("originPlatform", "Print"),
                entry("coverDate", "1988:06:03"),
                entry("coverDisplayDate", "03.06.88")
        );

        String string = "Cursor jg. 30 nr. 37. 03.06.88. Technische Universiteit Eindhoven.";

        Map<String, String> commonBindings = Map.of("name", "Cursor", "volume", "30", "number", "37", "coverDisplayDate", "03.06.88");

        long start = System.currentTimeMillis();
        assertThat(expression)
                .matching(string, variables)
                .bindings()
                .filteredOnScore(0.5)
                .containValues(combine(commonBindings, Map.of("creatorName", "Technische Universiteit Eindhoven", "publisher", "")),
                               combine(commonBindings, Map.of("creatorName", "", "publisher", "Technische Universiteit Eindhoven")));
    }

    private Map<String, String> combine(Map<String, String> one, Map<String, String> two) {
        var map = new HashMap<String, String>();
        map.putAll(one);
        map.putAll(two);
        return map;
    }
}
