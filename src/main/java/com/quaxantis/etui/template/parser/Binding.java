package com.quaxantis.etui.template.parser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Binding {

    Match match();

    default RangeFlex matchRange() {
        return match().matchRange();
    }

    boolean hasBoundVariables();

    Stream<String> boundVariables();

    Stream<Binding> parentBindings();

    default double score() {
        return scoreObject().value();
    }

    Score scoreObject();

    interface Score {
        double value();

        String reason();


        static Score base(String reason) {
            return Score.of(1.0, reason);
        }

        static Score of(double factor, String reason) {
            return new Score() {
                @Override
                public double value() {
                    return factor;
                }

                @Override
                public String reason() {
                    return reason;
                }
            };
        }

        default Score times(double factor, String reason) {
            Score parent = this;
            return new Score() {
                @Override
                public double value() {
                    return parent.value() * factor;
                }

                @Override
                public String reason() {
                    return reason;
                }
            };
        }

        default Score times(Score right, String reason) {
            Score left = this;
            return new Score() {
                @Override
                public double value() {
                    return left.value() * right.value();
                }

                @Override
                public String reason() {
                    return reason;
                }
            };
        }
    }

//    Optional<Score> scoreOf(String variable);
//    Optional<VariableInfo> infoOf(String variable);

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

    default Stream<Map.Entry<String, RangeFlex.Applied>> boundValueRanges() {
        return boundVariables().map(var -> Map.entry(var, valueRangeOf(var).orElseThrow()));
    }

    default Binding with(@Nonnull Match directParent, @Nonnull String boundVariable, @Nullable String valueRepresentation) {
        Binding parent = this;
        class Single implements Binding {

            @Override
            public Match match() {
                return directParent;
            }

            @Override
            public Score scoreObject() {
                Score boundVariableScore = boundVariableScore();
                return parent.scoreObject().times(boundVariableScore, boundVariableScore.reason());
            }

            @Override
            public Stream<Binding> parentBindings() {
                return Stream.of(parent);
            }

//            @Override
//            public Optional<VariableInfo> infoOf(String variable) {
//                Optional<VariableInfo> parentInfo = parent.infoOf(variable);
//                if (boundVariable.equals(variable)) {
//                    return Optional.of(new VariableInfo(variable, this, boundVariableScore(), boundRange(), parentInfo.stream().toList()));
//                } else {
//                    return parentInfo;
//                }
//            }
//
//            @Override
//            public Optional<Score> scoreOf(String variable) {
//                return (boundVariable.equals(variable)) ? Optional.of(this.boundVariableScore()) : parent.scoreOf(variable);
//            }

            private Score boundVariableScore() {
                return match().givenBindings()
                        .filter(entry -> entry.getKey().equals(boundVariable))
                        .map(Map.Entry::getValue)
                        .map(givenRange -> score(boundRange(), givenRange))
                        .reduce(Score.of(1.0, boundVariable + "=" + boundRange().format()), (current, given) -> current.times(given, current.reason() + " & " + given.reason()));
            }

            private Score score(RangeFlex.Applied bound, RangeFlex.Applied given) {
                String value1 = bound.extractMax();
                String value2 = given.extractMax();
                if (value1.equals(value2)) {
                    return Score.of(1.0, "equal to given value: " + given);
                } else if (value1.isEmpty() && !value2.isEmpty()) {
                    return Score.of(0.5, "omitted given value: " + given.format());
                } else {
                    return RangeFlex.Applied.merge(bound, given).map(merged -> Score.of(.9, "merged with given value: " + given.format()))
                            .orElseGet(() -> Score.of(0.2, "different from given value: " + given.format()));
                }
            }

            @Override
            public boolean hasBoundVariables() {
                return true;
            }

            @Override
            public Stream<String> boundVariables() {
                return Stream.concat(parent.boundVariables(), Stream.of(boundVariable)).distinct();
            }

            @Override
            public Optional<RangeFlex.Applied> valueRangeOf(String variable) {
                return (boundVariable.equals(variable)) ? Optional.of(boundRange()) : parent.valueRangeOf(variable);
            }

            private RangeFlex.Applied boundRange() {
                return directParent.appliedRange();
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

    default Binding adaptScore(UnaryOperator<Score> operator) {
        Binding parent = this;
        return new Binding() {
            @Override
            public Stream<Binding> parentBindings() {
                return Stream.of(parent);
            }

            @Override
            public Match match() {
                return parent.match();
            }

            @Override
            public boolean hasBoundVariables() {
                return parent.hasBoundVariables();
            }

            @Override
            public Stream<String> boundVariables() {
                return parent.boundVariables();
            }

            @Override
            public Score scoreObject() {
                return operator.apply(parent.scoreObject());
            }
//
//            @Override
//            public Optional<Score> scoreOf(String variable) {
//                return parent.scoreOf(variable);
//            }
//
//            @Override
//            public Optional<VariableInfo> infoOf(String variable) {
//                return parent.infoOf(variable);
//            }

            @Override
            public Optional<RangeFlex.Applied> valueRangeOf(String variable) {
                return parent.valueRangeOf(variable);
            }
        };
    }

    static Binding empty(Match parent) {
        return new Empty(parent);
    }

    @Deprecated
    static Binding of(Match.RootMatch rootMatch, Map<String, String> values) {
        var valueMap = Map.copyOf(values);
        class OfMap implements Binding {
            @Override
            public Match match() {
                return rootMatch;
            }

            @Override
            public Stream<Binding> parentBindings() {
                return Stream.empty();
            }

            @Override
            public Score scoreObject() {
                return Score.base("map");
            }

//            @Override
//            public Optional<Score> scoreOf(String variable) {
//                return valueOf(variable).map(_ -> scoreObject());
//            }
//
//            @Override
//            public Optional<VariableInfo> infoOf(String variable) {
//                return valueOf(variable).map(value -> new VariableInfo(variable, this, scoreObject(), RangeFlex.Applied.ofCompleteFixed(value),List.of()));
//            }

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

    static Binding combine(@Nonnull Match match, @Nonnull RangeFlex matchRange, @Nonnull Binding one, @Nonnull Binding two) {

        class Expanded implements Binding {

            public Expanded(Binding parent) {
                this.parent = parent;
            }

            private final Binding parent;

            @Override
            public Match match() {
                return match;
            }

            @Override
            public Score scoreObject() {
                return parent.scoreObject().times(1.0, "expanded");
            }

            @Override
            public Stream<Binding> parentBindings() {
                return Stream.of(parent);
            }

//            @Override
//            public Optional<Score> scoreOf(String variable) {
//                return parent.scoreOf(variable);
//            }
//
//            @Override
//            public Optional<VariableInfo> infoOf(String variable) {
//                Optional<VariableInfo> info2 = parent.infoOf(variable);
//                return valueRangeOf(variable).map(range -> new VariableInfo(variable, this, scoreOf(variable).orElseThrow(), range, info2.stream().toList()));
//            }

            @Override
            public RangeFlex matchRange() {
                return matchRange;
            }

            @Override
            public boolean hasBoundVariables() {
                return parent.hasBoundVariables();
            }

            @Override
            public Stream<String> boundVariables() {
                return parent.boundVariables();
            }

            @Override
            public Optional<String> valueOf(String variable) {
                return parent.valueOf(variable);
            }

            @Override
            public Optional<RangeFlex.Applied> valueRangeOf(String variable) {
                return parent.valueRangeOf(variable);
            }

            @Nullable
            @Override
            public String valueRepresentation(String variable) {
                return parent.valueRepresentation(variable);
            }

            @Override
            public String toString() {
                return asString(this);
            }
        }

        class Combined implements Binding {
            private final Binding one;
            private final Binding two;

            public Combined(Binding one, Binding two) {
                this.one = one;
                this.two = two;
            }

            @Override
            public Match match() {
                return match;
            }

            @Override
            public Score scoreObject() {
                return one.scoreObject().times(two.scoreObject(), "combined");
            }

            @Override
            public Stream<Binding> parentBindings() {
                return Stream.of(one, two);
            }

//            @Override
//            public Optional<Score> scoreOf(String variable) {
//                return one.scoreOf(variable)
//                        .map(scoreOne -> two.scoreOf(variable).map(scoreTwo -> scoreOne.times(scoreTwo, "combined")).orElse(scoreOne))
//                        .or(() -> two.scoreOf(variable));
//            }
//
//            @Override
//            public Optional<VariableInfo> infoOf(String variable) {
//                Optional<VariableInfo> info1 = one.infoOf(variable);
//                Optional<VariableInfo> info2 = two.infoOf(variable);
//                return valueRangeOf(variable).map(range -> new VariableInfo(variable, this, scoreOf(variable).orElseThrow(), range, Stream.concat(info1.stream(), info2.stream()).toList()));
//            }

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

    private static String asString(Binding binding) {
        return binding.boundVariables().map(v -> v + "=" + binding.valueOf(v).map(s -> '"' + s + '"').orElse("null")
                                                 + "(" + binding.valueRepresentation(v) + ")")
                .collect(Collectors.joining(", ", "Binding{", "}"));
    }

    record VariableInfo(String name, Binding binding, Score score, RangeFlex.Applied range,
                        List<VariableInfo> sources) {}

    ;

    class Empty implements Binding {

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
        public Match match() {
            return match;
        }

        @Override
        public RangeFlex matchRange() {
            return matchRange;
        }

        @Override
        public Stream<Binding> parentBindings() {
            return Stream.empty();
        }

        @Override
        public Score scoreObject() {
            return Score.base("empty");
        }
//
//        @Override
//        public Optional<Score> scoreOf(String variable) {
//            return Optional.empty();
//        }
//
//        @Override
//        public Optional<VariableInfo> infoOf(String variable) {
//            return Optional.empty();
//        }

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
}
