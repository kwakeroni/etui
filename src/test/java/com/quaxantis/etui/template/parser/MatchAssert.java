package com.quaxantis.etui.template.parser;

import com.quaxantis.support.ide.API;
import com.quaxantis.support.util.ANSI;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowingConsumer;
import org.assertj.core.presentation.StandardRepresentation;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;

@SuppressWarnings("UnusedReturnValue")
public class MatchAssert<SELF extends MatchAssert<SELF>> extends AbstractObjectAssert<SELF, Match> {

    @API
    public static MatchAssert assertThat(Match match) {
        return new MatchAssert(match);
    }

    public MatchAssert(Match match) {
        this(match, MatchAssert.class);
    }

    protected MatchAssert(Match match, Class<?> selfType) {
        super(match, selfType);
        StandardRepresentation.registerFormatterForType(FormattedMatch.class, FormattedMatch::formatted);
    }

    @API
    public SELF isMatch() {
        if (actual instanceof Match.NoMatch noMatch) {
            failWithActualAndMessage("Expected actual to be a match, but it was not");
        }
        return myself;
    }

    @API
    public SELF isNoMatch() {
        return isNoMatchSatisfying(_ -> {
        });
    }

    @API
    public SELF isNoMatchSatisfying(Consumer<Match.NoMatch> consumer) {
        if (actual instanceof Match.NoMatch noMatch) {
            consumer.accept(noMatch);
        } else {
            failWithActualExpectedAndMessage(actual, new Match.NoMatch(actual.expression(), actual.fullString(), null), "Expected actual not to be a match, but it was");
        }
        return myself;

    }

    @API
    public SELF isNotFullMatch() {
        if (actual.isFullMatch()) {
            failWithActualAndMessage("Expected actual not to be a full match, but it was");
        }
        return myself;
    }

    @API
    public SELF isFullMatch() {
        if (!actual.isFullMatch()) {
            failWithActualExpectedAndMessage(actual, new Match.RootMatch(actual.expression(), actual.fullString(), Map.of()), "Expected actual to be a full match");
        }
        return myself;
    }

    @API
    public SELF hasBoundVariables() {
        if (!actual.hasBoundVariables()) {
            failWithActualAndMessage("Expected actual to have bound variables, but it has not");
        }
        return myself;
    }

    @API
    public SELF hasNoBoundVariables() {
        if (actual.hasBoundVariables()) {
            failWithActualAndMessage("Expected actual not to have bound variables, but it has");
        }
        return myself;
    }

    @API
    public SELF hasMatchRepresentation(String expected) {
        String fullRepresentation = actual.matchRepresentation().replaceFirst("!\\d*<\\d*$", "");
        if (!expected.equals(fullRepresentation)) {
            failWithDerivedActualAndMessage(_ -> fullRepresentation, actual, expected, "Expected actual representation to be equal to");
        }
        return myself;
    }

    @API
    public SELF hasMatchRepresentation(String expected, String expectedLength) {
        String fullRepresentation = actual.matchRepresentation();
        String expectedRepresentationWithLength = expected + "!" + expectedLength;
        if (!expectedRepresentationWithLength.equals(fullRepresentation)) {
            failWithDerivedActualAndMessage(_ -> fullRepresentation, actual, expected, "Expected actual representation to be equal to");
        }
        return myself;
    }

    @API
    @SafeVarargs
    public final SELF hasBindings(Map<String, String>... bindings) {
        return hasBindings(_ -> true, bindings);
    }

    @API
    @SafeVarargs
    public final SELF hasBindings(double filterByMinimalScore, Map<String, String>... bindings) {
        return hasBindings(b -> b.score() >= filterByMinimalScore, bindings);
    }

    @SafeVarargs
    private SELF hasBindings(Predicate<Binding> predicate, Map<String, String>... bindings) {
        Assertions.assertThat(actual.bindings())
                .filteredOn(predicate)
                .extracting(Binding::asMap)
                .containsExactlyInAnyOrder(bindings);
        return myself;
    }

    @API
    public SELF hasOnlyBindingsMatching(Expression expression) {
        return hasOnlyBindingsMatching(_ -> true, expression);
    }

    @API
    public SELF hasOnlyBindingsMatching(double filterByMinimalScore, Expression expression) {
        return hasOnlyBindingsMatching(b -> b.score() >= filterByMinimalScore, expression);
    }

    private SELF hasOnlyBindingsMatching(Predicate<Binding> predicate, Expression expression) {
        Assertions.assertThat(
                        actual.bindings()
                                .filter(predicate)
                                .map(binding -> new ExpressionResolver(var -> binding.valueOf(var).orElse("")))
                                .map(resolver -> resolver.resolve(expression))
                                .distinct())
                .containsExactlyInAnyOrder(actual.fullString());
        return myself;
    }

    @API
    public SELF hasOnlyBindingsMatchingOrEmpty(Expression expression) {
        Assertions.assertThat(
                        actual.bindings()
                                .map(binding -> new ExpressionResolver(var -> binding.valueOf(var).orElse("")))
                                .map(resolver -> resolver.resolve(expression))
                                .distinct())
                .containsAnyOf(actual.fullString(), "");
        return myself;
    }

    @API
    public SELF hasOnlyBindingsPartiallyMatching(Expression expression) {
        Assertions.assertThat(
                        actual.bindings()
                                .map(binding -> new ExpressionResolver(var -> binding.valueOf(var).orElse("")))
                                .map(resolver -> resolver.resolve(expression))
                                .distinct())
                .allMatch(matchString -> actual.fullString().contains(matchString), "is part of \"" + actual.fullString() + '"');
        return myself;
    }

    public SELF debugBindings(Expression expression) {
        GenericTreeModel.build()
                .with(Match.class, m -> toHtml("%s [%s] %s".formatted(m.simpleFormat(), m, m.expression().emphasisString())), Match::parentMatches)
                .with(Binding.class, MatchAssert::toNodeString, MatchAssert::toNodeList)
                .withToString(RangeFlex.Applied.class, range -> toHtml(range.format()))
                .with(Binding.VariableInfo.class, MatchAssert::toNodeString, Binding.VariableInfo::sources)
                .show(Map.entry("Bindings", actual.bindings().sorted(comparing(Binding::score).reversed()).toList()), actual);
        return myself;
    }

    private static String toNodeString(Binding binding) {
        String matchString = binding.match().appliedRange().format(RangeFlexFormatter.UNDERLINE_ONLY);
        return toHtml(matchString + " (" + ANSI.BOLD + binding.score() + ANSI.NOT_BOLD + " " + binding.scoreObject().reason() + ") variables = " + binding.boundVariables().toList());
    }

    private static List<Binding> toNodeList(Binding binding) {
        return binding.parentBindings().filter(not(Binding.Empty.class::isInstance)).toList();
//        return binding.boundVariables().map(var -> binding.infoOf(var).orElse(new Binding.VariableInfo(var, binding, null, null, List.of()))).toList();
    }

    private static String toNodeString(Binding.VariableInfo info) {
        return toHtml("%s=%s %s %s %s  - %s".formatted(
                info.name(),
                ((info.range() instanceof RangeFlex.Applied range) ? '"' + range.extractMax() + '"' : "null"),
                ANSI.BOLD + ((info.score() instanceof Binding.Score score) ? score.value() : "?") + ANSI.NOT_BOLD,
                ANSI.ITALIC + ((info.score() instanceof Binding.Score score) ? score.reason() : "") + ANSI.NOT_ITALIC,
                ((info.range() instanceof RangeFlex.Applied range) ? range.format() + range.range().lengthString() : "?"),
                info.binding().match()
        ));
    }

    static int i = 0;

    private static String toHtml(String string) {
        String ansiHtml = ANSI.toHTML(string);
        if (i++ < 100)
            System.out.println(string + " = " + ansiHtml);
        return "<html>" + ansiHtml + "</html>";
    }

    @API
    public SELF isChoiceOfSatisfying(Consumer<List<Match>> consumer) {
        if (!(actual instanceof Match.ChoiceMatch(var expression, var choices))) {
            failWithActualAndMessage("Expected to be an instance of Match.ChoiceMatch, but was not");
        } else {
            consumer.accept(choices);
        }
        return myself;
    }

    @API
    @SafeVarargs
    public final SELF hasChoicesSatisfying(ThrowingConsumer<? super Match>... requirements) {
        return isChoiceOfSatisfying(list -> Assertions.assertThat(list).satisfiesExactlyInAnyOrder(requirements));
    }

    protected void failWithActualAndMessage(String errorMessageFormat, Object... arguments) {
        failWithMessage(appendActualMatch(errorMessageFormat, actual), arguments);
    }

    protected void failWithDerivedActualAndMessage(Function<Match, ?> function, Match actualMatch, Object expected, String errorMessageFormat, Object... arguments) {
        failWithActualExpectedAndMessage(
                function.apply(actualMatch),
                expected,
                appendActualMatch(errorMessageFormat, actualMatch),
                arguments);
    }

    @Override
    protected AssertionError failureWithActualExpected(Object actual, Object expected, String errorMessageFormat, Object... arguments) {
        if (expected instanceof Match match) {
            errorMessageFormat = appendExpectedMatch(errorMessageFormat, match);
            expected = new FormattedMatch(match);
        }
        if (actual instanceof Match match) {
            errorMessageFormat = appendActualMatch(errorMessageFormat, match);
            actual = new FormattedMatch(match);
        }
        return super.failureWithActualExpected(actual, expected, errorMessageFormat, arguments);
    }

    private static String appendActualMatch(String errorMessageFormat, Match match) {
        return errorMessageFormat + System.lineSeparator()
               + "Actual   :" + formatted(match) + ANSI.RESET;
    }

    private static String appendExpectedMatch(String errorMessageFormat, Match match) {
        return errorMessageFormat + System.lineSeparator()
               + "Expected :" + formatted(match) + ANSI.RESET;
    }

    private static String formatted(Match match) {
        return FormattedMatch.formatted(match);
    }

    record FormattedMatch(Match match) {
        public String formatted() {
            return formatted(this.match);
        }

        public String toString() {
            return match.matchRange().format(match.fullString(), RangeFlexFormatter.SEPARATORS_ONLY) + System.lineSeparator() + System.lineSeparator() + ANSI.strip(match.toString());
        }

        private static String formatted(Match match) {
            return "{" + match.simpleFormat() + "} " + match;
        }
    }

    public static class MatchWithExpressionAssert extends MatchAssert<MatchWithExpressionAssert> {
        private final Expression expression;

        public MatchWithExpressionAssert(Match match, Expression expression) {
            super(match);
            this.expression = expression;
        }

        @API
        public MatchWithExpressionAssert hasOnlyBindingsMatchingExpression() {
            return hasOnlyBindingsMatching(expression);
        }

        @API
        public MatchWithExpressionAssert hasOnlyBindingsMatchingExpression(double filterByMinimalScore) {
            return hasOnlyBindingsMatching(filterByMinimalScore, expression);
        }

        @API
        public MatchWithExpressionAssert hasOnlyBindingsMatchingExpressionOrEmpty() {
            return hasOnlyBindingsMatchingOrEmpty(expression);
        }

        @API
        public MatchWithExpressionAssert hasOnlyBindingsPartiallyMatchingExpression() {
            return hasOnlyBindingsPartiallyMatching(expression);
        }

        @API
        public MatchWithExpressionAssert debugBindings() {
            return debugBindings(expression);
        }
    }
}
