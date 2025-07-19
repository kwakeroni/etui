package com.quaxantis.etui.template.expression.matching;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class Binding implements com.quaxantis.etui.template.expression.Binding {

    private Binding() {
    }

    abstract Stream<Binding> parentBindings();

    abstract Match match();

    RangeFlex matchRange() {
        return match().matchRange();
    }

    public double score() {
        return scoreObject().value();
    }

    Match.Score scoreObject() {
        return match().scoreObject();
    }

    public Optional<String> valueOf(String variable) {
        return valueRangeOf(variable).map(RangeFlex.Applied::extractMax);
    }

    abstract Optional<RangeFlex.Applied> valueRangeOf(String variable);

    @Nullable
    String valueRepresentation(String variable) {
        return valueOf(variable).map(v -> '"' + v + '"').orElse(null);
    }

    abstract boolean hasBoundVariables();

    abstract Stream<String> boundVariables();

    Map<String, String> asMap() {
        return boundVariables()
                .map(variable -> valueOf(variable).map(value -> Map.entry(variable, value)).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public String toString() {
        return boundVariables().map(v -> v + "=" + valueOf(v).map(s -> '"' + s + '"').orElse("null")
                                         + "(" + valueRepresentation(v) + ")")
                .collect(Collectors.joining(", ", "Binding{", "}"));
    }

    private static abstract class BindingAdapter extends Binding {

        private final Binding parent;

        BindingAdapter(Binding parent) {
            this.parent = parent;
        }


        @Override
        Stream<Binding> parentBindings() {
            return Stream.of(parent);
        }

        @Override
        Match match() {
            return parent.match();
        }

        @Override
        Optional<RangeFlex.Applied> valueRangeOf(String variable) {
            return parent.valueRangeOf(variable);
        }

        @Nullable
        @Override
        String valueRepresentation(String variable) {
            return parent.valueRepresentation(variable);
        }

        @Override
        boolean hasBoundVariables() {
            return parent.hasBoundVariables();
        }

        @Override
        Stream<String> boundVariables() {
            return parent.boundVariables();
        }

    }

    Binding with(@Nonnull Match match, @Nonnull String boundVariable, @Nullable String valueRepresentation) {
        class BoundVariable extends BindingAdapter {

            BoundVariable(Binding parent) {
                super(parent);
            }

            @Override
            Match match() {
                return match;
            }

            @Override
            boolean hasBoundVariables() {
                return true;
            }

            @Override
            Stream<String> boundVariables() {
                return Stream.concat(super.boundVariables(), Stream.of(boundVariable)).distinct();
            }

            @Override
            Optional<RangeFlex.Applied> valueRangeOf(String variable) {
                return (boundVariable.equals(variable)) ? Optional.of(boundRange()) : super.valueRangeOf(variable);
            }

            private RangeFlex.Applied boundRange() {
                return match.appliedRange();
            }

            @Nullable
            @Override
            String valueRepresentation(String variable) {
                return (valueRepresentation != null) ? valueRepresentation : super.valueRepresentation(variable);
            }
        }
        return new BoundVariable(this);
    }

    static Binding combine(@Nonnull Match match, @Nonnull RangeFlex matchRange, @Nonnull Binding one, @Nonnull Binding two) {

        class Expanded extends BindingAdapter {
            Expanded(Binding parent) {
                super(parent);
            }

            @Override
            Match match() {
                return match;
            }

            @Override
            RangeFlex matchRange() {
                return matchRange;
            }
        }

        class Combined extends Binding {
            private final Binding one;
            private final Binding two;

            Combined(Binding one, Binding two) {
                this.one = one;
                this.two = two;
            }

            @Override
            Match match() {
                return match;
            }

            @Override
            Stream<Binding> parentBindings() {
                return Stream.of(one, two);
            }

            @Override
            RangeFlex matchRange() {
                return matchRange;
            }

            @Override
            boolean hasBoundVariables() {
                return one.hasBoundVariables() || two.hasBoundVariables();
            }

            @Override
            Stream<String> boundVariables() {
                return Stream.concat(one.boundVariables(), two.boundVariables()).distinct();
            }

            @Override
            public Optional<String> valueOf(String variable) {
                var leftOpt = one.valueOf(variable);
                var rightOpt = two.valueOf(variable);
                if (leftOpt.isPresent()) {
                    if (rightOpt.isPresent() && !rightOpt.get().equals(leftOpt.get())) {
                        return super.valueOf(variable);
                    } else {
                        return leftOpt;
                    }
                } else {
                    return rightOpt;
                }
            }

            @Override
            Optional<RangeFlex.Applied> valueRangeOf(String variable) {
                var range1 = one.valueRangeOf(variable);
                var range2 = two.valueRangeOf(variable);
                if (range1.isPresent()) {
                    if (range2.isPresent()) {
                        return mergeRanges(variable, range1.get(), range2.get());
                    } else {
                        return range1;
                    }
                } else {
                    return range2;
                }
            }

            private Optional<RangeFlex.Applied> mergeRanges(String variable, RangeFlex.Applied one, RangeFlex.Applied two) {
                Optional<RangeFlex.Applied> result = RangeFlex.Applied.merge(one, two);
                if (result.isEmpty()) {
                    System.out.printf(">>> Variable %s cannot merge bindings '%s' and '%s'%n", variable, one, two);
                }
                return result;
            }

            @Nullable
            @Override
            String valueRepresentation(String variable) {
                var leftOpt = one.valueOf(variable);
                var rightOpt = two.valueOf(variable);
                if (leftOpt.isPresent()) {
                    if (rightOpt.isPresent()) {
                        return "%s = %s & %s".formatted(
                                one.valueRangeOf(variable).flatMap(app1 -> two.valueRangeOf(variable).flatMap(app2 -> RangeFlex.Applied.merge(app1, app2)))
                                        .map(RangeFlex.Applied::format).orElse(null),
                                one.valueRangeOf(variable).map(RangeFlex.Applied::format).orElse(null),
                                two.valueRangeOf(variable).map(RangeFlex.Applied::format).orElse(null)
                        );
                    } else {
                        return one.valueRepresentation(variable);
                    }
                } else if (rightOpt.isPresent()) {
                    return two.valueRepresentation(variable);
                } else {
                    return null;
                }
            }
        }

        if (one instanceof Empty) {
            if (two instanceof Empty) {
                return new Empty(match, matchRange);
            } else {
                return new Expanded(two);
            }
        } else if (two instanceof Empty) {
            return new Expanded(one);
        }

        return new Combined(one, two);
    }

    Binding withNormalizedScore() {
        if (this.match().isFullMatch()) {
            return this;
        } else {
            class WithScore extends BindingAdapter {
                WithScore(Binding parent) {
                    super(parent);
                }

                @Override
                Match.Score scoreObject() {
                    return ((UnaryOperator<Match.Score>) score -> score.times(0.5, "no full match")).apply(super.scoreObject());
                }
            }
            return new WithScore(this);
        }
    }

    static Binding empty(Match parent) {
        return new Empty(parent);
    }

    static class Empty extends Binding {

        private final Match match;
        private final RangeFlex matchRange;

        Empty(Match match) {
            this(match, match.matchRange());
        }

        Empty(Match match, RangeFlex matchRange) {
            this.match = match;
            this.matchRange = matchRange;
        }

        @Override
        Match match() {
            return match;
        }

        @Override
        RangeFlex matchRange() {
            return matchRange;
        }

        @Override
        Stream<Binding> parentBindings() {
            return Stream.empty();
        }

        @Override
        boolean hasBoundVariables() {
            return false;
        }

        @Override
        Stream<String> boundVariables() {
            return Stream.empty();
        }

        @Override
        Optional<RangeFlex.Applied> valueRangeOf(String variable) {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return "{}";
        }
    }

    record VariableInfo(String name, Binding binding, Match.Score score, RangeFlex.Applied range,
                        List<VariableInfo> sources) {}

}



