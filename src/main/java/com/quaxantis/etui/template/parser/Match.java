package com.quaxantis.etui.template.parser;

import com.quaxantis.support.util.Result;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;

public sealed interface Match {

    static Match of(@Nonnull Expression expression, @Nonnull String fullString, @Nonnull Map<String, String> bindings) {
        Map<String, RangeFlex.Applied> givenBindings = bindings.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> RangeFlex.Applied.ofCompleteFixed(entry.getValue())));
        return new RootMatch(expression, fullString, givenBindings);
    }

    default double score() {
        return scoreObject().value();
    }

    Score scoreObject();

    Expression expression();

    String fullString();

    default String multilineFormat() {
        return multilineFormat("  ");
    }

    String multilineFormat(String indent);

    RangeFlex matchRange();

    default RangeFlex.Applied appliedRange() {
        return matchRange().applyTo(fullString());
    }

    Match constrain(Constraint constraint, Expression causeExpression);

    private static Match tryConstrain(Match self, Supplier<Match> supplier) {
        try {
            return supplier.get();
        } catch (Exception exc) {
            return new NoMatch(self.expression(), self.fullString(), exc.toString(), self);
        }
    }

    boolean hasBoundVariables();

    default Stream<Binding> bindings() {
        return Stream.of(Binding.empty(this));
    }

    Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings();

    List<Match> parentMatches();

    default boolean isFullMatch() {
        int length = fullString().length();
        RangeFlex range = matchRange();
        return range.contains(0) && range.contains(length - 1)
               && range.maxLength().map(max -> max >= length).orElse(true);
    }

    default String matchedString() {
        return matchRange().extractMaxFrom(fullString());
    }

    default String matchRepresentation() {
        return matchRange().applyTo(fullString()) + matchRange().lengthRange().transform((from, to) -> "!" + from + "<" + to);
    }

    default String simpleFormat() {
        return matchRange().format(fullString());
    }

    default Match binding(@Nonnull Expression expression, @Nonnull String boundVariable) {
        return new BindingMatch(expression, this, boundVariable);
    }

    default Match bindingAll(@Nonnull Expression causeExpression, @Nonnull List<Binding> bindings) {
        Supplier<Stream<Map.Entry<String, RangeFlex.Applied>>> givenBindings = () ->
                bindings.stream().flatMap(
                        binding -> binding.boundVariables()
                                .map(var -> binding.valueRangeOf(var).map(range -> Map.entry(var, range)))
                                .flatMap(Optional::stream)
                );
        return new AddGivenBindings(causeExpression, this, givenBindings);
    }

    default Match andThen(UnaryOperator<Match> mapper) {
        return mapper.apply(this);
    }

    Stats recursiveStats();

    interface Score {
        double value();

        String reason();

        private static String asString(Score score) {
            return score.value() + "[" + score.reason() + "]";
        }

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

                @Override
                public String toString() {
                    return asString(this);
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

                @Override
                public String toString() {
                    return asString(this);
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


                @Override
                public String toString() {
                    return asString(this);
                }

            };
        }
    }

    record Stats(int depth, int size, int noMatches) {
        Stats andThen(IntUnaryOperator depthOperator, IntUnaryOperator sizeOperator, IntUnaryOperator noMatchesOperator) {
            return new Stats(depthOperator.applyAsInt(depth), sizeOperator.applyAsInt(size), noMatchesOperator.applyAsInt(noMatches));
        }

        static Collector<Match, ?, Stats> toStats() {
            return mapping(Match::recursiveStats,
                           teeing(
                                   mapping(Stats::depth, maxBy(Comparator.naturalOrder())),
                                   teeing(
                                           summingInt(Stats::size),
                                           summingInt(Stats::noMatches),
                                           (size, noMatches) -> new int[]{size, noMatches}
                                   ),
                                   (depth, array) -> new Stats(depth.orElse(0), array[0], array[1])
                           )
            );
        }
    }

    record NoMatch(@Nonnull Expression expression, @Nonnull String fullString, @Nullable String reason,
                   Collection<Match> mismatches) implements Match {

        public NoMatch(@Nonnull Expression expression, @Nonnull String fullString, @Nullable String reason, Match... mismatches) {
            this(expression, fullString, reason, List.of(mismatches));
        }

        @Override
        public Stats recursiveStats() {
            return mismatches.stream()
                    .collect(Stats.toStats())
                    .andThen(d -> d + 1, s -> s + 1, n -> n + 1);
        }

        @Override
        public RangeFlex matchRange() {
            return RangeFlex.empty();
        }

        @Override
        public Match constrain(Constraint constraint, Expression causeExpression) {
            return this;
        }

        @Override
        public Match andThen(UnaryOperator<Match> mapper) {
            return this;
        }

        @Override
        public boolean hasBoundVariables() {
            return false;
        }

        @Override
        public Score scoreObject() {
            return Score.of(0.0, "no match");
        }

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            return Stream.empty();
        }

        @Override
        public List<Match> parentMatches() {
            return List.of();
        }

        @Override
        public String multilineFormat(String indent) {
            StringBuilder builder = new StringBuilder()
                    .append(simpleFormat())
                    .append(indent)
                    .append("no match");
            if (reason != null) {
                builder.append(" : because ").append(reason);
            }
            String subIndent = spaced(indent) + " |-- ";

            for (Match mismatch : mismatches) {
                builder.append(System.lineSeparator()).append(mismatch.multilineFormat(subIndent));
            }
            return builder.toString();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    record RootMatch(@Nonnull Expression expression, @Nonnull String fullString,
                     @Nonnull Map<String, RangeFlex.Applied> givenBindingsMap) implements Match {

        @Override
        public Stats recursiveStats() {
            return new Stats(1, 1, 0);
        }

        @Override
        public RangeFlex matchRange() {
            return RangeFlex.ofComplete(fullString).constrain(Constraint.toMinLength(0));
        }

        @Override
        public String matchedString() {
            return fullString;
        }

        @Override
        public Match constrain(Constraint constraint, Expression causeExpression) {
            return tryConstrain(this, () -> new PartialMatch(causeExpression, this, matchRange().constrain(constraint)));
        }

        @Override
        public boolean hasBoundVariables() {
            return false;
        }

        @Override
        public Score scoreObject() {
            return Score.base("no bindings");
        }

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            return givenBindingsMap.entrySet().stream();
        }

        @Override
        public List<Match> parentMatches() {
            return List.of();
        }

        @Override
        public String multilineFormat(String indent) {
            return fullString;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[given=" + givenBindings().count() + ", string=" + fullString + "]";
        }

    }

    record BindingMatch(@Nonnull Expression expression, @Nonnull Match parent,
                        @Nonnull String boundVariable) implements Match {

        public BindingMatch {
            parent.bindings()
                    .map(binding -> binding.valueRangeOf(boundVariable))
                    .flatMap(Optional::stream)
                    .filter(inherited -> RangeFlex.Applied.merge(inherited, parent.appliedRange()).isEmpty())
                    .findAny()
                    .ifPresent(incompatible -> {
                        throw new IllegalStateException("Cannot combine new binding match for variable %s with inherited binding match:%n%s%n%s".formatted(
                                boundVariable, parent.appliedRange(), incompatible));
                    });

        }

        @Override
        public Stats recursiveStats() {
            Stats parentStats = parent.recursiveStats();
            return new Stats(parentStats.depth() + 1, parentStats.size() + 1, parentStats.noMatches());
        }

        @Override
        public String fullString() {
            return parent.fullString();
        }

        public String matchRepresentation() {
            return parent.matchRepresentation();
        }

        @Override
        public RangeFlex matchRange() {
            return parent.matchRange();
        }

        @Override
        public String matchedString() {
            return parent.matchedString();
        }

        @Override
        public Match constrain(Constraint constraint, Expression causeExpression) {
            return parent.constrain(constraint, causeExpression).andThen(newParent -> {
                BindingMatch newMatch = new BindingMatch(expression, newParent, boundVariable);
                List<RangeFlex.Applied> newValues = newMatch.bindings().map(binding -> binding.valueRangeOf(boundVariable)).flatMap(Optional::stream).toList();
                List<RangeFlex.Applied> parValues = parent.bindings().map(binding -> binding.valueRangeOf(boundVariable)).flatMap(Optional::stream).toList();

//                int score = newValues.stream().flatMap(newRange -> parValues.stream().map(
//                                parentRange -> RangeFlex.Applied.merge(parentRange, newRange).map(_ -> 1).orElse(9)))
//                        .mapToInt(i -> i)
//                        .max()
//                        .orElse(1);

//                System.out.println("Constrained binding: " + score + " = " + parValues.stream().map(a -> a.format(RangeFlexFormatter.SEPARATORS_ONLY)).toList() + " ->  " + newValues.stream().map(a -> a.format(RangeFlexFormatter.SEPARATORS_ONLY)).toList());


//                parent.bindings()
//                    .map(binding -> binding.valueRangeOf(boundVariable))
//                    .flatMap(Optional::stream)
//                    .filter(inherited -> RangeFlex.Applied.merge(inherited, parent.appliedRange()).isEmpty())
//                    .findAny()
//                    .ifPresent(incompatible -> {
//                        throw new IllegalStateException("Cannot combine new binding match for variable %s with inherited binding match:%n%s%n%s".formatted(
//                                boundVariable, parent.appliedRange(), incompatible));
//                    });
//
                return newMatch;
            });
        }

        @Override
        public boolean isFullMatch() {
            return parent.isFullMatch();
        }

        @Override
        public Score scoreObject() {
            Score boundVariableScore = boundVariableScore();
            return parent.scoreObject().times(boundVariableScore, boundVariableScore.reason());
        }

        private Score boundVariableScore() {
            return givenBindings()
                    .filter(entry -> entry.getKey().equals(boundVariable))
                    .map(Map.Entry::getValue)
                    .map(givenRange -> score(appliedRange(), givenRange))
                    .reduce(Score.of(1.0, boundVariable + "=" + appliedRange().format()), (current, given) -> current.times(given, current.reason() + " & " + given.reason()));
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
        public Stream<Binding> bindings() {
            String format = simpleFormat();
            return parent.bindings().map(binding -> binding.with(this, boundVariable, format));
        }

        @Override
        public boolean hasBoundVariables() {
            return true;
        }

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            return parent.givenBindings();
        }

        @Override
        public List<Match> parentMatches() {
            return List.of(parent);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + boundVariable + "=\"" + parent.appliedRange().extractMax() + "\"]";
        }

        @Override
        public String simpleFormat() {
            return parent.simpleFormat();
        }

        @Override
        public String multilineFormat(String indent) {
            return simpleFormat() + indent + getClass().getSimpleName() + "[" + boundVariable + "] " + matchRange().lengthRange() + " << " + expression().representationString();
        }
    }

    record AddGivenBindings(@Nonnull Expression expression, @Nonnull Match parent,
                            @Nonnull Supplier<Stream<Map.Entry<String, RangeFlex.Applied>>> addedGivenBindings) implements Match {

        @Override
        public Stats recursiveStats() {
            Stats parentStats = parent.recursiveStats();
            return new Stats(parentStats.depth() + 1, parentStats.size() + 1, parentStats.noMatches());
        }

        @Override
        public String fullString() {
            return parent.fullString();
        }

        public String matchRepresentation() {
            return parent.matchRepresentation();
        }

        @Override
        public RangeFlex matchRange() {
            return parent.matchRange();
        }

        @Override
        public String matchedString() {
            return parent.matchedString();
        }

        @Override
        public Match constrain(Constraint constraint, Expression causeExpression) {
            return parent.constrain(constraint, causeExpression).andThen(parent -> new AddGivenBindings(expression, parent, addedGivenBindings));
        }

        @Override
        public boolean isFullMatch() {
            return parent.isFullMatch();
        }

        @Override
        public boolean hasBoundVariables() {
            return parent.hasBoundVariables();
        }

        @Override
        public Score scoreObject() {
            return parent.scoreObject();
        }

        @Override
        public Stream<Binding> bindings() {
            return parent.bindings();
        }

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            return Stream.concat(parent.givenBindings(), addedGivenBindings.get());
        }

        @Override
        public List<Match> parentMatches() {
            return List.of(parent);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[given=" + addedGivenBindings.get().map(Map.Entry::getKey).collect(joining(", ")) + "]";
        }

        @Override
        public String simpleFormat() {
            return parent.simpleFormat();
        }

        @Override
        public String multilineFormat(String indent) {
            return simpleFormat() + indent + this + " " + matchRange().lengthRange() + " << " + expression().representationString();
        }
    }

    record PartialMatch(@Nonnull Expression expression, @Nonnull Match parent,
                        @Nonnull RangeFlex range) implements Match {

        public PartialMatch {
            Objects.requireNonNull(parent, "parent");
            Objects.requireNonNull(range, "range");
        }

        @Override
        public Stats recursiveStats() {
            Stats parentStats = parent.recursiveStats();
            return new Stats(parentStats.depth() + 1, parentStats.size() + 1, parentStats.noMatches());
        }

        @Override
        public String fullString() {
            return parent.fullString();
        }

        @Override
        public RangeFlex matchRange() {
            return range;
        }

        @Override
        public Match constrain(Constraint constraint, Expression causeExpression) {
            return Match.tryConstrain(this, () -> new PartialMatch(causeExpression, this, range.constrain(constraint)));
        }

        @Override
        public boolean hasBoundVariables() {
            return parent.hasBoundVariables();
        }

        @Override
        public Score scoreObject() {
            return parent.scoreObject();
        }

        @Override
        public Stream<Binding> bindings() {
            return parent.bindings();
        }

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            return parent.givenBindings();
        }

        @Override
        public List<Match> parentMatches() {
            return List.of(parent);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + range + "]";
        }

        @Override
        public String multilineFormat(String indent) {
            return simpleFormat() + indent + getClass().getSimpleName() + " << " + expression().representationString();
        }
    }

    record ConcatMatch(@Nonnull Expression expression, @Nonnull String fullString,
                       @Nonnull Match left, @Nonnull Match right,
                       @Nonnull RangeFlex matchRange) implements Match {
        public ConcatMatch {
            Objects.requireNonNull(fullString, "fullString");
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(matchRange, "range");
            if (left instanceof NoMatch || right instanceof NoMatch) {
                throw new IllegalArgumentException("Cannot combine matches:%n%s%n%s%n".formatted(left, right));
            }
        }


        @Override
        public Stats recursiveStats() {
            Stats leftStats = left.recursiveStats();
            Stats rightStats = right.recursiveStats();

            return new Stats(Math.max(leftStats.depth, rightStats.depth()) + 1, leftStats.depth() + rightStats.depth() + 1, leftStats.noMatches() + rightStats.noMatches());
        }


        @Override
        public Match constrain(Constraint constraint, Expression causeExpression) {
            return Match.tryConstrain(this, () ->
                    (constraint instanceof Constraint.MinLength minLength)
                            // minimum length does not apply to each side individually
                            ? new ConcatMatch(expression, fullString, left, right, matchRange.constrain(minLength))
                            // other constraints can be applied to each side individually
                            : new ConcatMatch(expression, fullString, left.constrain(constraint, causeExpression), right.constrain(constraint, causeExpression), matchRange.constrain(constraint))
            );
        }

        @Override
        public boolean hasBoundVariables() {
            return left.hasBoundVariables() || right.hasBoundVariables();
        }

        @Override
        public Score scoreObject() {
            return left.scoreObject().times(right.scoreObject(), "concatenated");
        }

        @Override
        public Stream<Binding> bindings() {
            List<Binding> bindings =
                    left.bindings()
                            .flatMap(leftBinding -> right.bindings()
                                    .map(rightBinding -> concat(leftBinding, rightBinding))
                                    .flatMap(Optional::stream)).toList();
            if (bindings.isEmpty()) {
                return Stream.of(Binding.empty(this));
            } else {
                return bindings.stream();
            }
        }

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            return Stream.concat(left.givenBindings(), right.givenBindings());
        }

        @Override
        public List<Match> parentMatches() {
            return List.of(left, right);
        }

        private Optional<Binding> concat(Binding leftBinding, Binding rightBinding) {
            return Result.ofTry(() -> RangeFlex.concat((leftBinding.matchRange()), rightBinding.matchRange()))
                    .ifSuccess()
                    .map(concatRange -> Binding.combine(this, concatRange.combined(), leftBinding, rightBinding));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + left.getClass().getSimpleName() + ", " + right.getClass().getSimpleName() + "]";
        }

        @Override
        public String multilineFormat(String indent) {
            String localContext = getClass().getSimpleName() + " << " + expression().representationString();
            String subIndent = spaced(indent) + " |-- AND ";
            return simpleFormat() + indent + localContext + System.lineSeparator() +
                   left.multilineFormat(subIndent) + System.lineSeparator()
                   + right.multilineFormat(subIndent);
        }
    }

    @Deprecated
    record ChoiceMatch(@Nonnull Expression expression, List<Match> matches) implements Match {

        public static Optional<Match> ofPossibleMatches(@Nonnull Expression expression, Stream<Match> matches) {
            List<Match> matchList = matches
                    .filter(not(NoMatch.class::isInstance))
                    .toList();
            return switch (matchList.size()) {
                case 0 -> Optional.empty();
                case 1 -> Optional.of(matchList.getFirst());
                default -> Optional.of(new ChoiceMatch(expression, matchList));
            };
        }

        @SuppressWarnings("StringEquality") // Identity match on purpose
        public ChoiceMatch {
            Objects.requireNonNull(matches, "matches");
            matches = List.copyOf(matches);
            if (matches.isEmpty()) {
                throw new IllegalArgumentException("Cannot create choice with zero matches");
            }
            String fullString = matches.getFirst().fullString();
            for (int i = 0; i < matches.size(); i++) {
                var idx = i;
                Match match = matches.get(i);
                Objects.requireNonNull(match, () -> "matches[" + idx + "]");
                if (match instanceof NoMatch) {
                    throw new IllegalArgumentException("Cannot create choice with no match at index %s: %s".formatted(i, match));
                }
                if (match.fullString() != fullString) {
                    throw new IllegalArgumentException("Cannot create choice from different sources at indexes 0 and %s :%n%s%n%s%n".formatted(i, fullString, match.fullString()));
                }
            }
        }

        @Override
        public Stats recursiveStats() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String fullString() {
            return matches.getFirst().fullString();
        }

        @Override
        public RangeFlex matchRange() {
            RangeFlex result = RangeFlex.expand(matches.stream().map(Match::matchRange).toList());
            Integer minLength = matches.stream().map(Match::matchRange).map(RangeFlex::minLength)
                    .reduce((opt1, opt2) -> opt1.flatMap(l1 -> opt2.map(l2 -> Math.min(l1, l2))))
                    .flatMap(o -> o)
                    .orElse(null);
            if (minLength != null) {
                result = result.constrain(Constraint.toMinLength(minLength));
            }

            Integer maxLength = matches.stream().map(Match::matchRange).map(RangeFlex::maxLength)
                    .reduce((opt1, opt2) -> opt1.flatMap(l1 -> opt2.map(l2 -> Math.max(l1, l2))))
                    .flatMap(o -> o)
                    .orElse(null);
            if (maxLength != null) {
                result = result.constrain(Constraint.toMaxLength(maxLength));
            }

            return result;
        }

        @Override
        public Match constrain(Constraint constraint, Expression causeExpression) {
            return Match.tryConstrain(this, () -> ChoiceMatch.ofPossibleMatches(expression(), matches().stream().map(m -> m.constrain(constraint, causeExpression)))
                    .orElse(new NoMatch(expression(), fullString(), "No choices left when applying constraint " + constraint, matches().stream().map(m -> m.constrain(constraint, causeExpression)).toList())));
        }

        @Override
        public boolean hasBoundVariables() {
            return matches.stream().anyMatch(Match::hasBoundVariables);
        }

        @Override
        public Stream<Binding> bindings() {
            return matches.stream()
                    .flatMap(match -> match.bindings())
                    .map(Binding::withNormalizedScore);
        }

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Score scoreObject() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Match> parentMatches() {
            return matches;
        }

        @Override
        public String multilineFormat(String indent) {
            String localContext = getClass().getSimpleName() + " << " + expression().representationString();
            String subIndent = spaced(indent) + " |-- OR ";
            return matches.stream()
                    .map(match -> match.multilineFormat(subIndent))
                    .collect(Collectors.joining(System.lineSeparator(),
                                                simpleFormat() + indent + localContext + System.lineSeparator(),
                                                ""));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[choices=" + matches.size() + "]";
        }
    }

    private static String spaced(String indent) {
        int lastPipe = indent.lastIndexOf('|');
        if (lastPipe < 0) {
            return indent;
        } else {
            return indent.substring(0, lastPipe + 1) + " ".repeat(indent.length() - lastPipe - 1);
        }
    }
}
