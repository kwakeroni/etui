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

    public Match match(Expression expression, String string, Map<String, String> bindings) {
        Collection<Match> matches = doMatch(expression, string, Match.of(expression, string, bindings));
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

    Collection<Match> doMatch(Expression expression, String string, Match parent) {
        return switch (expression) {
            case Expression.Text text -> matchText(text, string, parent);
            case Expression.Identifier identifier -> matchIdentifier(identifier, string, parent);
            case Expression.Elvis elvis -> matchElvis(elvis, string, parent);
            case Expression.OptPrefix optPrefix -> matchOptPrefix(optPrefix, string, parent);
            case Expression.OptSuffix optSuffix -> matchOptSuffix(optSuffix, string, parent);
            case Expression.Concat concat -> matchConcat(concat, string, parent);
        };
    }

    private Collection<Match> matchIdentifier(Expression.Identifier identifier, String fullString, Match parent) {
        return parent.bindings()
                .flatMap(binding -> matchIdentifier(identifier, fullString, parent, binding))
                .toList();
    }

    private Stream<Match> matchIdentifier(Expression.Identifier identifier, String fullString, Match parent, Binding binding) {
        String variable = identifier.name();
        return binding.valueOf(variable)
                .map(value -> matchLiteral(value, identifier, fullString, parent))
                .orElseGet(() -> {
                    IntRange fullRange = IntRange.of(0, fullString.length());
                    RangeFlex flex = RangeFlex.of(fullRange, fullRange);
                    Match match = parent
                            .constrain(toRange(flex).and(toMinLength(0)))
                            .binding(identifier, variable);
                    return Stream.of(match);
                });
    }

    private Collection<Match> matchText(Expression.Text textExpr, String fullString, Match parent) {
        Objects.requireNonNull(textExpr);
        Objects.requireNonNull(fullString);
        String text = textExpr.literal();

        return matchLiteral(text, textExpr, fullString, parent).toList();
    }

    private static Stream<Match> matchLiteral(String literal, Expression expression, String fullString, Match parent) {
        if (fullString.equals(literal)) {
            int end = fullString.length() - 1;
            RangeFlex matchRange = RangeFlex.ofFixed(0, end);
            int matchLength = end + 1;
            return Stream.of(literalMatch(parent, matchRange, matchLength));
        } else if (literal.isEmpty()) {
            // Special case match empty literal only at the start
            return Stream.of(literalMatch(parent, RangeFlex.empty(), 0));
        } else {
            List<Match> matches = new ArrayList<>();
            int index = -1;
            while (index < fullString.length() && (index = fullString.indexOf(literal, ++index)) >= 0) {
                int end = index + literal.length() - 1;
                RangeFlex matchRange = RangeFlex.ofFixed(index, end);
                int matchLength = end - index + 1;
                matches.add(literalMatch(parent, matchRange, matchLength));
            }

            if (matches.isEmpty()) {
                return Stream.of(new NoMatch(expression, fullString, "literal expression not found: " + literal));
            } else {
                return matches.stream();
            }
        }
    }

    private static Match literalMatch(Match parent, RangeFlex matchRange, int matchLength) {
        return parent.constrain(
                toRange(matchRange)
                        .and(toFixedRange())
                        .and(toMinLength(matchLength))
                        .and(toMaxLength(matchLength)));
    }

    private List<Match> matchElvis(Expression.Elvis elvis, String string, Match parent) {
        Collection<Match> leftMatches = doMatch(elvis.expression(), string, parent);
        return leftMatches.stream().flatMap(leftMatch -> elvisMatch(elvis, string, leftMatch, parent)).toList();
    }

    private Stream<Match> elvisMatch(Expression.Elvis elvis, String string, Match leftMatch, Match parent) {
        Match mainMatch = leftMatch.constrain(toMinLength(1));
        Match emptyMainMatch = leftMatch.constrain(toMaxLength(0));
        if (emptyMainMatch instanceof NoMatch) {
            return Stream.of(mainMatch);
        } else {
            Collection<Match> fallbackMatches = doMatch(elvis.orElse(), string, parent.bindingAll(parent.expression(), emptyMainMatch.bindings().toList()));
            return Stream.concat(Stream.of(mainMatch),
                                 fallbackMatches.stream().map(fallbackMatch -> elvisMatchFallback(elvis, string, emptyMainMatch, fallbackMatch)));
        }
    }

    private static Match elvisMatchFallback(Expression.Elvis elvis, String string, Match emptyMainMatch, Match fallbackMatch) {
        RangeFlex.ConcatResult concatRange = RangeFlex.concat(emptyMainMatch.matchRange(), fallbackMatch.matchRange());
        Match constrainedEmptyMatch = emptyMainMatch.constrain(toRange(concatRange.left()));
        Match constrainedFallbackMatch = fallbackMatch.constrain(toRange(concatRange.right()));
        return constrainedFallbackMatch.andThen(cfm -> new Match.ConcatMatch(elvis, string, constrainedEmptyMatch, cfm, concatRange.combined()));
    }

    private List<Match> matchOptPrefix(Expression.OptPrefix optPrefix, String string, Match parent) {
        Collection<Match> exprMatches = doMatch(optPrefix.expression(), string, parent);
        return exprMatches.stream().flatMap(exprMatch -> optPrefixMatch(optPrefix, string, exprMatch, parent)).toList();
    }

    private List<Match> matchOptSuffix(Expression.OptSuffix optSuffix, String string, Match parent) {
        Collection<Match> exprMatches = doMatch(optSuffix.expression(), string, parent);
        return exprMatches.stream().flatMap(exprMatch -> optSuffixMatch(optSuffix, string, exprMatch, parent)).toList();
    }

    private Stream<Match> optPrefixMatch(Expression.OptPrefix optPrefix, String string, Match exprMatch, Match parent) {
        if (exprMatch instanceof NoMatch) {
            return Stream.of(new NoMatch(optPrefix, string, null, exprMatch));
        }

        Match mainMatch = exprMatch.constrain(toMinLength(1));
        Match emptyMainMatch = exprMatch.constrain(toMaxLength(0));
        Collection<Match> prefixMatches = doMatch(optPrefix.prefix(), string, parent.bindingAll(parent.expression(), mainMatch.bindings().toList()));
        return Stream.concat(
                prefixMatches.stream().map(prefixMatch -> concatMatches(optPrefix, string, prefixMatch, mainMatch)),
                Stream.of(emptyMainMatch));
    }

    private Stream<Match> optSuffixMatch(Expression.OptSuffix optSuffix, String string, Match exprMatch, Match parent) {

        if (exprMatch instanceof NoMatch) {
            return Stream.of(new NoMatch(optSuffix, string, null, exprMatch));
        }

        Match mainMatch = exprMatch.constrain(toMinLength(1));
        Match emptymainMatch = exprMatch.constrain(toMaxLength(0));
        Collection<Match> suffixMatches = doMatch(optSuffix.suffix(), string, parent.bindingAll(parent.expression(), mainMatch.bindings().toList()));
        return Stream.concat(
                suffixMatches.stream().map(suffixMatch -> concatMatches(optSuffix, string, mainMatch, suffixMatch)),
                Stream.of(emptymainMatch));
    }

    private Collection<Match> matchConcat(Expression.Concat concat, String string, Match parent) {
        Iterator<Expression> iter = concat.parts().iterator();

        if (!iter.hasNext()) {
            return List.of(parent.constrain(toRange(RangeFlex.empty()).and(toMaxLength(0))));
        }

        Expression part = iter.next();
        Collection<Match> results = doMatch(part, string, parent);

        while (iter.hasNext()) {
            Expression nextPart = iter.next();
            Collection<Match> nextMatches = doMatch(nextPart, string, parent);
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
