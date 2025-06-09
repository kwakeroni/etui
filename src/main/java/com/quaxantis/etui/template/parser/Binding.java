package com.quaxantis.etui.template.parser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Binding {

    Match match();

    RangeFlex matchRange();

    Stream<String> boundVariables();

    Optional<String> valueOf(String variable);

    @Nullable
    default String valueRepresentation(String variable) {
        return valueOf(variable).map(v -> '"' + v + '"').orElse(null);
    }

    default Map<String, String> asMap() {
        return boundVariables().collect(Collectors.toUnmodifiableMap(key -> key, key -> valueOf(key).orElseThrow()));
    }

    default Binding with(@Nonnull Match directParent, @Nonnull RangeFlex matchRange, @Nonnull String boundVariable, @Nonnull String value) {
        return with(directParent, matchRange, boundVariable, value, null);
    }

    default Binding with(@Nonnull Match directParent, @Nonnull RangeFlex matchRange, @Nonnull String boundVariable, @Nonnull String value, @Nullable String valueRepresentation) {
        Binding parent = this;
        class Single implements Binding {

            @Override
            public Match match() {
                return directParent;
            }

            @Override
            public RangeFlex matchRange() {
                return matchRange;
            }

            @Override
            public Stream<String> boundVariables() {
                return Stream.concat(parent.boundVariables(), Stream.of(boundVariable));
            }

            @Override
            public Optional<String> valueOf(String variable) {
                return (boundVariable.equals(variable)) ? Optional.of(value) : parent.valueOf(boundVariable);
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

    static Binding empty(@Nonnull Match parent, @Nonnull RangeFlex matchRange) {
        class Empty implements Binding {

            @Override
            public Match match() {
                return parent;
            }

            @Override
            public RangeFlex matchRange() {
                return matchRange;
            }

            @Override
            public Stream<String> boundVariables() {
                return Stream.empty();
            }

            @Override
            public Optional<String> valueOf(String variable) {
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
            public Stream<String> boundVariables() {
                return Stream.concat(one.boundVariables(), two.boundVariables()).distinct();
            }

            @Override
            public Optional<String> valueOf(String variable) {
                var leftOpt = one.valueOf(variable);
                var rightOpt = two.valueOf(variable);
                if (leftOpt.isPresent()) {
                    if (rightOpt.isPresent()) {
                        // TODO: merge left and right
                        return Optional.empty();
                    } else {
                        return leftOpt;
                    }
                } else {
                    return rightOpt;
                }
            }

            @Nullable
            @Override
            public String valueRepresentation(String variable) {
                var leftOpt = one.valueOf(variable);
                var rightOpt = two.valueOf(variable);
                if (leftOpt.isPresent()) {
                    if (rightOpt.isPresent()) {
                        // TODO: merge left and right
                        return "<empty> TODO: merge " + one.valueRepresentation(variable) + " and " + two.valueRepresentation(variable);
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
        return variablesAsString(binding);
    }

    private static String variablesAsString(Binding binding) {
        return binding.boundVariables().map(v -> v + "=" + binding.valueRepresentation(v)).collect(Collectors.joining(", ", "{", "}"));
    }

}
