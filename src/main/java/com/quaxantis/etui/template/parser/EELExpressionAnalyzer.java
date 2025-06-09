package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.Match.NoMatch;
import com.quaxantis.support.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.quaxantis.etui.template.parser.Constraint.*;

public class EELExpressionAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(EELExpressionAnalyzer.class);

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
            case Expression.OptSuffix optSuffix -> matchOptSuffix(optSuffix, string);
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

        if (string.equals(text)) {
            int end = string.length() - 1;
            RangeFlex matchRange = RangeFlex.ofFixed(0, end);
            int matchLength = end + 1;
            return literalMatch(string, matchRange, matchLength);
        } else if (text.isEmpty()) {
            // Special case match empty literal only at the start
            return literalMatch(string, RangeFlex.empty(), 0);
        } else {
            List<Match> matches = new ArrayList<>();
            int index = -1;
            while (index < string.length() && (index = string.indexOf(text, ++index)) >= 0) {
                int end = index + text.length() - 1;
                RangeFlex matchRange = RangeFlex.ofFixed(index, end);
                int matchLength = end - index + 1;
                matches.add(literalMatch(string, matchRange, matchLength));
                System.out.println(index);
            }

            return Match.ChoiceMatch.ofPossibleMatches(matches.stream())
                    .orElseGet(() -> new NoMatch(string, "literal expression not found: " + text));
        }
    }

    private static Match literalMatch(String string, RangeFlex matchRange, int matchLength) {
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

            RangeFlex.ConcatResult concatRange = RangeFlex.concat(emptyMainMatch.matchRange(), fallbackMatch.matchRange());
            Match constrainedEmptyMatch = emptyMainMatch.constrain(toRange(concatRange.left()));
            Match constrainedFallbackMatch = fallbackMatch.constrain(toRange(concatRange.right()));
            Match combined = constrainedFallbackMatch.andThen(fallback -> new Match.ConcatMatch(string, constrainedEmptyMatch, fallback, concatRange.combined()));

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

            concatMatch = switch (RangeFlex.tryConcat(leftFlex, rightFlex)) {
                case Result.Success(RangeFlex.ConcatResult(var left, var right, var combined)) ->
                        new Match.ConcatMatch(string,
                                              leftMatch.constrain(toRange(left)),
                                              mainMatch.constrain(toRange(right)),
                                              combined);
                case Result.Failure(var exc) ->
                        new NoMatch(string, exc + " at " + exc.getStackTrace()[0], leftMatch, rightMatch);
            };
        }

        return Match.ChoiceMatch.ofPossibleMatches(concatMatch, emptyMainMatch)
                .orElseGet(() -> new NoMatch(string, null, leftMatch, mainMatch));
    }

    private Match matchOptSuffix(Expression.OptSuffix optSuffix, String string) {
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

            concatMatch = switch (RangeFlex.tryConcat(leftFlex, rightFlex)) {
                case Result.Success(RangeFlex.ConcatResult(var left, var right, var combined)) ->
                        new Match.ConcatMatch(string,
                                              leftMatch.constrain(toRange(left)),
                                              rightMatch.constrain(toRange(right)),
                                              combined);
                case Result.Failure(var exc) ->
                        new NoMatch(string, exc + " at " + exc.getStackTrace()[0], leftMatch, rightMatch);
            };
        }

        return Match.ChoiceMatch.ofPossibleMatches(concatMatch, emptymainMatch)
                .orElseGet(() -> new NoMatch(string, null, mainMatch, rightMatch));
    }

    private Match matchConcat(Expression.Concat concat, String string) {
        Iterator<Expression> iter = concat.parts().iterator();

        if (!iter.hasNext()) {
            return Match.of(string).constrain(toRange(RangeFlex.empty()).and(toMaxLength(0)));
        }

        Match result = match(iter.next(), string);

        while (iter.hasNext()) {
            Match nextMatch = match(iter.next(), string);
            if (nextMatch instanceof NoMatch) {
                return new NoMatch(string, null, result, nextMatch);
            }
            var leftFlex = result.matchRange();
            var rightFlex = nextMatch.matchRange();
            result = switch (RangeFlex.tryConcat(leftFlex, rightFlex)) {
                case Result.Success(RangeFlex.ConcatResult(var left, var right, var combined)) -> {
                    var constrainedLeftMatch = result.constrain(toRange(left));
                    var constrainedRightMatch = nextMatch.constrain(toRange(right));
                    if (constrainedLeftMatch instanceof NoMatch || constrainedRightMatch instanceof NoMatch) {
                        yield new NoMatch(string, null, constrainedLeftMatch, constrainedRightMatch);
                    }
                    yield new Match.ConcatMatch(string, constrainedLeftMatch, constrainedRightMatch, combined);
                }
                case Result.Failure(var exc) ->
                        new NoMatch(string, exc + " at " + exc.getStackTrace()[0], result, nextMatch);
            };
        }

        return result;
    }
}
