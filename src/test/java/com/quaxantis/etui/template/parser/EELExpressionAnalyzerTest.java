package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.EELExpressionAnalyzer.Binding;
import com.quaxantis.etui.template.parser.EELExpressionAnalyzer.Bindings;
import com.quaxantis.etui.template.parser.EELExpressionAnalyzer.Match;
import com.quaxantis.etui.template.parser.EELExpressionAnalyzer.Match.NoMatch;
import com.quaxantis.etui.template.parser.Expression.Elvis;
import com.quaxantis.etui.template.parser.Expression.Identifier;
import com.quaxantis.etui.template.parser.Expression.OptSuffix;
import com.quaxantis.etui.template.parser.Expression.Text;
import org.junit.jupiter.api.Disabled;
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
        assertThat(bindings.isEmpty());
    }

    @Test
    @DisplayName("Detects no match for an unknown expression")
    void unknownExpressionNoMatch() {
        var match = analyzer.match(new Expression.Concat(), "");
        assertThat(match).isEqualTo(new NoMatch());
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
            assertThat(match).returns("my literal value", whenAppliedTo("my literal value"));
        }

        @Test
        @DisplayName("providing no match")
        void noMatch() {
            var text = new Text("my literal value");
            var match = analyzer.match(text, "my other value");
            assertThat(match).isInstanceOf(NoMatch.class);
        }

        @Test
        @DisplayName("providing a partial match from the left")
        void partialMatchLeft() {
            var text = new Text("my lite");
            var match = analyzer.match(text, "my literal value");
            assertThat(match).returns("my lite", whenAppliedTo("my literal value"));
        }

        @Test
        @DisplayName("providing a partial match from the right")
        void partialMatchRight() {
            var text = new Text("al value");
            var match = analyzer.match(text, "my literal value");
            assertThat(match).returns("al value", whenAppliedTo("my literal value"));
        }


        @Test
        @DisplayName("providing a partial match in the middle")
        void partialMatchMid() {
            var text = new Text("eral val");
            var match = analyzer.match(text, "my literal value");
            assertThat(match).returns("eral val", whenAppliedTo("my literal value"));
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
            assertThat(match).returns("[my test value][my test value]", whenAppliedTo("my test value"));
        }
    }

    Function<Match, String> whenAppliedTo(String string) {
        return match -> apply(match, string);
    }

    String apply(Match match, String string) {
        assertThat(match).isInstanceOf(Match.FlexMatch.class);
        RangeFlex flex = ((Match.FlexMatch) match).flex();

        String prefix = string.substring(flex.start().from(), flex.start().exclusiveTo());
        prefix = (prefix.length() < 2) ? prefix : '[' + prefix + ']';

        String main = (flex.start().exclusiveTo() > flex.end().from()) ? "" : string.substring(flex.start().exclusiveTo(), flex.end().from());

        String suffix = string.substring(flex.end().from(), flex.end().exclusiveTo());
        suffix = (suffix.length() < 2) ? suffix : '[' + suffix + ']';

        return prefix + main + suffix;
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
//        @Test
//        @DisplayName("provides bindings for an identifier with a literal suffix")
//        void identifierWithLiteralSuffix() {
//            var optSuffix = new OptSuffix(new Identifier("var1"), new Text(". with suffix"));
//            var bindings = analyzer.detectBindings(optSuffix, "my test value. with suffix");
//            assertThat(bindings).containsExactly(
//                    new Binding("var1", "my test value")
//            );
//        }

        @Test
        @DisplayName("providing a match for a literal with a literal suffix")
        void matchLiteralWithLiteralSuffix() {
            var optSuffix = new OptSuffix(new Text("a literal"), new Text(". with a suffix"));
            var match = analyzer.match(optSuffix, "a literal. with a suffix");
            assertThat(match).returns("a literal. with a suffix", whenAppliedTo("a literal. with a suffix"));
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


        @Disabled
        @Test
        @DisplayName("providing a match for an identifier with a literal suffix")
        void matchIdentifierWithLiteralSuffix() {
            var optSuffix = new OptSuffix(new Identifier("var1"), new Text(". with suffix"));
            var match = analyzer.match(optSuffix, "my test value. with suffix");
            // TODO: add knowledge that left side cannot be empty for optsuffix
//            assertThat(match).as(match.toString()).returns("[my test value]. with suffix", whenAppliedTo("my test value. with suffix"));
            assertThat(match).as(match.toString()).returns("[my test valu]e. with suffix", whenAppliedTo("my test value. with suffix"));
        }

        @Disabled
        @Test
        @DisplayName("no match for an empty identifier with a literal suffix")
        void nomatchForEmptyIdentifierWithLiteralSuffix() {
            var optSuffix = new OptSuffix(new Identifier("var1"), new Text(". with suffix"));
            var match = analyzer.match(optSuffix, ". with suffix");
            // TODO: add knowledge that left side cannot be empty for optsuffix
            assertThat(match).as(match.toString()).returns(". with suffix", whenAppliedTo("my test value. with suffix"));
//            assertThat(match).as(match.toString()).returns("[my test valu]e. with suffix", whenAppliedTo("my test value. with suffix"));
        }

    }
}
