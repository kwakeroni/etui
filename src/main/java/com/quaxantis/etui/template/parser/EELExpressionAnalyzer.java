package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.Match.NoMatch;
import com.quaxantis.support.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

import static com.quaxantis.etui.template.parser.Constraint.*;

public class EELExpressionAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(EELExpressionAnalyzer.class);

    public Match match(Expression expression, String string) {
        Collection<Match> matches = doMatch(expression, string);
        Match match = (matches.size() == 0) ? new NoMatch(expression, string, "No resulting matches")
                : (matches.size() == 1) ? matches.iterator().next()
                : Match.ChoiceMatch.ofPossibleMatches(expression, matches.stream()).orElseGet(() -> new NoMatch(expression, string, "No matches", matches));

        if (match instanceof NoMatch) {
            log.debug("Match result for {}{}{}", expression.representationString(), System.lineSeparator(), match.multilineFormat());
        } else {
            log.trace("Match result for {}{}{}", expression.representationString(), System.lineSeparator(), match.multilineFormat());
        }
        return match;
    }

    Collection<Match> doMatch(Expression expression, String string) {
        return switch (expression) {
            case Expression.Text text -> matchText(text, string);
            case Expression.Identifier identifier -> matchIdentifier(identifier, string);
            case Expression.Elvis elvis -> matchElvis(elvis, string);
            case Expression.OptPrefix optPrefix -> matchOptPrefix(optPrefix, string);
            case Expression.OptSuffix optSuffix -> matchOptSuffix(optSuffix, string);
            case Expression.Concat concat -> matchConcat(concat, string);
        };
    }

    private Collection<Match> matchIdentifier(Expression.Identifier identifier, String resolvedExpression) {
        IntRange fullRange = IntRange.of(0, resolvedExpression.length());
        RangeFlex flex = RangeFlex.of(fullRange, fullRange);
        Match match = Match.of(identifier, resolvedExpression)
                .constrain(toRange(flex).and(toMinLength(0)))
                .binding(identifier, identifier.name());
        return List.of(match);
    }

    private Collection<Match> matchText(Expression.Text textExpr, String string) {
        Objects.requireNonNull(textExpr);
        Objects.requireNonNull(string);
        String text = textExpr.literal();

        if (string.equals(text)) {
            int end = string.length() - 1;
            RangeFlex matchRange = RangeFlex.ofFixed(0, end);
            int matchLength = end + 1;
            return List.of(literalMatch(textExpr, string, matchRange, matchLength));
        } else if (text.isEmpty()) {
            // Special case match empty literal only at the start
            return List.of(literalMatch(textExpr, string, RangeFlex.empty(), 0));
        } else {
            List<Match> matches = new ArrayList<>();
            int index = -1;
            while (index < string.length() && (index = string.indexOf(text, ++index)) >= 0) {
                int end = index + text.length() - 1;
                RangeFlex matchRange = RangeFlex.ofFixed(index, end);
                int matchLength = end - index + 1;
                matches.add(literalMatch(textExpr, string, matchRange, matchLength));
            }

            if (matches.isEmpty()) {
                return List.of(new NoMatch(textExpr, string, "literal expression not found: " + text));
            } else {
                return Collections.unmodifiableCollection(matches);
            }
        }
    }

    private static Match literalMatch(Expression expression, String string, RangeFlex matchRange, int matchLength) {
        return Match.of(expression, string).constrain(
                toRange(matchRange)
                        .and(toFixedRange())
                        .and(toMinLength(matchLength))
                        .and(toMaxLength(matchLength)));
    }

    private List<Match> matchElvis(Expression.Elvis elvis, String string) {
        Collection<Match> leftMatches = doMatch(elvis.expression(), string);
        Collection<Match> fallbackMatches = doMatch(elvis.orElse(), string);
        return leftMatches.stream().flatMap(leftMatch -> elvisMatch(elvis, string, leftMatch, fallbackMatches)).toList();
    }

    private Stream<Match> elvisMatch(Expression.Elvis elvis, String string, Match leftMatch, Collection<Match> fallbackMatches) {
        Match mainMatch = leftMatch.constrain(toMinLength(1));
        Match emptyMainMatch = leftMatch.constrain(toMaxLength(0));
        if (emptyMainMatch instanceof NoMatch) {
            return Stream.of(mainMatch);
        } else {
            return Stream.concat(Stream.of(mainMatch),
                                 fallbackMatches.stream().map(fallbackMatch -> elvisMatch(elvis, string, emptyMainMatch, fallbackMatch)));
        }
    }

    private static Match elvisMatch(Expression.Elvis elvis, String string, Match emptyMainMatch, Match fallbackMatch) {
        RangeFlex.ConcatResult concatRange = RangeFlex.concat(emptyMainMatch.matchRange(), fallbackMatch.matchRange());
        Match constrainedEmptyMatch = emptyMainMatch.constrain(toRange(concatRange.left()));
        Match constrainedFallbackMatch = fallbackMatch.constrain(toRange(concatRange.right()));
        return constrainedFallbackMatch.andThen(fallback -> new Match.ConcatMatch(elvis, string, constrainedEmptyMatch, fallback, concatRange.combined()));
    }

    private List<Match> matchOptPrefix(Expression.OptPrefix optPrefix, String string) {
        Collection<Match> prefixMatches = doMatch(optPrefix.prefix(), string);
        Collection<Match> exprMatches = doMatch(optPrefix.expression(), string);
        return exprMatches.stream().flatMap(exprMatch -> optPrefixMatch(optPrefix, string, exprMatch, prefixMatches)).toList();
    }

    private List<Match> matchOptSuffix(Expression.OptSuffix optSuffix, String string) {
        Collection<Match> exprMatches = doMatch(optSuffix.expression(), string);
        Collection<Match> suffixMatches = doMatch(optSuffix.suffix(), string);
        return exprMatches.stream().flatMap(exprMatch -> optSuffixMatch(optSuffix, string, exprMatch, suffixMatches)).toList();
    }

    private Stream<Match> optPrefixMatch(Expression.OptPrefix optPrefix, String string, Match exprMatch, Collection<Match> prefixMatches) {
        if (exprMatch instanceof NoMatch) {
            return Stream.of(new NoMatch(optPrefix, string, null, exprMatch));
        }

        Match mainMatch = exprMatch.constrain(toMinLength(1));
        Match emptyMainMatch = exprMatch.constrain(toMaxLength(0));
        return Stream.concat(
                prefixMatches.stream().map(prefixMatch -> concatMatches(optPrefix, string, prefixMatch, mainMatch)),
                Stream.of(emptyMainMatch));
    }

    private Stream<Match> optSuffixMatch(Expression.OptSuffix optSuffix, String string, Match exprMatch, Collection<Match> suffixMatches) {

        if (exprMatch instanceof NoMatch) {
            return Stream.of(new NoMatch(optSuffix, string, null, exprMatch));
        }

        Match mainMatch = exprMatch.constrain(toMinLength(1));
        Match emptymainMatch = exprMatch.constrain(toMaxLength(0));
        return Stream.concat(
                suffixMatches.stream().map(suffixMatch -> concatMatches(optSuffix, string, mainMatch, suffixMatch)),
                Stream.of(emptymainMatch));
    }

    private Collection<Match> matchConcat(Expression.Concat concat, String string) {
        Iterator<Expression> iter = concat.parts().iterator();

        if (!iter.hasNext()) {
            return List.of(Match.of(concat, string).constrain(toRange(RangeFlex.empty()).and(toMaxLength(0))));
        }

        Expression part = iter.next();
        Collection<Match> results = doMatch(part, string);

        while (iter.hasNext()) {
            Expression nextPart = iter.next();
            Collection<Match> nextMatches = doMatch(nextPart, string);
            part = new Expression.Concat(part, nextPart);
            final var expression = part;
            results = results.stream().flatMap(match -> concatMatch(expression, string, match, nextMatches)).toList();
        }

        return results;
    }

    private Stream<Match> concatMatch(Expression expression, String string, Match match, Collection<Match> nextMatches) {
        return nextMatches.stream().map(nextMatch -> concatMatches(expression, string, match, nextMatch));
    }

    private static Match concatMatches(Expression expression, String string, Match leftMatch, Match rightMatch) {
        if (leftMatch instanceof NoMatch || rightMatch instanceof NoMatch) {
            return new NoMatch(expression, string, null, leftMatch, rightMatch);
        } else {

            var leftFlex = leftMatch.matchRange();
            var rightFlex = rightMatch.matchRange();

            return switch (RangeFlex.tryConcat(leftFlex, rightFlex)) {
                case Result.Success(RangeFlex.ConcatResult(var left, var right, var combined)) -> {
                    var constrainedLeftMatch = leftMatch.constrain(toRange(left));
                    var constrainedRightMatch = rightMatch.constrain(toRange(right));
                    if (constrainedLeftMatch instanceof NoMatch || constrainedRightMatch instanceof NoMatch) {
                        yield new NoMatch(expression, string, null, constrainedLeftMatch, constrainedRightMatch);
                    }
                    yield new Match.ConcatMatch(expression, string, constrainedLeftMatch, constrainedRightMatch, combined);
                }
                case Result.Failure(var exc) ->
                        new NoMatch(expression, string, exc + " at " + exc.getStackTrace()[0], leftMatch, rightMatch);
            };
        }
    }
}
