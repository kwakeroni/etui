package com.quaxantis.etui.template.parser;

import com.quaxantis.support.util.Result;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

sealed interface RangeFlex {

    static RangeFlex empty() {
        return ofFixed(0, -1);
    }

    static RangeFlex of(int leftFrom, int leftToIncl, int rightFrom, int rightToIncl) {
        return of(IntRange.ofClosed(leftFrom, leftToIncl), IntRange.ofClosed(rightFrom, rightToIncl));
    }

    static RangeFlex of(IntRange left, IntRange right) {
        return new Simple(left, right);
    }

    static RangeFlex ofFixedStart(int from, int rightFrom, int rightToIncl) {
        return of(IntRange.of(from, from), IntRange.ofClosed(rightFrom, rightToIncl));
    }

    static RangeFlex ofFixedEnd(int leftFrom, int leftToIncl, int to) {
        return of(IntRange.ofClosed(leftFrom, leftToIncl), IntRange.of(to + 1, to + 1));
    }

    static RangeFlex ofFixed(int from, int to) {
        return of(IntRange.of(from, from), IntRange.of(to + 1, to + 1));
    }

    static RangeFlex ofComplete(String string) {
        IntRange completeRange = IntRange.of(0, string.length());
        return of(completeRange, completeRange);
    }

    static RangeFlex ofCompleteFixed(String string) {
        return ofFixed(0, string.length() - 1);
    }

    static RangeFlex expand(Iterable<? extends RangeFlex> ranges) {
        Iterator<? extends RangeFlex> iter = ranges.iterator();
        if (!iter.hasNext()) {
            throw new IllegalArgumentException("ranges cannot be empty");
        }
        RangeFlex result = iter.next();
        while (iter.hasNext()) {
            result = expand(result, iter.next());
        }
        return result;
    }

    static RangeFlex expand(RangeFlex range1, RangeFlex range2) {

        var first = Math.min(range1.start().from(), range2.start().from());
        var lastExcl = Math.max(range1.end().exclusiveTo(), range2.end().exclusiveTo());

        var prefixToExcl = lastExcl;
        var suffixFrom = first;

        var optMain1 = range1.main();
        var optMain2 = range2.main();
        if (optMain1.isPresent() && optMain2.isPresent()) {
            var main1 = optMain1.get();
            var main2 = optMain2.get();
            if (main1.contains(main2.from()) || main2.contains(main1.from())) {
                prefixToExcl = Math.max(main1.from(), main2.from());
                suffixFrom = Math.min(main1.exclusiveTo(), main2.exclusiveTo());
            }
        }

        IntRange prefix = IntRange.of(first, prefixToExcl);
        IntRange suffix = IntRange.of(suffixFrom, lastExcl);

        return RangeFlex.of(prefix, suffix);
    }

    record ConcatResult(RangeFlex left, RangeFlex right, RangeFlex combined) {}

    static Result<ConcatResult, RuntimeException> tryConcat(RangeFlex left, RangeFlex right) {
        return Result.ofTry(() -> concat(left, right));
    }

    static ConcatResult concat(RangeFlex left, RangeFlex right) {
        var leftConstrained = left.tryConstrain(Constraint.succeedBy(right))
                .orElseThrow(exc -> new IllegalArgumentException("Cannot concatenate flexes %s and %s: %s".formatted(left, right, exc.getMessage()), exc));
        var rightConstrained = right.constrain(Constraint.precedeBy(left));
        var minLength = leftConstrained.minLength().map(minLeft -> minLeft + rightConstrained.minLength().orElse(0))
                .orElse(rightConstrained.minLength().orElse(null));

        var maxLength = leftConstrained.maxLength().flatMap(maxLeft -> rightConstrained.maxLength().map(maxRight -> maxLeft + maxRight))
                .orElse(null);

        RangeFlex combined = new RangeFlex.Simple(leftConstrained.start(), rightConstrained.end(), minLength, maxLength);
        return new ConcatResult(leftConstrained, rightConstrained, combined);
    }

    IntRange start();

    IntRange end();

    Optional<Integer> minLength();

    Optional<Integer> maxLength();

    RangeFlex constrain(Constraint constraint);

    default Result<? extends RangeFlex, RuntimeException> tryConstrain(Constraint constraint) {
        return Result.ofTry(() -> constrain(constraint));
    }

    default Optional<IntRange> main() {
        if (start().to() < end().from()) {
            return Optional.of(IntRange.of(start().exclusiveTo(), end().from()));
        } else {
            return Optional.empty();
        }
    }

    default boolean contains(int i) {
        return i >= start().from() && i < end().exclusiveTo();
    }

    default IntRange lengthRange() {

        int minRange = end().from() - start().exclusiveTo();
        int maxRange = end().exclusiveTo() - start().from();

        int minimum = this.minLength().map(min -> Math.max(min, minRange)).orElse(minRange);
        int maximum = this.maxLength().map(max -> Math.min(max, maxRange)).orElse(maxRange);
        return IntRange.ofClosed(minimum, maximum);
    }

    default boolean isEmpty() {
        IntRange lengthRange = lengthRange();
        return lengthRange.from() == lengthRange.to();
    }


    default String extractFrom(String string) {
        // TODO what about fuzzy strings and other length constraints ?

        if (maxLength().filter(max -> max == 0).isPresent()) {
            return "";
        } else if (start().from() <= end().exclusiveTo()) {
            return string.substring(start().from(), end().exclusiveTo());
        } else {
            throw new IllegalStateException("Cannot extract valid string from range: " + applyTo(string));
        }
    }

    default String applyTo(String string) {
        return format(string, RangeFlexFormatter.SEPARATORS_ONLY);
    }

    default String format(String string) {
        return format(string, RangeFlexFormatter.defaultFormatter());
    }

    default String format(String string, RangeFlexFormatter formatter) {
        return formatter.format(this, string);
    }

    private static String toString(IntRange start, IntRange end) {
        return toString(start, end, null, null);
    }

    private static String toString(IntRange start, IntRange end, Integer minLength, Integer maxLength) {
        double log10 = Math.log10(Math.max(start.to(), end.to()));
        int d = Double.isFinite(log10) ? ((int) log10) + 1 : -1;
        String rangePattern = (d > 0) ? "%" + d + "d-%" + d + "d" : "%s..%s";
        String singletonPattern = (d > 0) ? "%" + (d * 2 + 1) + "d" : "%s";
        String empty = (d > 0) ? " ".repeat(d * 2 + 1) : "";
        String leftString = (start.from() == start.to() ? singletonPattern : (start.from() < start.to()) ? rangePattern : empty)
                .formatted(start.from(), start.to());
        String midString = ((start.to() + 1 == end.from() - 1) ? singletonPattern : (start.to() + 1 < end.from() - 1) ? rangePattern : empty).formatted(start.to() + 1, end.from() - 1);
        String midSpace = (midString.isEmpty()) ? "" : " ";
        String rightString = (end.from() == end.to() ? singletonPattern : (end.from() < end.to()) ? rangePattern : empty).formatted(end.from(), end.to());
        String minLengthString = (minLength == null && maxLength == null) ? "" : ("!" + Objects.toString(minLength, "") + "<" + Objects.toString(maxLength, ""));
        return "{[" + leftString + "]" + midSpace + midString + midSpace + "[" + rightString + "]" + minLengthString + "}";
    }

    private static <R> R verify(R range, Consumer<? super R> verification) {
        verification.accept(range);
        return range;
    }


    record Simple(IntRange start, IntRange end, Integer minLengthValue, Integer maxLengthValue) implements RangeFlex {

        public Simple {
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            verifyLengthConstraints(start, end, minLengthValue, maxLengthValue).orElseThrow();
        }

        Simple(IntRange start, IntRange end) {
            this(start, end, null, null);
        }

        @Override
        public Optional<Integer> minLength() {
            return Optional.ofNullable(minLengthValue);
        }

        @Override
        public Optional<Integer> maxLength() {
            return Optional.ofNullable(maxLengthValue);
        }

        public RangeFlex constrain(Constraint constraint) {
            return switch (constraint) {
                case Constraint.MinLength(int min) -> constrainMinLength(min);
                case Constraint.MaxLength(int max) -> constrainMaxLength(max);
                case Constraint.LimitLeft(int left) -> limitLeft(left);
                case Constraint.LimitRight(int right) -> limitRight(right);
                case Constraint.RangeLimit(RangeFlex range) -> constrainToRange(range);
                case Constraint.FixedRange _ -> toFixedRange();
                case Constraint.FixPrefix _ -> fixPrefix();
                case Constraint.FixSuffix _ -> fixSuffix();
                case Constraint.PrecedeBy(var predecessor) -> constrainWithPredecessor(predecessor);
                case Constraint.SucceedBy(var successor) -> constrainWithSuccessor(successor);
                case Constraint.Satisfying(var verification) -> verify(this, verification);
                case Constraint.And(var one, var two) -> constrain(one).constrain(two);
            };
        }

        private RangeFlex toFixedRange() {
            int from = start().from();
            int to = end.to();
            return new Simple(IntRange.of(from, from), IntRange.of(to + 1, to + 1));
        }

        private RangeFlex fixPrefix() {
            int from = start.from();
            return new Simple(IntRange.of(from, from), end);
        }

        private RangeFlex fixSuffix() {
            int to = end.to();
            return new Simple(start, IntRange.of(to + 1, to + 1));
        }

        private RangeFlex constrainMinLength(Integer minLength) {
            return new Simple(start, end, (this.minLengthValue == null) ? minLength : Math.max(this.minLengthValue, minLength), this.maxLengthValue);
        }

        private Simple constrainMaxLength(int maxLength) {
            return new Simple(start, end, this.minLengthValue, (this.maxLengthValue == null) ? maxLength : Math.min(this.maxLengthValue, maxLength));
        }

        private RangeFlex limitLeft(int limit) {
            IntRange limitStart = start().limitLeft(limit);
            IntRange limitEnd = end().limitLeft(limit);
            return new RangeFlex.Simple(limitStart, limitEnd, minLengthValue(), maxLengthValue());
        }

        private RangeFlex limitRight(int limit) {
            IntRange limitStart = start().limitRight(limit);
            IntRange limitEnd = end().limitRight(limit);
            return new RangeFlex.Simple(limitStart, limitEnd, minLengthValue(), maxLengthValue());
        }

        private RangeFlex constrainToRange(RangeFlex constraint) {
            int left = constraint.start().from();
            int right = constraint.end().to();
            IntRange limitStart = start().limitLeft(left).limitRight(right);
            IntRange limitEnd = end().limitLeft(left).limitRight(right);
            return new RangeFlex.Simple(limitStart, limitEnd, minLengthValue(), maxLengthValue());
        }

        private RangeFlex constrainWithSuccessor(RangeFlex successor) {
            return tryConstrain(Constraint.limitRight(successor.start().to()))
                    .orElseThrow(cause -> {
                        // TODO: this could be nicer
                        if (cause.getStackTrace()[0].getClassName().equals(IntRange.class.getName())) {
                            return new IllegalArgumentException("incompatible overlap at [%s - %s]".formatted(successor.start().to() + 1, this.end().from() - 1), cause);
                        }
                        return cause;

                    })
                    .tryConstrain(Constraint.satisfying(range -> {
                        if (range.end().exclusiveTo() < successor.start().from()) {
                            throw new IllegalArgumentException("gap at [%s - %s]".formatted(range.end().to() + 1, successor.start().from() - 1));
                        }
                    }))
                    .orElseThrow();
        }

        private RangeFlex constrainWithPredecessor(RangeFlex predecessor) {
            // TODO Compared with constrainWithSuccessor it feels like some tests are missing here
            return constrain(Constraint.limitLeft(predecessor.end().from()));
        }

        @Override
        public String toString() {
            return RangeFlex.toString(start, end, minLengthValue, maxLengthValue);
        }

    }

    private static Result<Void, RuntimeException> verifyLengthConstraints(IntRange start, IntRange end, Integer minLength, Integer maxLength) {
        int lowestLength = end.from() - start.exclusiveTo();
        int highestLength = end.exclusiveTo() - start.from();
        if (minLength != null && minLength > highestLength) {
            return Result.ofFailure(new IllegalArgumentException("Range %s with highest length of %d is strictly shorter than the minimum length of %d".formatted(RangeFlex.toString(start, end), highestLength, minLength)));
        } else if (maxLength != null && maxLength < lowestLength) {
            return Result.ofFailure(new IllegalArgumentException("Range %s with lowest length of %d is strictly longer than the maximum length of %d".formatted(RangeFlex.toString(start, end), lowestLength, maxLength)));
        } else if (minLength != null && maxLength != null && minLength > maxLength) {
            return Result.ofFailure(new IllegalArgumentException("Range %s cannot have length constraints where minimum length of %d is greater than maximum length of %d".formatted(RangeFlex.toString(start, end), minLength, maxLength)));
        } else {
            return Result.ofVoid();
        }
    }

}
