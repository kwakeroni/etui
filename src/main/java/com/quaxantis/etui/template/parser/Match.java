package com.quaxantis.etui.template.parser;

import com.quaxantis.support.util.Result;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public sealed interface Match {

    static Match of(@Nonnull Expression expression, @Nonnull String fullString) {
        return new RootMatch(expression, fullString);
    }

    Expression expression();
    String fullString();

    default String multilineFormat() {
        return multilineFormat("  ");
    }

    String multilineFormat(String indent);

    RangeFlex matchRange();

    Match constrain(Constraint constraint);

    private static Match tryConstrain(Match self, Supplier<Match> supplier) {
        try {
            return supplier.get();
        } catch (Exception exc) {
            return new NoMatch(self.expression(), self.fullString(), exc.toString(), self);
        }
    }

    boolean hasBoundVariables();

    Stream<Binding> bindings();

    default boolean isFullMatch() {
        int length = fullString().length();
        RangeFlex range = matchRange();
        return range.contains(0) && range.contains(length - 1)
               && range.maxLength().map(max -> max >= length).orElse(true);
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

    default Match binding(@Nonnull Expression expression, String boundVariable) {
        return new BindingMatch(expression, this, boundVariable);
    }

    default Match andThen(UnaryOperator<Match> mapper) {
        return mapper.apply(this);
    }

    record NoMatch(@Nonnull Expression expression, @Nonnull String fullString, @Nullable String reason,
                   Collection<Match> mismatches) implements Match {

        public NoMatch(@Nonnull Expression expression, @Nonnull String fullString, @Nullable String reason, Match... mismatches) {
            this(expression, fullString, reason, List.of(mismatches));
        }

        @Override
        public RangeFlex matchRange() {
            return RangeFlex.empty();
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
        public Stream<Binding> bindings() {
            return Stream.of(Binding.empty(this, this.matchRange()));
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
            ;
            for (Match mismatch : mismatches) {
                builder.append(System.lineSeparator()).append(mismatch.multilineFormat(subIndent));
            }
            return builder.toString();
        }
    }

    record RootMatch(@Nonnull Expression expression, @Nonnull String fullString) implements Match {

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
            return tryConstrain(this, () -> new PartialMatch(expression, this, matchRange().constrain(constraint)));
        }

        @Override
        public boolean hasBoundVariables() {
            return false;
        }

        @Override
        public Stream<Binding> bindings() {
            return Stream.of(Binding.empty(this, this.matchRange()));
        }

        @Override
        public String multilineFormat(String indent) {
            return fullString;
        }
    }

    record BindingMatch(@Nonnull Expression expression, @Nonnull Match parent,
                        @Nonnull String boundVariable) implements Match {
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
            return parent.constrain(constraint).andThen(parent -> new BindingMatch(expression, parent, boundVariable));
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
        public Stream<Binding> bindings() {
            String format = simpleFormat();
            return parent.bindings().map(binding -> binding.with(this, this.matchRange(), boundVariable, matchedString(), format));
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
        public String multilineFormat(String indent) {
            return simpleFormat() + indent + getClass().getSimpleName() + "[" + boundVariable + "] " + matchRange().lengthRange() + " << " + expression().representationString();
        }
    }

    record PartialMatch(@Nonnull Expression expression, @Nonnull Match parent,
                        @Nonnull RangeFlex range) implements Match {

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
            return Match.tryConstrain(this, () -> new PartialMatch(expression, parent, range.constrain(constraint)));
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
        public String toString() {
            return getClass().getSimpleName() + "[" + simpleFormat() + "]";
        }

        @Override
        public String multilineFormat(String indent) {
            return simpleFormat() + indent + getClass().getSimpleName() + " << " + expression().representationString();
        }
    }

    record ConcatMatch(@Nonnull Expression expression, @Nonnull String fullString,
                       @Nonnull Match left, @Nonnull Match right,
                       @Nonnull RangeFlex matchRange) implements Match {
        @SuppressWarnings("StringEquality") // Identity match on purpose
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
        public Match constrain(Constraint constraint) {
            return Match.tryConstrain(this, () -> switch (constraint) {
                // minimum length does not apply to each side individually
                case Constraint.MinLength minLength ->
                        new ConcatMatch(expression, fullString, left, right, matchRange.constrain(minLength));
                // other constraints can be applied to each side individually
                default ->
                        new ConcatMatch(expression, fullString, left.constrain(constraint), right.constrain(constraint), matchRange.constrain(constraint));
            });
        }

        @Override
        public boolean hasBoundVariables() {
            return left.hasBoundVariables() || right.hasBoundVariables();
        }

        @Override
        public Stream<Binding> bindings() {
            return left.bindings()
                    .flatMap(leftBinding -> right.bindings()
                            .map(rightBinding -> concat(leftBinding, rightBinding))
                            .flatMap(Optional::stream));
        }

        private Optional<Binding> concat(Binding leftBinding, Binding rightBinding) {
            return Result.ofTry(() -> RangeFlex.concat((leftBinding.match().matchRange()), rightBinding.match().matchRange()))
                    .ifSuccess()
                    .map(concatRange -> Binding.combine(this, concatRange.combined(), leftBinding, rightBinding));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + simpleFormat() + " = " + left + " + " + right + "]";
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


    record ChoiceMatch(@Nonnull Expression expression, List<Match> matches) implements Match {
        public static Optional<Match> ofPossibleMatches(@Nonnull Expression expression, Match... matches) {
            return ofPossibleMatches(expression, Arrays.stream(matches));
        }

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

        public ChoiceMatch(@Nonnull Expression expression, Match... matches) {
            this(expression, List.of(matches));
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
            return Match.tryConstrain(this, () -> ChoiceMatch.ofPossibleMatches(expression(), matches().stream().map(m -> m.constrain(constraint)))
                    .orElse(new NoMatch(expression(), fullString(), "No choices left when applying constraint " + constraint, matches().stream().map(m -> m.constrain(constraint)).toList())));
        }

        @Override
        public boolean hasBoundVariables() {
            return matches.stream().anyMatch(Match::hasBoundVariables);
        }

        @Override
        public Stream<Binding> bindings() {
            return matches.stream().flatMap(Match::bindings);
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
