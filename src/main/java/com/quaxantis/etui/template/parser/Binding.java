package com.quaxantis.etui.template.parser;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Binding {

    static final Binding EMPTY = new Binding() {
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
    };

    Stream<String> boundVariables();

    Optional<String> valueOf(String variable);

    default Map<String, String> asMap() {
        return boundVariables().collect(Collectors.toUnmodifiableMap(key -> key, key -> valueOf(key).orElseThrow()));
    }

    default Binding with(@Nonnull String boundVariable, @Nonnull String value) {
        Binding parent = this;
        return new Binding() {

            @Override
            public Stream<String> boundVariables() {
                return Stream.concat(parent.boundVariables(), Stream.of(boundVariable));
            }

            @Override
            public Optional<String> valueOf(String variable) {
                return (boundVariable.equals(variable)) ? Optional.of(value) : parent.valueOf(boundVariable);
            }

            @Override
            public String toString() {
                return asString(this);
            }
        };
    }

    static Binding empty() {
        return EMPTY;
    }

    static Binding combine(@Nonnull Binding one, @Nonnull Binding two) {
        return new Binding() {
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

            @Override
            public String toString() {
                return asString(this);
            }
        };
    }

    private static String asString(Binding binding) {
        return binding.boundVariables().map(v -> v + "=" + binding.valueOf(v).orElse(null)).collect(Collectors.joining(", ", "{", "}"));
    }
}
