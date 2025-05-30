package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.EELExpressionAnalyzer.Binding;
import com.quaxantis.etui.template.parser.EELExpressionAnalyzer.Bindings;
import com.quaxantis.etui.template.parser.Expression.Elvis;
import com.quaxantis.etui.template.parser.Expression.Identifier;
import com.quaxantis.etui.template.parser.Expression.OptSuffix;
import com.quaxantis.etui.template.parser.Expression.Text;
import com.quaxantis.etui.template.parser.Match.NoMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EELExpressionAnalyzer")
class EELExpressionAnalyzerTest {

    private EELExpressionAnalyzer analyzer = new EELExpressionAnalyzer();

    @Test
    @DisplayName("Detects bindings for an unknown expression")
    void unknownExpressionBinding() {
        var bindings = analyzer.detectBindings(new Expression.Concat(), "");
        assertThat(bindings.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Detects no match for an unknown expression")
    void unknownExpressionNoMatch() {
        var match = analyzer.match(new Expression.Concat(), "test value");
        assertThat(match).isInstanceOfSatisfying(NoMatch.class, noMatch -> {
            assertThat(noMatch.reason()).contains("unknown expression", "Concat");
            assertThat(noMatch.hasBoundVariables()).isFalse();
        });
    }

    @Nested
    @DisplayName("Analyzes a text expression")
    class TextExpression {
        @Test
        @DisplayName("providing no bindings")
        void noBinding() {
            var text = new Text("my literal value");
            var bindings = analyzer.detectBindings(text, "my literal value");
            assertThat(bindings).isEmpty();
        }

        @Test
        @DisplayName("providing a full match")
        void fullMatch() {
            var text = new Text("my literal value");
            var match = analyzer.match(text, "my literal value");
            assertThat(match)
                    .returns(true, Match::isFullMatch)
                    .returns(false, Match::hasBoundVariables)
                    .returns("{[]my literal value[]}", Match::matchRepresentation);
        }

        @Test
        @DisplayName("providing no match")
        void noMatch() {
            var text = new Text("my literal value");
            var match = analyzer.match(text, "my other value");
            assertThat(match).isInstanceOf(NoMatch.class)
                    .returns(false, Match::isFullMatch)
                    .returns(false, Match::hasBoundVariables);
        }

        @Test
        @DisplayName("providing a partial match from the left")
        void partialMatchLeft() {
            var text = new Text("my lite");
            var match = analyzer.match(text, "my literal value");
            assertThat(match)
                    .returns(false, Match::isFullMatch)
                    .returns(false, Match::hasBoundVariables)
                    .returns("{[]my lite[]}ral value", Match::matchRepresentation);
        }

        @Test
        @DisplayName("providing a partial match from the right")
        void partialMatchRight() {
            var text = new Text("al value");
            var match = analyzer.match(text, "my literal value");
            assertThat(match)
                    .returns(false, Match::isFullMatch)
                    .returns(false, Match::hasBoundVariables)
                    .returns("my liter{[]al value[]}", Match::matchRepresentation);
        }


        @Test
        @DisplayName("providing a partial match in the middle")
        void partialMatchMid() {
            var text = new Text("eral val");
            var match = analyzer.match(text, "my literal value");
            assertThat(match)
                    .returns(false, Match::isFullMatch)
                    .returns(false, Match::hasBoundVariables)
                    .returns("my lit{[]eral val[]}ue", Match::matchRepresentation);
        }

        // TODO: multiple partial matches
    }

    @Nested
    @DisplayName("Analyzes an Identifier expression")
    class IdentifierExpression {
        @Test
        @DisplayName("providing a binding for a variable")
        void binding() {
            var bindings = analyzer.detectBindings(new Identifier("testVar"), "my test value");
            assertThat(bindings).containsExactly(
                    new Binding("testVar", "my test value")
            );
        }

        @Test
        @DisplayName("providing a match")
        void match() {
            var match = analyzer.match(new Identifier("testVar"), "my test value");
            assertThat(match)
                    .returns(true, Match::isFullMatch)
                    .returns(true, Match::hasBoundVariables)
                    .returns("my test value", valueOf("testVar"))
                    .returns("{[[my test value]]}", Match::matchRepresentation);

        }
    }

    @Nested
    @DisplayName("Analyzes an Elvis expression")
    class ElvisExpression {
        @Test
        @DisplayName("providing bindings for two identifiers")
        void twoIdentifiersBinding() {
            var elvis = new Elvis(new Identifier("var1"), new Identifier("var2"));
            var bindings = analyzer.detectBindings(elvis, "my test value");
            assertThat(bindings).containsExactly(
                    new Binding("var1", "my test value"),
                    new Binding("var1", "", Bindings.of(new Binding("var2", "my test value")))
            );

        }
    }


    @Nested
    @DisplayName("Analyzes an OptSuffix expression")
    class OptSuffixExpression {

        @Test
        @DisplayName("providing a match for a literal with a literal suffix")
        void matchLiteralWithLiteralSuffix() {
            var optSuffix = new OptSuffix(new Text("a literal"), new Text(". with a suffix"));
            var match = analyzer.match(optSuffix, "a literal. with a suffix");
            assertThat(match)
                    .returns(true, Match::isFullMatch)
                    .returns(false, Match::hasBoundVariables)
                    .returns("{[]a literal. with a suffix[]}", Match::matchRepresentation);
        }

        @Test
        @DisplayName("providing a partial match for a literal with a literal suffix")
        void partialMatchLiteralWithLiteralSuffix() {
            var optSuffix = new OptSuffix(new Text("a literal"), new Text(". with a suffix"));
            var match = analyzer.match(optSuffix, "also a literal. with a suffix");
            assertThat(match)
                    .returns(false, Match::isFullMatch)
                    .returns(false, Match::hasBoundVariables)
                    .returns("also {[]a literal. with a suffix[]}", Match::matchRepresentation);
        }

        @Test
        @DisplayName("providing no match if the prefix doesn't match")
        void nomatchForPrefix() {
            var optSuffix = new OptSuffix(new Text("a$literal"), new Text(". with a suffix"));
            var match = analyzer.match(optSuffix, "a literal. with a suffix");
            assertThat(match).isInstanceOf(NoMatch.class);
        }

        @Test
        @DisplayName("providing no match if the suffix doesn't match")
        void nomatchForSuffix() {
            var optSuffix = new OptSuffix(new Text("a literal"), new Text(". with a$suffix"));
            var match = analyzer.match(optSuffix, "a literal. with a suffix");
            assertThat(match).isInstanceOf(NoMatch.class);
        }

        @Test
        @DisplayName("providing no match if there is a gap")
        void nomatchForGap() {
            var optSuffix = new OptSuffix(new Text("a literal"), new Text(" with a suffix"));
            var match = analyzer.match(optSuffix, "a literal. with a suffix");
            assertThat(match).isInstanceOf(NoMatch.class);
        }

        @Test
        @DisplayName("providing no match if there is a negative gap")
        void nomatchForNegativeGap() {
            var optSuffix = new OptSuffix(new Text("a literal."), new Text(". with a suffix"));
            var match = analyzer.match(optSuffix, "a literal. with a suffix");
            assertThat(match).isInstanceOf(NoMatch.class);
        }


        @Test
        @DisplayName("providing a match for an identifier with a literal suffix")
        void matchIdentifierWithLiteralSuffix() {
            var optSuffix = new OptSuffix(new Identifier("var1"), new Text(". with suffix"));
            var match = analyzer.match(optSuffix, "my test value. with suffix");
            assertThat(match).as(match.toString())
                    .returns(true, Match::isFullMatch)
                    .returns(true, Match::hasBoundVariables)
                    .returns("my test value", valueOf("var1"))
                    .returns("{[my test value]. with suffix[]}", Match::matchRepresentation);
        }

        @Test
        @DisplayName("providing a match for a literal with an identifier suffix")
        void matchLiteralWithIdentifierSuffix() {
            var optSuffix = new OptSuffix(new Text("prefix with "), new Identifier("var1"));
            var match = analyzer.match(optSuffix, "prefix with my test value");
            assertThat(match).as(match.toString())
                    .returns(true, Match::isFullMatch)
                    .returns(true, Match::hasBoundVariables)
                    .returns("my test value", valueOf("var1"))
                    .returns("{[]prefix with [my test value]}", Match::matchRepresentation);
        }

        @Test
        @DisplayName("providing a match for an identifier with an identifier suffix")
        void matchIdentifierWithIdentifierSuffix() {
            var optSuffix = new OptSuffix(new Identifier("var1"), new Identifier("var2"));
            var match = analyzer.match(optSuffix, "my test value");
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
            var optSuffix = new OptSuffix(new Identifier("var1"), new Text(". with suffix"));
            var match = analyzer.match(optSuffix, ". with suffix");
            assertThat(match).isInstanceOf(NoMatch.class);
        }


        @Test
        @DisplayName("providing a match for an identifier with a complex suffix")
        void matchIdentifierWithComplexSuffix() {
            var optSuffix = new OptSuffix(new Identifier("var1"), new OptSuffix(new Text(" with suffix "), new Identifier("var2")));
            var match = analyzer.match(optSuffix, "my test value with suffix another value");
            assertThat(match).as(match.toString())
                    .returns(true, Match::isFullMatch)
                    .returns(true, Match::hasBoundVariables)
                    .returns("my test value", valueOf("var1"))
                    .returns("another value", valueOf("var2"))
                    .returns("{[my test value] with suffix [another value]}", Match::matchRepresentation);
        }
    }


    private static Function<Match, String> valueOf(String variable) {
        return match -> {
            var optionalValue = match.valueOf(variable);
            assertThat(optionalValue).isNotEmpty();
            return optionalValue.orElseThrow();
        };
    }
}
