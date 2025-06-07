package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.Match.NoMatch;
import com.quaxantis.support.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.quaxantis.etui.template.parser.Constraint.*;

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
            log.debug("Match result for {}{}{}", expression.representationString(), System.lineSeparator(), match.multilineFormat());
        } else {
            log.trace("Match result for {}{}{}", expression.representationString(), System.lineSeparator(), match.multilineFormat());
        }
        return match;
    }

    Match doMatch(Expression expression, String string) {
        return switch (expression) {
            case Expression.Text text -> matchText(text, string);
            case Expression.Identifier identifier -> matchIdentifier(identifier, string);
            case Expression.Elvis elvis -> matchElvis(elvis, string);
            case Expression.OptPrefix optPrefix -> matchOptPrefix(optPrefix, string);
            case Expression.OptSuffix optSuffix -> matchOptSuffixAlternative(optSuffix, string);
            case Expression.Concat concat -> matchConcat(concat, string);
        };
    }

    private Match matchIdentifier(Expression.Identifier identifier, String resolvedExpression) {
        IntRange fullRange = IntRange.of(0, resolvedExpression.length());
        RangeFlex flex = RangeFlex.of(fullRange, fullRange);
        return Match.of(resolvedExpression)
                .constrain(toRange(flex).and(toMinLength(0)))
                .binding(identifier.name());
    }

    private Match matchText(Expression.Text textExpr, String string) {
        Objects.requireNonNull(textExpr);
        Objects.requireNonNull(string);
        String text = textExpr.literal();
        RangeFlex matchRange;
        int matchLength;

        if (string.equals(text)) {
            int end = string.length() - 1;
            matchRange = RangeFlex.ofFixed(0, end);
            matchLength = end + 1;
        } else {
            int index = string.indexOf(text);
            if (index < 0) {
                return new NoMatch(string, "literal expression not found: " + text);
            } else {
                int end = index + text.length() - 1;
                matchRange = RangeFlex.ofFixed(index, end);
                matchLength = end - index + 1;
            }
        }
        System.out.println(Match.of(string).constrain(toRange(matchRange).and(toFixedRange())));
        System.out.println(new Match.PartialMatch(new Match.RootMatch(string), matchRange));
        return Match.of(string).constrain(
                toRange(matchRange)
                        .and(toFixedRange())
                        .and(toMinLength(matchLength))
                        .and(toMaxLength(matchLength)));
    }

    private Match matchElvis(Expression.Elvis elvis, String string) {

        Match leftMatch = match(elvis.expression(), string);
        Match mainMatch = leftMatch.constrain(toMinLength(1));
        Match emptyMainMatch = leftMatch.constrain(toMaxLength(0));
        if (emptyMainMatch instanceof NoMatch) {
            return mainMatch;
        } else {
            Match fallbackMatch = match(elvis.orElse(), string);

            RangeFlex.Concat concatRange = RangeFlex.concat(emptyMainMatch.matchRange(), fallbackMatch.matchRange());
            Match constrainedEmptyMatch = emptyMainMatch.constrain(toRange(concatRange.left()));
            Match constrainedFallbackMatch = fallbackMatch.constrain(toRange(concatRange.right()));
            Match combined = constrainedFallbackMatch.andThen(fallback -> new Match.ConcatMatch(constrainedEmptyMatch, fallback, concatRange));
//            Match combined = fallbackMatch.andThen(fallback ->  new Match.CombinedMatch(string, emptyMainMatch, fallback, RangeFlex.concat(emptyMainMatch.matchRange(), fallback.matchRange())));

            return Match.ChoiceMatch.ofPossibleMatches(mainMatch, combined)
                    .orElseGet(() -> new NoMatch(string, null, mainMatch, fallbackMatch));
        }
    }

    private Match matchOptPrefix(Expression.OptPrefix optPrefix, String string) {
        Match leftMatch = doMatch(optPrefix.prefix(), string);
        Match rightMatch = doMatch(optPrefix.expression(), string);

        if (rightMatch instanceof NoMatch) {
            return new NoMatch(string, null, leftMatch, rightMatch);
        }

        Match mainMatch = rightMatch.constrain(toMinLength(1));
        Match emptyMainMatch = rightMatch.constrain(toMaxLength(0));
        Match concatMatch;

        if (mainMatch instanceof NoMatch || leftMatch instanceof NoMatch) {
            concatMatch = new NoMatch(string, null, mainMatch, leftMatch);
        } else {

            var leftFlex = leftMatch.matchRange();
            var rightFlex = mainMatch.matchRange();

            var concatResult = Result.ofTry(() -> RangeFlex.concatAlternative(leftFlex, rightFlex));
            concatMatch = switch (concatResult) {
                case Result.Success(var concat) -> new Match.CombinedMatch(string,
                                                                           leftMatch.constrain(toRange(concat.left())),
                                                                           mainMatch.constrain(toRange(concat.right())),
                                                                           concat.combined());
                case Result.Failure(var exc) -> new NoMatch(string,
                                                            exc + " at " + exc.getStackTrace()[0],
                                                            leftMatch,
                                                            rightMatch);
            };
        }

        return Match.ChoiceMatch.ofPossibleMatches(concatMatch, emptyMainMatch)
                .orElseGet(() -> new NoMatch(string, null, leftMatch, mainMatch));
    }

    private Match matchOptSuffixAlternative(Expression.OptSuffix optSuffix, String string) {
        Match leftMatch = doMatch(optSuffix.expression(), string);
        Match rightMatch = doMatch(optSuffix.suffix(), string);

        if (leftMatch instanceof NoMatch) {
            return new NoMatch(string, null, leftMatch, rightMatch);
        }

        Match mainMatch = leftMatch.constrain(toMinLength(1));
        Match emptymainMatch = leftMatch.constrain(toMaxLength(0));
        Match concatMatch;

        if (mainMatch instanceof NoMatch || rightMatch instanceof NoMatch) {
            concatMatch = new NoMatch(string, null, mainMatch, rightMatch);
        } else {

            var leftFlex = leftMatch.matchRange().constrain(toMinLength(1));
            var rightFlex = rightMatch.matchRange();

            var concatResult = Result.ofTry(() -> RangeFlex.concatAlternative(leftFlex, rightFlex));
            concatMatch = switch (concatResult) {
                case Result.Success(var concat) -> new Match.CombinedMatch(string,
                                                                           leftMatch.constrain(toRange(concat.left())),
                                                                           rightMatch.constrain(toRange(concat.right())),
                                                                           concat.combined());
                case Result.Failure(var exc) -> new NoMatch(string,
                                                            exc + " at " + exc.getStackTrace()[0],
                                                            leftMatch,
                                                            rightMatch);
            };
        }

        return Match.ChoiceMatch.ofPossibleMatches(concatMatch, emptymainMatch)
                .orElseGet(() -> new NoMatch(string, null, mainMatch, rightMatch));
    }
    private Match matchOptSuffix(Expression.OptSuffix optSuffix, String string) {
        Match leftMatch = doMatch(optSuffix.expression(), string);
        Match rightMatch = doMatch(optSuffix.suffix(), string);

        if (!(leftMatch instanceof NoMatch || rightMatch instanceof NoMatch)) {
            var leftFlex = leftMatch.matchRange().constrain(toMinLength(1));
            var rightFlex = rightMatch.matchRange();

            var concatResult = RangeFlex.tryConcat(leftFlex, rightFlex);
            if (concatResult instanceof Result.Success(RangeFlex.Concat concatRangeFlex)) {
                Match constrainedLeftMatch = leftMatch.constrain(toRange(concatRangeFlex.left()));
                Match constrainedRightMatch = rightMatch.constrain(toRange(concatRangeFlex.right()));
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

    private Match matchConcat(Expression.Concat concat, String string) {
        Iterator<Expression> iter = concat.parts().iterator();

        if (!iter.hasNext()) {
            return Match.of(string).constrain(toRange(RangeFlex.empty()).and(toMaxLength(0)));
        }

        Match result = match(iter.next(), string);

        while (iter.hasNext()) {
            Match nextMatch = match(iter.next(), string);
            var leftFlex = result.matchRange();
            var rightFlex = nextMatch.matchRange();
            var concatResult = Result.ofTry(() -> RangeFlex.concatAlternative(leftFlex, rightFlex));
            result = switch (concatResult) {
                case Result.Success(var concatRange) -> new Match.CombinedMatch(string,
                                                                                result.constrain(toRange(concatRange.left())),
                                                                                nextMatch.constrain(toRange(concatRange.right())),
                                                                                concatRange.combined());
                case Result.Failure(var exc) -> new NoMatch(string,
                                                            exc + " at " + exc.getStackTrace()[0],
                                                            result,
                                                            nextMatch);
            };
        }

        return result;
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
