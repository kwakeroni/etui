package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.Match.NoMatch;
import com.quaxantis.support.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.quaxantis.etui.template.parser.Constraint.*;
import static java.util.function.Predicate.not;

public class EELExpressionAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(EELExpressionAnalyzer.class);

    private static final List<Predicate<Match>> FILTERS = List.of(
            not((Match match) -> match instanceof NoMatch).and(match -> match.score() >= 0.4),
            not((Match match) -> match instanceof NoMatch).and(match -> match.score() >= 0.2),
            not((Match match) -> match instanceof NoMatch).and(match -> match.score() >= 0.1)
    );

    public Match match(Expression expression, String string, Map<String, String> bindings) {
        var fullExpr = new Expression.Delegate(expression);
        Collection<Match> matches = doMatch(string, bindings, fullExpr);

        Match match = (matches.size() == 0) ? new NoMatch(fullExpr, string, "No resulting matches")
                : (matches.size() == 1) ? matches.iterator().next()
                : Match.ChoiceMatch.ofPossibleMatches(fullExpr, matches.stream()).orElseGet(() -> new NoMatch(fullExpr, string, "No matches", matches));

        return match;
    }

    private Collection<Match> doMatch(String string, Map<String, String> bindings, Expression.Delegate fullExpr) {
        Collection<Match> matches = List.of();
        Match rootMatch = Match.of(fullExpr, string, bindings);

        // Try again with lesser strict filter if no results are found
        for (int i = 0; matches.isEmpty() && i < FILTERS.size(); i++) {
            matches = doMatch(fullExpr, string, rootMatch, FILTERS.get(i));
        }

        return matches;
    }


    Collection<Match> doMatch(Expression expression, String string, Match parent, Predicate<Match> matchFilter) {
        Collection<Match> result = switch (expression) {
            case Expression.Text text -> matchText(text, string, parent);
            case Expression.Identifier identifier -> matchIdentifier(identifier, string, parent);
            case Expression.Elvis elvis -> matchElvis(elvis, string, parent, matchFilter);
            case Expression.OptPrefix optPrefix -> matchOptPrefix(optPrefix, string, parent, matchFilter);
            case Expression.OptSuffix optSuffix -> matchOptSuffix(optSuffix, string, parent, matchFilter);
            case Expression.Concat concat -> matchConcat(concat, string, parent, matchFilter);
            case Expression.Delegate(var delegate) -> doMatch(delegate, string, parent, matchFilter);
        };

        return result.stream().filter(matchFilter).toList();
    }

    private Collection<Match> matchIdentifier(Expression.Identifier identifier, String fullString, Match parent) {
//        return parent.bindings()
//                .flatMap(binding -> matchIdentifier(identifier, fullString, parent, binding))
//                .toList();

        String variable = identifier.name();
        IntRange fullRange = IntRange.of(0, fullString.length());
        RangeFlex flex = RangeFlex.of(fullRange, fullRange);
        return
//                Stream.concat(
//                binding.valueOf(variable).map(value -> matchLiteral(value, identifier, fullString, parent)).orElseGet(Stream::empty),
                List.of(parent.constrain(toRange(flex).and(toMinLength(0)), identifier).binding(identifier, variable))
//        )
                ;
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
            return Stream.of(literalMatch(parent, matchRange, matchLength, expression));
        } else if (literal.isEmpty()) {
            return Stream.of(parent.constrain(Constraint.toMaxLength(0), expression));
//             Special case match empty literal only at the start
//            return Stream.of(literalMatch(parent, RangeFlex.empty(), 0));
        } else {
            List<Match> matches = new ArrayList<>();
            int index = -1;
            while (index < fullString.length() && (index = fullString.indexOf(literal, ++index)) >= 0) {
                int end = index + literal.length() - 1;
                RangeFlex matchRange = RangeFlex.ofFixed(index, end);
                int matchLength = end - index + 1;
                matches.add(literalMatch(parent, matchRange, matchLength, expression));
            }

            if (matches.isEmpty()) {
                return Stream.of(new NoMatch(expression, fullString, "literal expression not found: " + literal));
            } else {
                return matches.stream();
            }
        }
    }

    private static Match literalMatch(Match parent, RangeFlex matchRange, int matchLength, Expression expression) {
        return parent.constrain(
                toRange(matchRange)
                        .and(toFixedRange())
                        .and(toMinLength(matchLength))
                        .and(toMaxLength(matchLength)), expression);
    }

    private List<Match> matchElvis(Expression.Elvis elvis, String string, Match parent, Predicate<Match> matchFilter) {
        Collection<Match> leftMatches = doMatch(elvis.expression(), string, parent, matchFilter);
        return leftMatches.stream().flatMap(leftMatch -> elvisMatch(elvis, string, leftMatch, parent, matchFilter)).toList();
    }

    private Stream<Match> elvisMatch(Expression.Elvis elvis, String string, Match leftMatch, Match parent, Predicate<Match> matchFilter) {
        Match mainMatch = leftMatch.constrain(toMinLength(1), elvis);
        Match emptyMainMatch = leftMatch.constrain(toMaxLength(0), elvis);
        if (emptyMainMatch instanceof NoMatch) {
            return Stream.of(mainMatch);
        } else {
            Collection<Match> fallbackMatches = doMatch(elvis.orElse(), string, parent.bindingAll(elvis, emptyMainMatch.bindings().toList()), matchFilter);
            return Stream.concat(Stream.of(mainMatch),
                                 fallbackMatches.stream().map(fallbackMatch -> elvisMatchFallback(elvis, string, emptyMainMatch, fallbackMatch)));
        }
    }

    private static Match elvisMatchFallback(Expression.Elvis elvis, String string, Match emptyMainMatch, Match fallbackMatch) {
        RangeFlex.ConcatResult concatRange = RangeFlex.concat(emptyMainMatch.matchRange(), fallbackMatch.matchRange());
        Match constrainedEmptyMatch = emptyMainMatch.constrain(toRange(concatRange.left()), elvis);
        Match constrainedFallbackMatch = fallbackMatch.constrain(toRange(concatRange.right()), elvis);
        return constrainedFallbackMatch.andThen(cfm -> new Match.ConcatMatch(elvis, string, constrainedEmptyMatch, cfm, concatRange.combined()));
    }

    private List<Match> matchOptPrefix(Expression.OptPrefix optPrefix, String string, Match parent, Predicate<Match> matchFilter) {
        Collection<Match> exprMatches = doMatch(optPrefix.expression(), string, parent, matchFilter);
        return exprMatches.stream().flatMap(exprMatch -> optPrefixMatch(optPrefix, string, exprMatch, parent, matchFilter)).toList();
    }

    private List<Match> matchOptSuffix(Expression.OptSuffix optSuffix, String string, Match parent, Predicate<Match> matchFilter) {
        Collection<Match> exprMatches = doMatch(optSuffix.expression(), string, parent, matchFilter);
        return exprMatches.stream().flatMap(exprMatch -> optSuffixMatch(optSuffix, string, exprMatch, parent, matchFilter)).toList();
    }

    private Stream<Match> optPrefixMatch(Expression.OptPrefix optPrefix, String string, Match exprMatch, Match parent, Predicate<Match> matchFilter) {
        if (exprMatch instanceof NoMatch) {
            return Stream.of(new NoMatch(optPrefix, string, null, exprMatch));
        }

        Match mainMatch = exprMatch.constrain(toMinLength(1), optPrefix);
        Match emptyMainMatch = exprMatch.constrain(toMaxLength(0), optPrefix);
        Collection<Match> prefixMatches = doMatch(optPrefix.prefix(), string, parent.bindingAll(optPrefix, mainMatch.bindings().toList()), matchFilter);
        return Stream.concat(
                prefixMatches.stream().map(prefixMatch -> concatMatches(optPrefix, string, prefixMatch, mainMatch)),
                Stream.of(emptyMainMatch));
    }

    private Stream<Match> optSuffixMatch(Expression.OptSuffix optSuffix, String string, Match exprMatch, Match parent, Predicate<Match> matchFilter) {

        if (exprMatch instanceof NoMatch) {
            return Stream.of(new NoMatch(optSuffix, string, null, exprMatch));
        }

        Match mainMatch = exprMatch.constrain(toMinLength(1), optSuffix);
        Match emptymainMatch = exprMatch.constrain(toMaxLength(0), optSuffix);
        Collection<Match> suffixMatches = doMatch(optSuffix.suffix(), string, parent.bindingAll(optSuffix, mainMatch.bindings().toList()), matchFilter);
        return Stream.concat(
                suffixMatches.stream().map(suffixMatch -> concatMatches(optSuffix, string, mainMatch, suffixMatch)),
                Stream.of(emptymainMatch));
    }

    private Collection<Match> matchConcat(Expression.Concat concat, String string, Match parent, Predicate<Match> matchFilter) {
        Iterator<Expression> iter = concat.parts().iterator();

        if (!iter.hasNext()) {
            return List.of(parent.constrain(toRange(RangeFlex.empty()).and(toMaxLength(0)), concat));
        }

        Expression part = iter.next();
        Collection<Match> results = doMatch(part, string, parent, matchFilter);

        while (iter.hasNext()) {
            Expression nextPart = iter.next();
            Collection<Match> nextMatches = doMatch(nextPart, string, parent, matchFilter);
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
                    var constrainedLeftMatch = leftMatch.constrain(toRange(left), expression);
                    var constrainedRightMatch = rightMatch.constrain(toRange(right), expression);
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
