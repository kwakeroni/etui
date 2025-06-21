package com.quaxantis.etui.template.parser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Binding {

    Match match();

    default RangeFlex matchRange() {
        return match().matchRange();
    }

    boolean hasBoundVariables();

    Stream<String> boundVariables();

    default Optional<String> valueOf(String variable) {
        return valueRangeOf(variable).map(RangeFlex.Applied::extractMax);
    }

    Optional<RangeFlex.Applied> valueRangeOf(String variable);

    @Nullable
    default String valueRepresentation(String variable) {
        return valueOf(variable).map(v -> '"' + v + '"').orElse(null);
    }

    default Map<String, String> asMap() {
        return boundVariables()
                .map(variable -> valueOf(variable).map(value -> Map.entry(variable, value)).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    default Binding with(@Nonnull Match directParent, @Nonnull String boundVariable, @Nullable String valueRepresentation) {
        Binding parent = this;
        class Single implements Binding {

            @Override
            public Match match() {
                return directParent;
            }

            @Override
            public boolean hasBoundVariables() {
                return true;
            }

            @Override
            public Stream<String> boundVariables() {
                return Stream.concat(parent.boundVariables(), Stream.of(boundVariable));
            }

            @Override
            public Optional<RangeFlex.Applied> valueRangeOf(String variable) {
                return (boundVariable.equals(variable)) ? Optional.of(directParent.appliedRange()) : parent.valueRangeOf(boundVariable);
            }

            @Nullable
            @Override
            public String valueRepresentation(String variable) {
                return (valueRepresentation != null) ? valueRepresentation : Binding.super.valueRepresentation(variable);
            }

            @Override
            public String toString() {
                return asString(this);
            }
        }
        return new Single();
    }

    static Binding empty(Match parent) {
        class Empty implements Binding {

            @Override
            public Match match() {
                return parent;
            }

            @Override
            public boolean hasBoundVariables() {
                return false;
            }

            @Override
            public Stream<String> boundVariables() {
                return Stream.empty();
            }

            @Override
            public Optional<RangeFlex.Applied> valueRangeOf(String variable) {
                return Optional.empty();
            }

            @Override
            public String toString() {
                return "{}";
            }
        }
        ;
        return new Empty();
    }

    static Binding of(Match.RootMatch rootMatch, Map<String, String> values) {
        var valueMap = Map.copyOf(values);
        class OfMap implements Binding {
            @Override
            public Match match() {
                return rootMatch;
            }

            @Override
            public boolean hasBoundVariables() {
                return !valueMap.isEmpty();
            }

            @Override
            public Stream<String> boundVariables() {
                return valueMap.keySet().stream();
            }

            @Override
            public Optional<String> valueOf(String variable) {
                return Optional.ofNullable(valueMap.get(variable));
            }

            @Override
            public Optional<RangeFlex.Applied> valueRangeOf(String variable) {
                return valueOf(variable).map(RangeFlex.Applied::ofCompleteFixed);
            }
        }
        return new OfMap();
    }

    static Binding combine(@Nonnull Match directParent, @Nonnull RangeFlex matchRange, @Nonnull Binding one, @Nonnull Binding two) {
        class Combined implements Binding {

            @Override
            public Match match() {
                return directParent;
            }

            @Override
            public RangeFlex matchRange() {
                return matchRange;
            }

            @Override
            public boolean hasBoundVariables() {
                return one.hasBoundVariables() || two.hasBoundVariables();
            }

            @Override
            public Stream<String> boundVariables() {
                return Stream.concat(one.boundVariables(), two.boundVariables()).distinct();
            }

            @Override
            public Optional<String> valueOf(String variable) {
                var leftOpt = one.valueOf(variable);
                var rightOpt = two.valueOf(variable);
                if (leftOpt.isPresent()) {
                    if (rightOpt.isPresent() && !rightOpt.get().equals(leftOpt.get())) {
                        return Binding.super.valueOf(variable);
                    } else {
                        return leftOpt;
                    }
                } else {
                    return rightOpt;
                }
            }

            @Override
            public Optional<RangeFlex.Applied> valueRangeOf(String variable) {
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
            public String valueRepresentation(String variable) {
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

            @Override
            public String toString() {
                return asString(this);
            }
        }
        return new Combined();
    }

    private static String asString(Binding binding) {
        return binding.boundVariables().map(v -> v + "=" + binding.valueOf(v).map(s -> '"' + s + '"').orElse("null")
                                                 + "(" + binding.valueRepresentation(v) + ")")
                .collect(Collectors.joining(", ", "Binding{", "}"));
    }

}
