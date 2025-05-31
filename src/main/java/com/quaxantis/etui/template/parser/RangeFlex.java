package com.quaxantis.etui.template.parser;

import com.quaxantis.support.util.Result;

import java.util.Objects;

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

    static boolean canConcat(RangeFlex left, RangeFlex right) {
        return tryConcat(left, right).isSuccess();
    }

    static RangeFlex concat(RangeFlex left, RangeFlex right) {
        return tryConcat(left, right).orElseThrow();
    }

    static Result<RangeFlex, RuntimeException> tryConcat(RangeFlex left, RangeFlex right) {
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
            RangeFlex limitLeft = left.limitRight(right.start().to());
            RangeFlex limitRight = right.limitLeft(left.end().from());
            return new Concat(limitLeft, limitRight);
        });
    }

    IntRange start();

    IntRange end();

    Integer minLength();

    RangeFlex withMinLength(Integer minLength);

    RangeFlex limitLeft(int limit);

    RangeFlex limitRight(int limit);

    RangeFlex constrain(RangeFlex constraint);

    default boolean contains(int i) {
        return i >= start().from() && i < end().exclusiveTo();
    }

    default IntRange lengthRange() {

        int minimum = end().from() - start().exclusiveTo();
        minimum = (this.minLength() == null) ? minimum : Math.max(this.minLength(), minimum);
        return IntRange.ofClosed(minimum, end().exclusiveTo() - start().from());
    }


    default String extractFrom(String string) {
        if (start().from() <= end().exclusiveTo()) {
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
        return toString(start, end, null);
    }

    private static String toString(IntRange start, IntRange end, Integer minLength) {
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
        String minLengthString = (minLength == null) ? "" : ("!" + minLength);
        return "{[" + leftString + "]" + midSpace + midString + midSpace + "[" + rightString + "]" + minLengthString + "}";
    }


    record Simple(IntRange start, IntRange end, Integer minLength) implements RangeFlex {

        public Simple {
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            verifyMinLengthConstraint(start, end, minLength).orElseThrow();
        }

        Simple(IntRange start, IntRange end) {
            this(start, end, null);
        }

        public RangeFlex withMinLength(Integer minLength) {
            if (minLength == null) {
                return this;
            }
            return new Simple(start, end, (this.minLength == null) ? minLength : Math.max(this.minLength, minLength));
        }


        @Override
        public RangeFlex limitLeft(int limit) {
            IntRange limitStart = start().limitLeft(limit);
            IntRange limitEnd = end().limitLeft(limit);
            return new RangeFlex.Simple(limitStart, limitEnd, minLength());
        }

        @Override
        public RangeFlex limitRight(int limit) {
            IntRange limitStart = start().limitRight(limit);
            IntRange limitEnd = end().limitRight(limit);
            return new RangeFlex.Simple(limitStart, limitEnd, minLength());
        }

        @Override
        public RangeFlex constrain(RangeFlex constraint) {
            int left = constraint.start().from();
            int right = constraint.end().to();
            IntRange limitStart = start().limitLeft(left).limitRight(right);
            IntRange limitEnd = end().limitLeft(left).limitRight(right);
            return new RangeFlex.Simple(limitStart, limitEnd, minLength()).withMinLength(constraint.minLength());
        }

        @Override
        public String toString() {
            return RangeFlex.toString(start, end, minLength);
        }

        private static Result<Void, RuntimeException> verifyMinLengthConstraint(IntRange start, IntRange end, Integer minLength) {
            int highestLength = end.exclusiveTo() - start.from();
            if (minLength != null && minLength > highestLength) {
                return Result.ofFailure(new IllegalArgumentException("Range %s with highest length of %d is strictly shorter than the minimum length of %d".formatted(RangeFlex.toString(start, end), highestLength, minLength)));
            } else {
                return Result.ofVoid();
            }
        }
    }

    record Concat(RangeFlex left, RangeFlex right, Integer minLength) implements RangeFlex {

        public Concat(RangeFlex left, RangeFlex right) {
            this(left, right, optSum(left.minLength(), right.minLength()));
        }

        @Override
        public IntRange start() {
            return left.start();
        }

        @Override
        public IntRange end() {
            return right.end();
        }

        @Override
        public Integer minLength() {
            return this.minLength;
        }

        @Override
        public RangeFlex.Concat withMinLength(Integer minLength) {
            if (minLength == null) {
                return this;
            }
            return new Concat(left, right, (this.minLength == null) ? minLength : Math.max(this.minLength, minLength));
        }

        @Override
        public RangeFlex.Concat limitLeft(int limit) {
            return new Concat(left.limitLeft(limit), right.limitLeft(limit), this.minLength);
        }

        @Override
        public RangeFlex.Concat limitRight(int limit) {
            return new Concat(left.limitRight(limit), right.limitRight(limit), this.minLength);
        }

        @Override
        public RangeFlex.Concat constrain(RangeFlex constraint) {
            return new RangeFlex.Concat(left.constrain(constraint), right.constrain(constraint), minLength()).withMinLength(constraint.minLength());
        }

        @Override
        public String toString() {
            return RangeFlex.toString(start(), end(), minLength) + " = " + left + " + " + right;
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

    }
}
