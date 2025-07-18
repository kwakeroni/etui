package com.quaxantis.etui.template.expression.matching;

import com.quaxantis.etui.template.test.GenericTreeModel;
import com.quaxantis.support.ide.API;
import com.quaxantis.support.util.ANSI;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.util.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;

public class BindingsAssert extends AbstractCollectionAssert<BindingsAssert, Collection<? extends Binding>, Binding, ObjectAssert<Binding>> {
    private final ResolvableExpression<?> expression;
    private final String fullString;

    BindingsAssert(ResolvableExpression<?> expression, String fullString, Collection<? extends Binding> bindings) {
        super(bindings, BindingsAssert.class);
        this.expression = expression;
        this.fullString = fullString;
    }

    @Override
    protected BindingsAssert newAbstractIterableAssert(Iterable<? extends Binding> iterable) {
        return new BindingsAssert(expression, fullString, Lists.newArrayList(iterable));
    }

    @Override
    protected ObjectAssert<Binding> toAssert(Binding value, String description) {
        return new ObjectAssert<>(value).describedAs(description);
    }

    public BindingsAssert filteredOnScore(double filterByMinimalScore) {
        return filteredOn(b -> b.score() >= filterByMinimalScore);
    }

    public AbstractIterableAssert<?, ? extends Iterable<? extends Map<String, String>>, Map<String, String>, ?> values() {
        return extracting(Binding::asMap);
    }

    @SafeVarargs
    public final BindingsAssert containValues(Map<String, String>... valueSets) {
        values()
                .containsExactlyInAnyOrder(valueSets);
        return this;
    }

    public BindingsAssert allFullyMatchExpression() {
        assertThat(actual.stream()
                           .map(expression::resolve)
                           .distinct())
                .containsExactlyInAnyOrder(fullString);
        return myself;
    }

    public BindingsAssert allFullyMatchExpressionOrEmpty() {
        assertThat(actual.stream()
                           .map(expression::resolve)
                           .distinct())
                .containsAnyOf(fullString, "");
        return myself;
    }

    @API
    public BindingsAssert allPartiallyMatchExpression() {
        assertThat(actual.stream()
                           .map(expression::resolve)
                           .distinct())
                .allMatch(fullString::contains, "is part of \"" + fullString + '"');
        return myself;
    }

    @API
    public BindingsAssert debug() {
        debugBindings(actual.stream(), actual.stream().map(Binding::match).distinct());
        return this;
    }

    static void debugBindings(Stream<? extends Binding> bindings, Stream<? extends Match> matches) {
        GenericTreeModel.build()
                .with(Match.class, m -> toHtml("%s [%s] %s".formatted(m.simpleFormat(), m, m.expression())), Match::parentMatches)
                .with(Binding.class, BindingsAssert::toNodeString, BindingsAssert::toNodeList)
                .withToString(RangeFlex.Applied.class, range -> toHtml(range.format()))
                .with(Binding.VariableInfo.class, BindingsAssert::toNodeString, Binding.VariableInfo::sources)
                .show(Map.entry("Bindings", bindings.sorted(comparing(Binding::score).reversed()).toList()), matches.toList());
    }


    private static String toNodeString(Binding binding) {
        String matchString = binding.match().appliedRange().format(RangeFlexFormatter.UNDERLINE_ONLY);
        return toHtml(matchString + " (" + ANSI.BOLD + binding.score() + ANSI.NOT_BOLD + " " + binding.scoreObject().reason() + ") variables = " + binding.boundVariables().toList());
    }

    private static List<Binding> toNodeList(Binding binding) {
        return binding.parentBindings().filter(not(Binding.Empty.class::isInstance)).toList();
    }

    private static String toNodeString(Binding.VariableInfo info) {
        return toHtml("%s=%s %s %s %s  - %s".formatted(
                info.name(),
                ((info.range() instanceof RangeFlex.Applied range) ? '"' + range.extractMax() + '"' : "null"),
                ANSI.BOLD + ((info.score() instanceof Match.Score score) ? score.value() : "?") + ANSI.NOT_BOLD,
                ANSI.ITALIC + ((info.score() instanceof Match.Score score) ? score.reason() : "") + ANSI.NOT_ITALIC,
                ((info.range() instanceof RangeFlex.Applied range) ? range.format() + range.range().lengthString() : "?"),
                info.binding().match()
        ));
    }

    private static String toHtml(String string) {
        String ansiHtml = ANSI.toHTML(string);
        return "<html>" + ansiHtml + "</html>";
    }
}
