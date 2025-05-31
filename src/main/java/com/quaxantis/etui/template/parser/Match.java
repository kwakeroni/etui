package com.quaxantis.etui.template.parser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public sealed interface Match {

    String fullString();

    String matchedString();

    String multilineFormat(String context);

    String matchRepresentation();

    RangeFlex matchRange();

    Match constrain(RangeFlex constraint);

    default String simpleFormat() {
        return matchRange().format(fullString());
    }

    default boolean isFullMatch() {
        RangeFlex range = matchRange();
        return range.contains(0) && range.contains(fullString().length() - 1);
    }

    boolean hasBoundVariables();

    Optional<String> valueOf(String variable);

    record NoMatch(@Nonnull String fullString, @Nullable String reason, List<Match> mismatches) implements Match {

        public NoMatch(@Nonnull String fullString, @Nullable String reason, Match... mismatches) {
            this(fullString, reason, List.of(mismatches));
        }

        @Override
        public String matchedString() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String matchRepresentation() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RangeFlex matchRange() {
            return RangeFlex.empty();
        }

        @Override
        public Match constrain(RangeFlex constraint) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasBoundVariables() {
            return false;
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
        public String matchRepresentation() {
            return fullString();
        }

        @Override
        public RangeFlex matchRange() {
            return RangeFlex.ofCompleteFixed(fullString);
        }

        @Override
        public String matchedString() {
            return fullString;
        }

        @Override
        public Match constrain(RangeFlex constraint) {
            return new PartialMatch(this, constraint);
        }

        @Override
        public boolean hasBoundVariables() {
            return false;
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
        public Match constrain(RangeFlex constraint) {
            return new BindingMatch(parent.constrain(constraint), boundVariable);
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

        public String matchRepresentation() {
            return range.applyTo(fullString());
        }

        @Override
        public RangeFlex matchRange() {
            return range;
        }

        @Override
        public String matchedString() {
            return range.extractFrom(fullString());
        }

        @Override
        public Match constrain(RangeFlex constraint) {
            return new PartialMatch(parent, range.constrain(constraint));
        }

        @Override
        public boolean hasBoundVariables() {
            return parent.hasBoundVariables();
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
        public String matchedString() {
            return matchRange.extractFrom(fullString());
        }

        @Override
        public Match constrain(RangeFlex constraint) {
            return new ConcatMatch(left.constrain(constraint), right.constrain(constraint), matchRange.constrain(constraint));
        }

        @Override
        public String matchRepresentation() {
            return matchRange.applyTo(fullString());
        }

        @Override
        public boolean hasBoundVariables() {
            return left.hasBoundVariables() || right.hasBoundVariables();
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
}
