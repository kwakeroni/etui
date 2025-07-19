package com.quaxantis.etui.template.expression.matching;

import com.quaxantis.support.ide.API;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.util.Lists;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class MatchingAssert extends AbstractCollectionAssert<MatchingAssert, Collection<? extends Match>, Match, MatchAssert> {
    private final ResolvableExpression<?> expression;
    private final String fullString;

    public <E> MatchingAssert(E expression, BiFunction<E, com.quaxantis.etui.template.expression.Binding, String> resolver, String fullString, Collection<? extends Match> matches) {
        this(new ResolvableExpression<>(expression, resolver), fullString, matches);
    }

    MatchingAssert(ResolvableExpression<?> expression, String fullString, Collection<? extends Match> matches) {
        super(matches, MatchingAssert.class);
        this.expression = expression;
        this.fullString = fullString;
    }

    @Override
    protected MatchingAssert newAbstractIterableAssert(Iterable<? extends Match> iterable) {
        return new MatchingAssert(this.expression, this.fullString, Lists.newArrayList(iterable));
    }


    @Override
    protected MatchAssert toAssert(Match value, String description) {
        var matchAssert = new MatchAssert(expression, value);
        return (description == null) ? matchAssert : matchAssert.describedAs(description);
    }

    public MatchingAssert assertAll(Consumer<MatchAssert> assertConsumer) {
        return allSatisfy(match -> assertWith(assertConsumer, match));
    }

    @SafeVarargs
    @SuppressWarnings("RedundantCast")
    public final MatchingAssert assertExactlyInAnyOrder(Consumer<MatchAssert>... assertConsumers) {
        return satisfiesExactlyInAnyOrder(
                (Consumer<Match>[]) Arrays.stream(assertConsumers)
                        .map(assertConsumer -> (Consumer<Match>)
                                (Match match) -> assertWith(assertConsumer, match))
                        .toArray(this::<Match>newConsumerArray));
    }

    private <T> Consumer<T>[] newConsumerArray(int size) {
        return (Consumer<T>[]) new Consumer[size];
    }

    private void assertWith(Consumer<MatchAssert> assertConsumer, Match match) {
        assertConsumer.accept(toAssert(match, navigationDescription("match")));
    }

    private Stream<Binding> actualBindings() {
        return actual.stream().flatMap(Match::bindings).map(Binding::withNormalizedScore);
    }

    public BindingsAssert bindings() {
        return new BindingsAssert(expression, fullString, actualBindings().toList());
    }

    public MatchingAssert hasNoBindings() {
        bindings().isEmpty();
        return this;
    }

    public MatchingAssert hasOnlyEmptyBindings() {
        bindings().allMatch(not(Binding::hasBoundVariables));
        return this;
    }

    @API
    @SafeVarargs
    public final MatchingAssert hasBindings(Map<String, String>... bindings) {
        bindings().containValues(bindings);
        return this;
    }

    public MatchingAssert debugBindings() {
        BindingsAssert.debugBindings(actualBindings(), actual.stream());
        return this;
    }
}
