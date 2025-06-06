package com.quaxantis.etui.template.parser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public sealed interface Match {

    static Match of(String fullString) {
        return new RootMatch(fullString);
    }

    String fullString();

    String multilineFormat(String context);

    RangeFlex matchRange();

    Match constrain(Constraint constraint);

    private static Match tryConstrain(Match self, Supplier<Match> supplier) {
        try {
            return supplier.get();
        } catch (Exception exc) {
            return new NoMatch(self.fullString(), exc.toString(), self);
        }
    }

    boolean hasBoundVariables();

    boolean hasBindings();

    Stream<Binding> bindings();

    Optional<String> valueOf(String variable);

    default boolean isFullMatch() {
        RangeFlex range = matchRange();
        return range.contains(0) && range.contains(fullString().length() - 1);
    }

    default String matchedString() {
        return matchRange().extractFrom(fullString());
    }

    default String matchRepresentation() {
        return matchRange().applyTo(fullString()) + matchRange().lengthRange().transform((from, to) -> "!" + from + "<" + to);
    }

    default String simpleFormat() {
        return matchRange().format(fullString());
    }

    default Match binding(String boundVariable) {
        return new BindingMatch(this, boundVariable);
    }

    default Match andThen(UnaryOperator<Match> mapper) {
        return mapper.apply(this);
    }

    record NoMatch(@Nonnull String fullString, @Nullable String reason, List<Match> mismatches) implements Match {

        public NoMatch(@Nonnull String fullString, @Nullable String reason, Match... mismatches) {
            this(fullString, reason, List.of(mismatches));
        }

        @Override
        public RangeFlex matchRange() {
            return RangeFlex.empty();
//            throw new UnsupportedOperationException();
        }

        @Override
        public Match constrain(Constraint constraint) {
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
        public boolean hasBindings() {
            return false;
        }

        @Override
        public Stream<Binding> bindings() {
            return Stream.of(Binding.empty());
        }

        @Override
        public Optional<String> valueOf(String variable) {
            return Optional.empty();
        }

        @Override
        public String multilineFormat(String context) {
            StringBuilder builder = new StringBuilder()
                    .append(simpleFormat())
                    .append(" < no match < ")
                    .append(context);
            if (reason != null) {
                builder.append(" : because ").append(reason);
            }
            for (Match mismatch : mismatches) {
                builder.append(System.lineSeparator()).append(mismatch.multilineFormat(context));
            }
            return builder.toString();
        }
    }

    record RootMatch(@Nonnull String fullString) implements Match {

        @Override
        public RangeFlex matchRange() {
            return RangeFlex.ofComplete(fullString).constrain(Constraint.toMinLength(0));
        }

        @Override
        public String matchedString() {
            return fullString;
        }

        @Override
        public Match constrain(Constraint constraint) {
            return tryConstrain(this, () -> new PartialMatch(this, matchRange().constrain(constraint)));
        }

        @Override
        public boolean hasBoundVariables() {
            return false;
        }

        @Override
        public boolean hasBindings() {
            return false;
        }

        @Override
        public Stream<Binding> bindings() {
            return Stream.of(Binding.empty());
        }

        @Override
        public Optional<String> valueOf(String variable) {
            return Optional.empty();
        }

        @Override
        public String multilineFormat(String context) {
            return fullString + " < " + context;
        }
    }

    record BindingMatch(@Nonnull Match parent, @Nonnull String boundVariable) implements Match {
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
        public Match constrain(Constraint constraint) {
            return parent.constrain(constraint).andThen(parent -> new BindingMatch(parent, boundVariable));
        }

        @Override
        public boolean isFullMatch() {
            return parent.isFullMatch();
        }

        @Override
        public boolean hasBoundVariables() {
            return true;
        }

        @Override
        public boolean hasBindings() {
            return true;
        }

        @Override
        public Stream<Binding> bindings() {
            return parent.bindings().map(binding -> binding.with(boundVariable, matchedString()));
        }

        @Override
        public Optional<String> valueOf(String variable) {
            if (boundVariable.equals(variable)) {
                return Optional.of(matchedString());
            } else {
                return parent.valueOf(variable);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + boundVariable + "=" + matchRange().format(fullString()) + "]";
        }

        @Override
        public String simpleFormat() {
            return parent.simpleFormat();
        }

        @Override
        public String multilineFormat(String context) {
            return simpleFormat() + " < " + getClass().getSimpleName() + "[" + boundVariable + "]" + " < " + context;
        }
    }

    record PartialMatch(@Nonnull Match parent, @Nonnull RangeFlex range) implements Match {

        public PartialMatch {
            Objects.requireNonNull(parent, "parent");
            Objects.requireNonNull(range, "range");
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
        public Match constrain(Constraint constraint) {
            return Match.tryConstrain(this, () -> new PartialMatch(parent, range.constrain(constraint)));
        }

        @Override
        public boolean hasBoundVariables() {
            return parent.hasBoundVariables();
        }

        @Override
        public boolean hasBindings() {
            return parent.hasBindings();
        }

        @Override
        public Stream<Binding> bindings() {
            return parent.bindings();
        }

        @Override
        public Optional<String> valueOf(String variable) {
            return parent.valueOf(variable);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + simpleFormat() + "]";
        }

        @Override
        public String multilineFormat(String context) {
            return simpleFormat() + " < " + getClass().getSimpleName() + " < " + context;
        }
    }

    record ConcatMatch(@Nonnull Match left, @Nonnull Match right,
                       @Nonnull RangeFlex.Concat matchRange) implements Match {
        @SuppressWarnings("StringEquality") // Identity match on purpose
        public ConcatMatch {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(matchRange, "range");
            if (left instanceof NoMatch || right instanceof NoMatch) {
                throw new IllegalArgumentException("Cannot concat matches:%n%s%n%s%n".formatted(left, right));
            }
            if (left.fullString() != right.fullString()) {
                throw new IllegalArgumentException("Cannot concat matches from different sources :%n%s%n%s%n".formatted(left, right));
            }
        }

        @Override
        public String fullString() {
            return left.fullString();
        }

        @Override
        public Match constrain(Constraint constraint) {
            return Match.tryConstrain(this, () -> switch (constraint) {
                // minimum length does not apply to each side individually
                case Constraint.MinLength minLength -> new ConcatMatch(left, right, matchRange.constrain(minLength));
                // other constraints can be applied to each side individually
                default ->
                        new ConcatMatch(left.constrain(constraint), right.constrain(constraint), matchRange.constrain(constraint));
            });
        }

        @Override
        public boolean hasBoundVariables() {
            return left.hasBoundVariables() || right.hasBoundVariables();
        }

        @Override
        public boolean hasBindings() {
            return left.hasBindings() || right.hasBindings();
        }

        @Override
        public Stream<Binding> bindings() {
            return left.bindings()
                    .flatMap(leftBinding -> right.bindings().map(rightBinding -> Binding.combine(leftBinding, rightBinding)));
        }

        @Override
        public Optional<String> valueOf(String variable) {
            var leftOpt = left.valueOf(variable);
            var rightOpt = right.valueOf(variable);
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
            return getClass().getSimpleName() + "[" + simpleFormat() + " = " + left + " + " + right + "]";
        }

        @Override
        public String multilineFormat(String context) {
            String localContext = getClass().getSimpleName() + "(" + Integer.toHexString(System.identityHashCode(this)) + ")";
            return simpleFormat() + " < " + localContext + " < " + context + System.lineSeparator() +
                   left.multilineFormat(localContext) + System.lineSeparator()
                   + right.multilineFormat(localContext);
        }
    }

    record CombinedMatch(@Nonnull String fullString, @Nonnull Match left, @Nonnull Match right,
                         @Nonnull RangeFlex matchRange) implements Match {
        @SuppressWarnings("StringEquality") // Identity match on purpose
        public CombinedMatch {
            Objects.requireNonNull(fullString, "fullString");
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(matchRange, "range");
            if (left instanceof NoMatch || right instanceof NoMatch) {
                throw new IllegalArgumentException("Cannot combine matches:%n%s%n%s%n".formatted(left, right));
            }
        }

        @Override
        public Match constrain(Constraint constraint) {
            return Match.tryConstrain(this, () -> switch (constraint) {
                // minimum length does not apply to each side individually
                case Constraint.MinLength minLength ->
                        new CombinedMatch(fullString, left, right, matchRange.constrain(minLength));
                // other constraints can be applied to each side individually
                default ->
                        new CombinedMatch(fullString, left.constrain(constraint), right.constrain(constraint), matchRange.constrain(constraint));
            });
        }

        @Override
        public boolean hasBoundVariables() {
            return left.hasBoundVariables() || right.hasBoundVariables();
        }

        @Override
        public boolean hasBindings() {
            return left.hasBindings() || right.hasBindings();
        }

        @Override
        public Stream<Binding> bindings() {
            return left.bindings()
                    .flatMap(leftBinding -> right.bindings().map(rightBinding -> Binding.combine(leftBinding, rightBinding)));
        }

        @Override
        public Optional<String> valueOf(String variable) {
            var leftOpt = left.valueOf(variable);
            var rightOpt = right.valueOf(variable);
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
            return getClass().getSimpleName() + "[" + simpleFormat() + " = " + left + " + " + right + "]";
        }

        @Override
        public String multilineFormat(String context) {
            String localContext = getClass().getSimpleName() + "(" + Integer.toHexString(System.identityHashCode(this)) + ")";
            return simpleFormat() + " < " + localContext + " < " + context + System.lineSeparator() +
                   left.multilineFormat(localContext) + System.lineSeparator()
                   + right.multilineFormat(localContext);
        }
    }


    record ChoiceMatch(List<Match> matches) implements Match {
        public static Optional<Match> ofPossibleMatches(Match... matches) {
            return ofPossibleMatches(Arrays.stream(matches));
        }

        public static Optional<Match> ofPossibleMatches(Stream<Match> matches) {
            List<Match> matchList = matches
                    .filter(not(NoMatch.class::isInstance))
                    .toList();
            return switch (matchList.size()) {
                case 0 -> Optional.empty();
                case 1 -> Optional.of(matchList.getFirst());
                default -> Optional.of(new ChoiceMatch(matchList));
            };
        }

        public ChoiceMatch(Match... matches) {
            this(List.of(matches));
        }

        @SuppressWarnings("StringEquality") // Identity match on purpose
        public ChoiceMatch {
            Objects.requireNonNull(matches, "matches");
            matches = List.copyOf(matches);
            if (matches.isEmpty()) {
                throw new IllegalArgumentException("Cannot create choice with zero matches");
            }
            String fullString = matches.get(0).fullString();
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
        public String fullString() {
            return matches.get(0).fullString();
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
        public Match constrain(Constraint constraint) {
            return Match.tryConstrain(this, () -> ChoiceMatch.ofPossibleMatches(matches().stream().map(m -> m.constrain(constraint)))
                    .orElse(new NoMatch(fullString(), "No choices left when applying constraint " + constraint, matches().stream().map(m -> m.constrain(constraint)).toList())));
        }

        @Override
        public boolean hasBoundVariables() {
            return matches.stream().anyMatch(Match::hasBoundVariables);
        }

        @Override
        public boolean hasBindings() {
            return matches.stream().anyMatch(Match::hasBindings);
        }

        @Override
        public Stream<Binding> bindings() {
            return matches.stream().flatMap(Match::bindings);
        }

        @Override
        public Optional<String> valueOf(String variable) {
            List<Binding> bindings = bindings().toList();
            if (bindings.size() == 1) {
                return bindings.getFirst().valueOf(variable);
            } else if (bindings.isEmpty()) {
                return Optional.empty();
            } else {
                throw new IllegalStateException("Multiple bindings");
            }
        }

        @Override
        public String multilineFormat(String context) {
            String localContext = getClass().getSimpleName() + "(" + Integer.toHexString(System.identityHashCode(this)) + ")";
            return matches.stream()
                    .map(match -> match.multilineFormat(localContext))
                    .collect(Collectors.joining(System.lineSeparator(),
                                                simpleFormat() + " < " + localContext + " < " + context + System.lineSeparator(),
                                                ""));
        }

    }
}
