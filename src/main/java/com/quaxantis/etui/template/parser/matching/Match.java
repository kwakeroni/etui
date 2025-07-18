package com.quaxantis.etui.template.parser.matching;

import com.quaxantis.etui.template.parser.eel.Expression;
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

import static com.quaxantis.etui.template.parser.matching.Constraint.toRange;
import static java.util.stream.Collectors.*;

public sealed abstract class Match {

    public static @Nonnull Match withGivenBindings(@Nonnull Expression expression, @Nonnull String fullString, @Nonnull Map<String, String> bindings) {
        Map<String, RangeFlex.Applied> givenBindings = bindings.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> RangeFlex.Applied.ofCompleteFixed(entry.getValue())));
        return new RootMatch(fullString, expression, givenBindings);
    }

    public static @Nonnull NoMatch noMatch(@Nonnull Expression expression, @Nonnull String fullString, Match... mismatches) {
        return noMatch(null, expression, fullString, mismatches);
    }

    public static @Nonnull NoMatch noMatch(@Nullable String reason, @Nonnull Expression expression, @Nonnull String fullString, Match... mismatches) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(fullString, "fullString");
        return new NoMatch(expression, fullString, reason, mismatches);
    }

    abstract String fullString();

    abstract Expression expression();

    abstract List<Match> parentMatches();

    abstract Stats recursiveStats();

    public abstract RangeFlex matchRange();

    public RangeFlex.Applied appliedRange() {
        return matchRange().applyTo(fullString());
    }

    public boolean isFullMatch() {
        int length = fullString().length();
        RangeFlex range = matchRange();
        return range.contains(0) && range.contains(length - 1)
               && range.maxLength().map(max -> max >= length).orElse(true);
    }

    String matchRepresentation() {
        return matchRange().applyTo(fullString()) + matchRange().lengthRange().transform((from, to) -> "!" + from + "<" + to);
    }

    String simpleFormat() {
        return matchRange().format(fullString());
    }

    public abstract Match constrain(Constraint constraint, Expression causeExpression);

    protected Match tryConstrain(Supplier<Match> supplier) {
        try {
            return supplier.get();
        } catch (Exception exc) {
            return notMatchingBecause(exc.toString());
        }
    }

    abstract Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings();

    abstract boolean hasBoundVariables();

    public Stream<Binding> bindings() {
        return Stream.of(Binding.empty(this));
    }

    public double score() {
        return scoreObject().value();
    }

    public abstract Score scoreObject();

    @Nonnull
    public Match binding(@Nonnull Expression expression, @Nonnull String boundVariable) {
        return new BindingMatch(this, expression, boundVariable);
    }

    @Nonnull
    public Match bindingAll(@Nonnull Expression causeExpression, @Nonnull List<Binding> bindings) {
        Supplier<Stream<Map.Entry<String, RangeFlex.Applied>>> givenBindings = () ->
                bindings.stream().flatMap(
                        binding -> binding.boundVariables()
                                .map(var -> binding.valueRangeOf(var).map(range -> Map.entry(var, range)))
                                .flatMap(Optional::stream)
                );

        // TODO Check that this is no longer possible after introducing "delegated binding"
        givenBindings = givenBindings.get().toList()::stream;

        return new AddGivenBindings(this, causeExpression, givenBindings);
    }

    @Nonnull
    public NoMatch notMatchingBecause(@Nonnull String reason) {
        return new NoMatch(this.expression(), this.fullString(), reason, this);
    }

    @Nonnull
    public Match concatenate(Match suffix, Expression expression, String fullString) {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(fullString, "fullString");
        return ConcatMatch.concatenating(this, suffix, expression, fullString);
    }

    @Nonnull
    public Match andThen(@Nonnull UnaryOperator<Match> mapper) {
        return mapper.apply(this);
    }

    private sealed abstract static class BindingLessMatch extends Match {

        @Nonnull
        protected final String fullString;
        @Nonnull
        protected final Expression expression;

        public BindingLessMatch(@Nonnull String fullString, @Nonnull Expression expression) {
            this.fullString = fullString;
            this.expression = expression;
        }

        @Override
        @Nonnull
        public String fullString() {
            return fullString;
        }

        @Override
        @Nonnull
        public Expression expression() {
            return expression;
        }

        public List<Match> parentMatches() {
            return List.of();
        }

        @Override
        public boolean hasBoundVariables() {
            return false;
        }

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            return Stream.empty();
        }
    }

    public static final class NoMatch extends BindingLessMatch {
        @Nullable
        private final String reason;
        private final Collection<Match> mismatches;

        private NoMatch(@Nonnull Expression expression, @Nonnull String fullString, @Nullable String reason, Match... mismatches) {
            this(expression, fullString, reason, List.of(mismatches));
        }

        private NoMatch(@Nonnull Expression expression, @Nonnull String fullString, @Nullable String reason, Collection<Match> mismatches) {
            super(fullString, expression);
            this.reason = reason;
            this.mismatches = mismatches;
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
        public Score scoreObject() {
            return Score.of(0.0, "no match");
        }

        @Nonnull
        @Override
        public Match andThen(@Nonnull UnaryOperator<Match> mapper) {
            return this;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + reason + "]";
        }
    }

    private static final class RootMatch extends BindingLessMatch {
        @Nonnull
        private final Map<String, RangeFlex.Applied> givenBindingsMap;

        private RootMatch(@Nonnull String fullString, @Nonnull Expression expression,
                          @Nonnull Map<String, RangeFlex.Applied> givenBindingsMap) {
            super(fullString, expression);
            this.givenBindingsMap = givenBindingsMap;
        }

        @Override
        public Stats recursiveStats() {
            return new Stats(1, 1, 0);
        }

        @Override
        public RangeFlex matchRange() {
            return RangeFlex.ofComplete(fullString).constrain(Constraint.toMinLength(0));
        }

        @Override
        public Match constrain(Constraint constraint, Expression causeExpression) {
            return tryConstrain(() -> new PartialMatch(this, causeExpression, matchRange().constrain(constraint)));
        }

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            return givenBindingsMap.entrySet().stream();
        }

        @Override
        public Score scoreObject() {
            return Score.base("no bindings");
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[given=" + givenBindings().count() + ", string=" + fullString + "]";
        }
    }

    private sealed abstract static class MatchAdapter extends Match {
        @Nonnull
        protected final Expression expression;
        @Nonnull
        private final Match parent;

        protected MatchAdapter(@Nonnull Match parent, @Nonnull Expression expression) {
            Objects.requireNonNull(parent, "parent");
            this.parent = parent;
            this.expression = expression;
        }

        @Override
        public String fullString() {
            return parent.fullString();
        }

        @Override
        @Nonnull
        public Expression expression() {
            return expression;
        }

        @Override
        public List<Match> parentMatches() {
            return List.of(parent);
        }

        @Override
        public Stats recursiveStats() {
            Stats parentStats = parent.recursiveStats();
            return new Stats(parentStats.depth() + 1, parentStats.size() + 1, parentStats.noMatches());
        }

        @Override
        public RangeFlex matchRange() {
            return parent.matchRange();
        }

        @Override
        public Match constrain(Constraint constraint, Expression causeExpression) {
            return parent.constrain(constraint, causeExpression)
                    .andThen(newParent -> withNewParent(newParent, causeExpression));
        }

        protected abstract Match withNewParent(Match newParent, Expression newExpression);

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            return parent.givenBindings();
        }

        @Override
        public boolean hasBoundVariables() {
            return parent.hasBoundVariables();
        }

        @Override
        public Stream<Binding> bindings() {
            return parent.bindings();
        }

        @Override
        public Score scoreObject() {
            return parent.scoreObject();
        }
    }

    private static final class BindingMatch extends MatchAdapter {

        @Nonnull
        private final String boundVariable;

        private BindingMatch(@Nonnull Match parent, @Nonnull Expression expression, @Nonnull String boundVariable) {
            super(parent, expression);
            this.boundVariable = boundVariable;
            verifyNoConflictingBindings(parent, boundVariable);
        }

        private static void verifyNoConflictingBindings(Match parent, String boundVariable) {
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
        protected BindingMatch withNewParent(Match newParent, Expression newExpression) {
            return new BindingMatch(newParent, newExpression, boundVariable);
        }

        @Override
        public boolean hasBoundVariables() {
            return true;
        }

        @Override
        public Stream<Binding> bindings() {
            String format = simpleFormat();
            return super.bindings().map(binding -> binding.with(this, boundVariable, format));
        }

        @Override
        public Score scoreObject() {
            Score boundVariableScore = boundVariableScore();
            return super.scoreObject().times(boundVariableScore, boundVariableScore.reason());
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
                return RangeFlex.Applied.merge(bound, given).map(_ -> Score.of(.9, "merged with given value: " + given.format()))
                        .orElseGet(() -> Score.of(0.2, "different from given value: " + given.format()));
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + boundVariable + "=\"" + appliedRange().extractMax() + "\"]";
        }
    }

    private static final class AddGivenBindings extends MatchAdapter {

        @Nonnull
        private final Supplier<Stream<Map.Entry<String, RangeFlex.Applied>>> addedGivenBindings;

        private AddGivenBindings(@Nonnull Match parent, @Nonnull Expression expression,
                                 @Nonnull Supplier<Stream<Map.Entry<String, RangeFlex.Applied>>> addedGivenBindings) {
            super(parent, expression);
            this.addedGivenBindings = addedGivenBindings;
        }

        @Override
        protected AddGivenBindings withNewParent(Match newParent, Expression newExpression) {
            return new AddGivenBindings(newParent, newExpression, addedGivenBindings);
        }

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            return Stream.concat(super.givenBindings(), addedGivenBindings.get());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[given=" + addedGivenBindings.get().map(Map.Entry::getKey).collect(joining(", ")) + "]";
        }
    }

    private static final class PartialMatch extends MatchAdapter {
        @Nonnull
        private final RangeFlex range;

        private PartialMatch(@Nonnull Match parent, @Nonnull Expression expression, @Nonnull RangeFlex range) {
            super(parent, expression);
            Objects.requireNonNull(parent, "parent");
            Objects.requireNonNull(range, "range");
            this.range = range;
        }

        @Override
        public RangeFlex matchRange() {
            return range;
        }

        @Override
        public Match constrain(Constraint constraint, Expression causeExpression) {
            return tryConstrain(() -> new PartialMatch(this, causeExpression, range.constrain(constraint)));
        }

        @Override
        protected PartialMatch withNewParent(Match newParent, Expression newExpression) {
            return new PartialMatch(newParent, newExpression, range);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + range + "]";
        }
    }

    private static final class ConcatMatch extends Match {
        @Nonnull
        private final String fullString;
        @Nonnull
        private final Expression expression;
        @Nonnull
        private final Match left;
        @Nonnull
        private final Match right;
        @Nonnull
        private final RangeFlex matchRange;

        private ConcatMatch(@Nonnull Expression expression, @Nonnull String fullString, @Nonnull Match left, @Nonnull Match right, @Nonnull RangeFlex matchRange) {
            Objects.requireNonNull(fullString, "fullString");
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(matchRange, "range");
            if (left instanceof NoMatch || right instanceof NoMatch) {
                throw new IllegalArgumentException("Cannot combine matches:%n%s%n%s%n".formatted(left, right));
            }
            this.expression = expression;
            this.fullString = fullString;
            this.left = left;
            this.right = right;
            this.matchRange = matchRange;
        }

        private static Match concatenating(Match leftMatch, Match rightMatch, Expression expression, String fullString) {
            if (leftMatch instanceof NoMatch || rightMatch instanceof NoMatch) {
                return new NoMatch(expression, fullString, null, leftMatch, rightMatch);
            } else {
                return switch (RangeFlex.tryConcat(leftMatch.matchRange(), rightMatch.matchRange())) {
                    case Result.Success(RangeFlex.ConcatResult(var left, var right, var combined)) -> {
                        var constrainedLeftMatch = leftMatch.constrain(toRange(left), expression);
                        var constrainedRightMatch = rightMatch.constrain(toRange(right), expression);
                        if (constrainedLeftMatch instanceof NoMatch || constrainedRightMatch instanceof NoMatch) {
                            yield new NoMatch(expression, fullString, "Cannot concatenate constrainted ranges", constrainedLeftMatch, constrainedRightMatch);
                        }
                        yield new ConcatMatch(expression, fullString, constrainedLeftMatch, constrainedRightMatch, combined);
                    }
                    case Result.Failure(var exc) -> {
                        String reason = exc + " at " + exc.getStackTrace()[0];
                        yield new NoMatch(expression, fullString, reason, leftMatch, rightMatch);
                    }
                };
            }
        }

        @Override
        @Nonnull
        public Expression expression() {
            return expression;
        }

        @Override
        @Nonnull
        public String fullString() {
            return fullString;
        }

        @Override
        public List<Match> parentMatches() {
            return List.of(left, right);
        }

        @Override
        public Stats recursiveStats() {
            Stats leftStats = left.recursiveStats();
            Stats rightStats = right.recursiveStats();
            return new Stats(Math.max(leftStats.depth, rightStats.depth()) + 1, leftStats.depth() + rightStats.depth() + 1, leftStats.noMatches() + rightStats.noMatches());
        }

        @Override
        @Nonnull
        public RangeFlex matchRange() {
            return matchRange;
        }

        @Override
        public Match constrain(Constraint constraint, Expression causeExpression) {
            return tryConstrain(() -> (constraint instanceof Constraint.MinLength minLength)
                    // minimum length does not apply to each side individually
                    ? new ConcatMatch(expression, fullString, left, right, matchRange.constrain(minLength))
                    // other constraints can be applied to each side individually
                    : new ConcatMatch(expression, fullString, left.constrain(constraint, causeExpression), right.constrain(constraint, causeExpression), matchRange.constrain(constraint))
            );
        }

        @Override
        public Stream<Map.Entry<String, RangeFlex.Applied>> givenBindings() {
            return Stream.concat(left.givenBindings(), right.givenBindings());
        }

        @Override
        public boolean hasBoundVariables() {
            return left.hasBoundVariables() || right.hasBoundVariables();
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

        private Optional<Binding> concat(Binding leftBinding, Binding rightBinding) {
            return Result.ofTry(() -> RangeFlex.concat((leftBinding.matchRange()), rightBinding.matchRange()))
                    .ifSuccess()
                    .map(concatRange -> Binding.combine(this, concatRange.combined(), leftBinding, rightBinding));
        }

        @Override
        public Score scoreObject() {
            return left.scoreObject().times(right.scoreObject(), "concatenated");
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + left.getClass().getSimpleName() + ", " + right.getClass().getSimpleName() + "]";
        }
    }

    public interface Score {
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

}
