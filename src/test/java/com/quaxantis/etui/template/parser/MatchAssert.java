package com.quaxantis.etui.template.parser;

import com.quaxantis.support.ide.API;
import com.quaxantis.support.util.ANSI;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.presentation.StandardRepresentation;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("UnusedReturnValue")
public class MatchAssert<SELF extends MatchAssert<SELF>> extends AbstractObjectAssert<SELF, Match> {

    @API
    public static MatchAssert<? extends MatchAssert<?>> assertThat(Match match) {
        return new MatchAssert<>(match);
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
        if (actual instanceof Match.NoMatch) {
            failWithActualAndMessage("Expected actual to be a match, but it was not");
        }
        return myself;
    }

    @API
    public SELF isNoMatch() {
        if (!(actual instanceof Match.NoMatch)) {
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
        bindings().containValues(bindings);
        return myself;
    }

    @API
    public BindingsAssert bindings() {
        return new BindingsAssert(actual.expression(), actual.fullString(), actual.bindings().toList());
    }

    @API
    public SELF debugBindings() {
        BindingsAssert.debugBindings(actual.bindings(), Stream.of(actual));
        return myself;
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

        public BindingsAssert bindings() {
            return new BindingsAssert(expression, actual.fullString(), actual.bindings().toList());
        }
    }
}
