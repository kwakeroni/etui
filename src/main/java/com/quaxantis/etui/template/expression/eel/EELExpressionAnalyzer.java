package com.quaxantis.etui.template.expression.eel;

import com.quaxantis.etui.template.expression.Binding;
import com.quaxantis.etui.template.expression.matching.IntRange;
import com.quaxantis.etui.template.expression.matching.Match;
import com.quaxantis.etui.template.expression.matching.Match.NoMatch;
import com.quaxantis.etui.template.expression.matching.RangeFlex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.quaxantis.etui.template.expression.matching.Constraint.*;
import static java.util.function.Predicate.not;

class EELExpressionAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(EELExpressionAnalyzer.class);

    private static final List<Predicate<Match>> FILTERS = List.of(
            not((Match match) -> match instanceof NoMatch).and(match -> match.score() >= 0.4),
            not((Match match) -> match instanceof NoMatch).and(match -> match.score() >= 0.2),
            not((Match match) -> match instanceof NoMatch).and(match -> match.score() >= 0.1)
    );

    public Stream<Binding> findBindings(Expression expression, String string, Map<String, String> bindings) {
        return match(expression, string, bindings).flatMap(Match::bindings);
    }

    Stream<Match> match(Expression expression, String string, Map<String, String> bindings) {
        var fullExpr = new Expression.Delegate(expression);
        Collection<Match> matches = doMatch(fullExpr, string, Match.withGivenBindings(fullExpr, string, bindings));
        return matches.stream();
    }

    private Collection<Match> doMatch(Expression.Delegate fullExpr, String string, Match rootMatch) {
        Collection<Match> matches = List.of();

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
        String variable = identifier.name();
        IntRange fullRange = IntRange.of(0, fullString.length());
        RangeFlex flex = RangeFlex.of(fullRange, fullRange);
        return List.of(parent.constrain(toRange(flex).and(toMinLength(0)), identifier).binding(identifier, variable));
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
            return Stream.of(parent.constrain(toMaxLength(0), expression));
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
                return Stream.of(Match.noMatch("literal expression not found: " + literal, expression, fullString));
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
                                 fallbackMatches.stream().map(fallbackMatch -> emptyMainMatch.concatenate(fallbackMatch, elvis, string)));
        }
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
            return Stream.of(Match.noMatch(optPrefix, string, exprMatch));
        }

        Match mainMatch = exprMatch.constrain(toMinLength(1), optPrefix);
        Match emptyMainMatch = exprMatch.constrain(toMaxLength(0), optPrefix);
        Collection<Match> prefixMatches = doMatch(optPrefix.prefix(), string, parent.bindingAll(optPrefix, mainMatch.bindings().toList()), matchFilter);
        return Stream.concat(
                prefixMatches.stream().map(prefixMatch -> prefixMatch.concatenate(mainMatch, optPrefix, string)),
                Stream.of(emptyMainMatch));
    }

    private Stream<Match> optSuffixMatch(Expression.OptSuffix optSuffix, String string, Match exprMatch, Match parent, Predicate<Match> matchFilter) {

        if (exprMatch instanceof NoMatch) {
            return Stream.of(Match.noMatch(optSuffix, string, exprMatch));
        }

        Match mainMatch = exprMatch.constrain(toMinLength(1), optSuffix);
        Match emptymainMatch = exprMatch.constrain(toMaxLength(0), optSuffix);
        Collection<Match> suffixMatches = doMatch(optSuffix.suffix(), string, parent.bindingAll(optSuffix, mainMatch.bindings().toList()), matchFilter);
        return Stream.concat(
                suffixMatches.stream().map(suffixMatch -> mainMatch.concatenate(suffixMatch, optSuffix, string)),
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
        return nextMatches.stream().map(nextMatch -> match.concatenate(nextMatch, expression, string));
    }
}
