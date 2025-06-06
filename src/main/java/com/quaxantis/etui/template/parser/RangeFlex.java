package com.quaxantis.etui.template.parser;

import com.quaxantis.support.util.Result;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

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

    static boolean canConcat(RangeFlex left, RangeFlex right) {
        return tryConcat(left, right).isSuccess();
    }

    static RangeFlex.Concat concat(RangeFlex left, RangeFlex right) {
        return tryConcat(left, right).orElseThrow();
    }

    static Result<RangeFlex.Concat, RuntimeException> tryConcat(RangeFlex left, RangeFlex right) {
        int leftMaxEnd = left.end().to();
        int rightMinStart = right.start().from();
        if (leftMaxEnd < rightMinStart - 1) {
            // the right flex must start at the very latest just after the left flex ends
            // or: if the highest rightmost value of the left flex is smaller than the smallest leftmost value of the right flex (by at least 1), then there is a gap
            return Result.ofFailure(new IllegalArgumentException("Cannot concatenate flexes %s and %s: gap at [%s - %s]".formatted(left, right, leftMaxEnd + 1, rightMinStart - 1)));
        }
        int rightMaxStart = right.start().to();
        int leftMinEnd = left.end().from();
        if (rightMaxStart < leftMinEnd - 1) {
            // the right flex cannot start before the left flex ends
            // or: if the highest leftmost value of the right flex is smaller than the smallest rightmost value of the left flex, then there is an incompatible overlap
            return Result.ofFailure(new IllegalArgumentException("Cannot concatenate flexes %s and %s: incompatible overlap at [%s - %s]".formatted(left, right, rightMaxStart + 1, leftMinEnd - 1)));
        }

        return Result.ofTry(() -> {
            RangeFlex limitLeft = left.constrain(Constraint.limitRight(right.start().to()));
            RangeFlex limitRight = right.constrain(Constraint.limitLeft(left.end().from()));
            return new Concat(limitLeft, limitRight);
        });
    }

    IntRange start();

    IntRange end();

    Integer minLength();

    Integer maxLength();

    RangeFlex constrain(Constraint constraint);

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

        int minimum = end().from() - start().exclusiveTo();
        minimum = (this.minLength() == null) ? minimum : Math.max(this.minLength(), minimum);
        int maximum = end().exclusiveTo() - start().from();
        maximum = (this.maxLength() == null) ? maximum : Math.min(this.maxLength(), maximum);
        return IntRange.ofClosed(minimum, maximum);
    }


    default String extractFrom(String string) {
        // TODO what about fuzzy strings and other length constraints ?
        if (maxLength() instanceof Integer max && max == 0) {
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


    record Simple(IntRange start, IntRange end, Integer minLength, Integer maxLength) implements RangeFlex {

        public Simple {
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            verifyLengthConstraints(start, end, minLength, maxLength).orElseThrow();
        }

        Simple(IntRange start, IntRange end) {
            this(start, end, null, null);
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
            return new Simple(start, end, (this.minLength == null) ? minLength : Math.max(this.minLength, minLength), this.maxLength);
        }

        private Simple constrainMaxLength(int maxLength) {
            return new Simple(start, end, this.minLength, (this.maxLength == null) ? maxLength : Math.min(this.maxLength, maxLength));
        }

        private RangeFlex limitLeft(int limit) {
            IntRange limitStart = start().limitLeft(limit);
            IntRange limitEnd = end().limitLeft(limit);
            return new RangeFlex.Simple(limitStart, limitEnd, minLength(), maxLength());
        }

        private RangeFlex limitRight(int limit) {
            IntRange limitStart = start().limitRight(limit);
            IntRange limitEnd = end().limitRight(limit);
            return new RangeFlex.Simple(limitStart, limitEnd, minLength(), maxLength());
        }

        private RangeFlex constrainToRange(RangeFlex constraint) {
            int left = constraint.start().from();
            int right = constraint.end().to();
            IntRange limitStart = start().limitLeft(left).limitRight(right);
            IntRange limitEnd = end().limitLeft(left).limitRight(right);
            return new RangeFlex.Simple(limitStart, limitEnd, minLength(), maxLength());
        }

        @Override
        public String toString() {
            return RangeFlex.toString(start, end, minLength, maxLength);
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
            return Result.ofFailure(new IllegalArgumentException("Range %s cannot have lenght constraints where minimum length of %d is greater than maximum length of %d".formatted(RangeFlex.toString(start, end), minLength, maxLength)));
        } else {
            return Result.ofVoid();
        }
    }

    record Concat(RangeFlex left, RangeFlex right, Integer minLength, Integer maxLength) implements RangeFlex {


        public Concat {
            Objects.requireNonNull(left, "start");
            Objects.requireNonNull(right, "end");
            verifyLengthConstraints(left.start(), right.end(), minLength, maxLength).orElseThrow();
        }

        public Concat(RangeFlex left, RangeFlex right) {
            this(left, right, optSum(left.minLength(), right.minLength()), optSumOrNull(left.maxLength(), right.maxLength()));
        }

        @Override
        public IntRange start() {
            return left.start();
        }

        @Override
        public IntRange end() {
            return right.end();
        }

        public Concat constrain(Constraint constraint) {
            return switch (constraint) {
                case Constraint.MinLength(int min) -> constrainMinLength(min);
                case Constraint.MaxLength(int max) -> constrainMaxLength(max);
                case Constraint.LimitLeft limitLeft -> limitLeft(limitLeft);
                case Constraint.LimitRight limitRight -> limitRight(limitRight);
                case Constraint.RangeLimit(RangeFlex range) -> constrainToRange(range);
                case Constraint.FixedRange _ -> toFixedRange();
                case Constraint.FixPrefix _ -> fixPrefix();
                case Constraint.FixSuffix _ -> fixSuffix();
                case Constraint.And(var one, var two) -> constrain(one).constrain(two);
            };
        }

        private Concat toFixedRange() {
            return new Concat(left.constrain(Constraint.fixPrefix()), right.constrain(Constraint.fixSuffix()), minLength, maxLength);
        }

        private Concat fixPrefix() {
            return new Concat(left.constrain(Constraint.fixPrefix()), right, minLength, maxLength);
        }

        private Concat fixSuffix() {
            return new Concat(left, right.constrain(Constraint.fixSuffix()), minLength, maxLength);
        }

        private Concat constrainMinLength(int minLength) {
            return new Concat(left, right, (this.minLength == null) ? minLength : Math.max(this.minLength, minLength), this.maxLength);
        }

        private Concat constrainMaxLength(Integer maxLength) {
            return new Concat(left, right, this.minLength, (this.maxLength == null) ? maxLength : Math.min(this.maxLength, maxLength));
        }

        private Concat limitLeft(Constraint.LimitLeft limitLeft) {
            return new Concat(left.constrain(limitLeft), right.constrain(limitLeft), this.minLength, this.maxLength);
        }

        private Concat limitRight(Constraint.LimitRight limitRight) {
            return new Concat(left.constrain(limitRight), right.constrain(limitRight), this.minLength, this.maxLength);
        }

        private Concat constrainToRange(RangeFlex constraint) {
            return new RangeFlex.Concat(left.constrain(Constraint.toRange(constraint)), right.constrain(Constraint.toRange(constraint)), minLength(), maxLength())/*.withMinLength(constraint.minLength()).withMaxLength(constraint.maxLength()) */;
        }

        @Override
        public String toString() {
            return RangeFlex.toString(start(), end(), minLength, maxLength) + " = " + left + " + " + right;
        }

        private static Integer optSum(Integer left, Integer right) {
            if (left != null) {
                if (right != null) {
                    return left + right;
                } else {
                    return left;
                }
            } else {
                return right;
            }
        }

        private static Integer optSumOrNull(Integer left, Integer right) {
            if (left != null) {
                if (right != null) {
                    return left + right;
                }
            }
            return null;
        }

    }
}
