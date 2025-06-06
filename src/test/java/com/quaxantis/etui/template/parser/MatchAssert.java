package com.quaxantis.etui.template.parser;

import com.quaxantis.support.ide.API;
import com.quaxantis.support.util.ANSI;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.presentation.StandardRepresentation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("UnusedReturnValue")
public class MatchAssert extends AbstractObjectAssert<MatchAssert, Match> {

    @API
    public static MatchAssert assertThat(Match match) {
        return new MatchAssert(match);
    }

    public MatchAssert(Match match) {
        super(match, MatchAssert.class);
        StandardRepresentation.registerFormatterForType(FormattedMatch.class, FormattedMatch::formatted);
    }

    @API
    public MatchAssert isNoMatch() {
        return isNoMatchSatisfying(_ -> {
        });
    }

    @API
    public MatchAssert isNoMatchSatisfying(Consumer<Match.NoMatch> consumer) {
        if (actual instanceof Match.NoMatch noMatch) {
            consumer.accept(noMatch);
        } else {
            failWithActualExpectedAndMessage(actual, new Match.NoMatch(actual.fullString(), null), "Expected actual not to be a match, but it was");
        }
        return myself;

    }

    @API
    public MatchAssert isNotFullMatch() {
        if (actual.isFullMatch()) {
            failWithActualAndMessage("Expected actual not to be a full match, but it was");
        }
        return myself;
    }

    @API
    public MatchAssert isFullMatch() {
        if (!actual.isFullMatch()) {
            failWithActualExpectedAndMessage(actual, new Match.RootMatch(actual.fullString()), "Expected actual to be a full match");
        }
        return myself;
    }

    @API
    public MatchAssert hasBoundVariables() {
        if (!actual.hasBoundVariables()) {
            failWithActualAndMessage("Expected actual to have bound variables, but it has not");
        }
        return myself;
    }

    @API
    public MatchAssert hasNoBoundVariables() {
        if (actual.hasBoundVariables()) {
            failWithActualAndMessage("Expected actual not to have bound variables, but it has");
        }
        return myself;
    }

    @API
    public MatchAssert hasMatchRepresentation(String expected) {
        String fullRepresentation = actual.matchRepresentation().replaceFirst("!\\d*<\\d*$", "");
        if (!expected.equals(fullRepresentation)) {
            failWithDerivedActualAndMessage(_ -> fullRepresentation, actual, expected, "Expected actual representation to be equal to");
        }
        return myself;
    }

    @API
    public MatchAssert hasMatchRepresentation(String expected, String expectedLength) {
        String fullRepresentation = actual.matchRepresentation();
        String expectedRepresentationWithLength = expected + "!" + expectedLength;
        if (!expectedRepresentationWithLength.equals(fullRepresentation)) {
            failWithDerivedActualAndMessage(_ -> fullRepresentation, actual, expected, "Expected actual representation to be equal to");
        }
        return myself;
    }


    @API
    @Deprecated
    public MatchAssert hasVariableValue(String variable, String expected) {
        Optional<String> actualValue = actual.valueOf(variable);
        if (actualValue.isEmpty()) {
            failWithActualAndMessage("Expected variable '%s' to be bound, but was not", variable);
        } else if (!Objects.equals(expected, actualValue.get())) {
            failWithActualAndMessage("Expected variable '%s' to have expected value", variable);
        }
        return myself;
    }

    @SafeVarargs
    public final MatchAssert hasBindings(Map<String, String>... bindings) {
        Assertions.assertThat(actual.bindings())
                .extracting(Binding::asMap)
                .containsExactlyInAnyOrder(bindings);
        return myself;
    }

    public MatchAssert isChoiceOfSatisfying(Consumer<List<Match>> consumer) {
        if (!(actual instanceof Match.ChoiceMatch(var choices))) {
            failWithActualAndMessage("Expected to be an instance of Match.ChoiceMatch, but was not");
        } else {
            consumer.accept(choices);
        }
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
}
