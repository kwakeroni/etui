package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.Match.NoMatch;
import com.quaxantis.etui.template.parser.Match.PartialMatch;
import com.quaxantis.support.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class EELExpressionAnalyzer {

    private static Logger log = LoggerFactory.getLogger(EELExpressionAnalyzer.class);

    public Bindings detectBindings(Expression expression, String resolvedExpression) {
        return switch (expression) {
            case Expression.Identifier identifier -> bindIdentifier(identifier, resolvedExpression);
            case Expression.Elvis elvis -> bindElvis(elvis, resolvedExpression);
            default -> Bindings.of();
        };
    }

    private Bindings bindIdentifier(Expression.Identifier identifier, String resolvedExpression) {
        return Bindings.of(new Binding(identifier.name(), resolvedExpression));
    }

    private Bindings bindElvis(Expression.Elvis elvis, String resolvedExpression) {
        Bindings bindLeft = detectBindings(elvis.expression(), resolvedExpression);
        Bindings bindEmptyLeft = detectBindings(elvis.expression(), "");
        Bindings bindOrElse = detectBindings(elvis.orElse(), resolvedExpression);
        Bindings bindRight = bindEmptyLeft.addSubBindings(bindOrElse);


        return bindLeft.concat(bindRight);
    }

    public Match match(Expression expression, String string) {
        Match match = doMatch(expression, string);
        if (match instanceof NoMatch) {
            log.debug("Match result for {}{}{}", expression.representationString(), System.lineSeparator(), match.multilineFormat("root"));
        } else {
            log.trace("Match result for {}{}{}", expression.representationString(), System.lineSeparator(), match.multilineFormat("root"));
        }
        return match;
    }

    Match doMatch(Expression expression, String string) {
        return switch (expression) {
            case Expression.Text text -> matchText(text, string);
            case Expression.Identifier identifier -> matchIdentifier(identifier, string);
//            case Expression.Elvis elvis -> bindElvis(elvis, resolvedExpression);
            case Expression.OptSuffix optSuffix -> matchOptSuffix(optSuffix, string);
            default -> new NoMatch(string, "unknown expression: " + expression);
        };
    }

    private Match matchIdentifier(Expression.Identifier identifier, String resolvedExpression) {
//        Match.RootMatch rootMatch = new Match.RootMatch(resolvedExpression);
        IntRange fullRange = IntRange.of(0, resolvedExpression.length());
        RangeFlex flex = RangeFlex.of(fullRange, fullRange).withMinLength(0);
        return new Match.BindingMatch(new Match.RootMatch(resolvedExpression).constrain(flex), identifier.name());
    }

    private Match matchText(Expression.Text textExpr, String string) {
        Objects.requireNonNull(textExpr);
        Objects.requireNonNull(string);
        String text = textExpr.literal();
        if (string.equals(text)) {
            int end = string.length() - 1;
            return new PartialMatch(new Match.RootMatch(string), RangeFlex.ofFixed(0, end).withMinLength(string.length()));
        } else {
            int index = string.indexOf(text);
            if (index < 0) {
                return new NoMatch(string, "literal expression not found: " + text);
            } else {
                int end = index + text.length() - 1;
                return new PartialMatch(new Match.RootMatch(string), RangeFlex.ofFixed(index, end).withMinLength(end - index + 1));
            }
        }
    }

    private Match matchOptSuffix(Expression.OptSuffix optSuffix, String string) {
        Match leftMatch = doMatch(optSuffix.expression(), string);
        Match rightMatch = doMatch(optSuffix.suffix(), string);

        if (!(leftMatch instanceof NoMatch || rightMatch instanceof NoMatch)) {
            var leftFlex = leftMatch.matchRange().withMinLength(1);
            var rightFlex = rightMatch.matchRange();

            var concatResult = RangeFlex.tryConcat(leftFlex, rightFlex);
            if (concatResult instanceof Result.Success(RangeFlex.Concat concatRangeFlex)) {
                Match constrainedLeftMatch = leftMatch.constrain(concatRangeFlex.left());
                Match constrainedRightMatch = rightMatch.constrain(concatRangeFlex.right());
                var concatMatch = new Match.ConcatMatch(constrainedLeftMatch, constrainedRightMatch, concatRangeFlex);
                log.trace("""
                          Matching {}
                          with left ={}
                          with right={}
                          with match={}
                          """, optSuffix, constrainedLeftMatch, constrainedRightMatch, concatMatch);
                return concatMatch;
            } else if (concatResult instanceof Result.Failure(RuntimeException exception)) {
                log.trace("""
                          Matching {}
                          with left ={}
                          with right={}
                          with no match because {}
                          """, this, leftMatch, rightMatch, exception.toString() + " at " + exception.getStackTrace()[0]);
                return new NoMatch(string, exception + " at " + exception.getStackTrace()[0], leftMatch, rightMatch);
            }
//        }
        }
        log.trace("""
                  Matching {}
                  with left ={}
                  with right={}
                  with no match
                  """, this, leftMatch, rightMatch);
        return new NoMatch(string, null, leftMatch, rightMatch);
    }

    public record Binding(
            String variable,
            String value,
            Bindings subBindings
    ) {

        public Binding {
            Objects.requireNonNull(variable, "variable");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(subBindings, "subBindings");
        }

        public Binding(String variable, String value) {
            this(variable, value, Bindings.of());
        }

        public Binding addSubBindings(Bindings bindings) {
            return new Binding(variable, value, subBindings.concat(bindings));
        }
    }

    public static class Bindings implements Iterable<Binding> {
        private static final Bindings EMPTY = new Bindings(List.of());
        private final List<Binding> bindings;

        public static Bindings of() {
            return EMPTY;
        }

        public static Bindings of(Binding binding) {
            Objects.requireNonNull(binding, "binding");
            return new Bindings(List.of(binding));
        }

        private Bindings(List<Binding> bindings) {
            this.bindings = bindings;
        }

        public boolean isEmpty() {
            return this.bindings.isEmpty();
        }

        @Override
        public Iterator<Binding> iterator() {
            return this.bindings.iterator();
        }

        public Stream<Binding> stream() {
            return this.bindings.stream();
        }

        public Bindings addSubBindings(Bindings subBindings) {
            return new Bindings(
                    this.bindings.stream()
                            .map(binding -> binding.addSubBindings(subBindings))
                            .toList());
        }

        public Bindings concat(Bindings addedBindings) {
            return new Bindings(Stream.concat(this.stream(), addedBindings.stream()).toList());
        }

        @Override
        public String toString() {
            return "Bindings" + bindings.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Bindings bindings1 = (Bindings) o;
            return Objects.equals(bindings, bindings1.bindings);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(bindings);
        }
    }

}
