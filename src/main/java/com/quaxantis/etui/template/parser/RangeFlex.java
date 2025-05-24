package com.quaxantis.etui.template.parser;

import com.quaxantis.support.util.Result;

import java.util.Objects;

record RangeFlex(IntRange start, IntRange end, Integer minLength) {
    RangeFlex {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        verifyMinLengthConstraint(start, end, minLength).orElseThrow();
    }
//
//    class RangeFlex {private IntRange start, private IntRange end, private int minLength;
//
//        public RangeFlex(IntRange start, IntRange end, int minLength) {
//            this.end = end;
//            this.minLength = minLength;
//            this.start = start;
//        }

    public static RangeFlex of(int leftFrom, int leftToIncl, int rightFrom, int rightToIncl) {
        return of(IntRange.ofClosed(leftFrom, leftToIncl), IntRange.ofClosed(rightFrom, rightToIncl));
    }

    public static RangeFlex of(IntRange left, IntRange right) {
        return new RangeFlex(left, right);
    }

    public static RangeFlex ofFixedStart(int from, int rightFrom, int rightToIncl) {
        return of(IntRange.of(from, from), IntRange.ofClosed(rightFrom, rightToIncl));
    }

    public static RangeFlex ofFixedEnd(int leftFrom, int leftToIncl, int to) {
        return of(IntRange.ofClosed(leftFrom, leftToIncl), IntRange.of(to + 1, to + 1));
    }

    public static RangeFlex ofFixed(int from, int to) {
        return of(IntRange.of(from, from), IntRange.of(to + 1, to + 1));
    }

    private static Result<Void, RuntimeException> verifyMinLengthConstraint(IntRange start, IntRange end, Integer minLength) {
        int highestLength = end.exclusiveTo() - start.from();
        if (minLength != null && minLength > highestLength) {
            return Result.ofFailure(new IllegalArgumentException("Range %s with highest length of %d is strictly shorter than the minimum length of %d".formatted(toString(start, end), highestLength, minLength)));
        } else {
            return Result.of(null);
        }
    }


    RangeFlex(IntRange start, IntRange end) {
        this(start, end, null);
    }

    public boolean contains(int i) {
        return i >= start.from() && i < end.exclusiveTo();
    }

    public IntRange lengthRange() {

        int minimum = end.from() - start.exclusiveTo();
        minimum = (this.minLength == null) ? minimum : Math.max(this.minLength, minimum);
        return IntRange.ofClosed(minimum, end.exclusiveTo() - start.from());
    }

    public RangeFlex withMinLength(Integer minLength) {
        if (minLength == null) {
            return this;
        }
        return new RangeFlex(start, end, (this.minLength == null) ? minLength : Math.max(this.minLength, minLength));
    }

    public static boolean canConcat(RangeFlex left, RangeFlex right) {
        return tryConcat(left, right).isSuccess();
    }

    public static RangeFlex concat(RangeFlex left, RangeFlex right) {
        return tryConcat(left, right).orElseThrow();
    }

    public static Result<RangeFlex, RuntimeException> tryConcat(RangeFlex left, RangeFlex right) {
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

        try {
            IntRange leftStart = left.start().limitRight(right.start().to());
            IntRange leftEnd = left.end().limitRight(right.start().to());
            IntRange rightStart = right.start().limitLeft(left.end().from());
            IntRange rightEnd = right.end().limitLeft(left.end().from());

            verifyMinLengthConstraint(leftStart, leftEnd, left.minLength())
                    .orElseThrow(exc -> new IllegalArgumentException("Cannot concatenate flexes %s and %s: Cannot maintain minLength of left flex: " + exc.getMessage(), exc));
            verifyMinLengthConstraint(rightStart, rightEnd, right.minLength())
                    .orElseThrow(exc -> new IllegalArgumentException("Cannot concatenate flexes %s and %s: Cannot maintain minLength of right flex: " + exc.getMessage(), exc));

            var result = new RangeFlex(leftStart, rightEnd).withMinLength(optSum(left.minLength, right.minLength));
            return Result.of(result);
        } catch (RuntimeException exc) {
            return Result.ofFailure(exc);
        }
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

    @Override
    public String toString() {
        return toString(start, end, minLength);
    }

    private static String toString(IntRange start, IntRange end) {
        return toString(start, end, null);
    }

    private static String toString(IntRange start, IntRange end, Integer minLength) {
        int log10 = (int) Math.log10(Math.max(start.to(), end.to()));
        int d = log10 + 1;
        String rangePattern = "%" + d + "d-%" + d + "d";
        String singletonPattern = "%" + (d * 2 + 1) + "d";
        String empty = " ".repeat(d * 2 + 1);
        String leftString = (start.from() == start.to() ? singletonPattern : (start.from() < start.to()) ? rangePattern : empty)
                .formatted(start.from(), start.to());
        String midString = ((start.to() + 1 == end.from() - 1) ? singletonPattern : (start.to() + 1 < end.from() - 1) ? rangePattern : empty).formatted(start.to() + 1, end.from() - 1);
        String rightString = (end.from() == end.to() ? singletonPattern : (end.from() < end.to()) ? rangePattern : empty).formatted(end.from(), end.to());
        String minLengthString = (minLength == null) ? "" : ("!" + minLength);
        return "{[" + leftString + "] " + midString + " [" + rightString + "]" + minLengthString + "}";
    }

    public String applyTo(String string) {
        StringBuilder result = new StringBuilder();
        if (start.from() <= end.from()) {
            result.append(string.substring(0, start.from()));
            result.append("{[");

            if (start.exclusiveTo() <= end.from()) {
                result.append(string.substring(start.from(), start.exclusiveTo()))
                        .append("]")
                        .append(string.substring(start.exclusiveTo(), end.from()))
                        .append("[")
                        .append(string.substring(end.from(), end.exclusiveTo()));
            } else {
                result.append(string.substring(start.from(), end.from()))
                        .append("[")
                        .append(string.substring(end.from(), start.exclusiveTo()))
                        .append("]")
                        .append(string.substring(start.exclusiveTo(), end.exclusiveTo()));
            }
            result.append("]}");
            result.append(string.substring(end.exclusiveTo()));
        } else {
            result.append(string.substring(0, end.from()))
                    .append("[");
            if (end.exclusiveTo() <= start.from()) {
                result.append(string.substring(end.from(), end.exclusiveTo()))
                        .append("]}")
                        .append(string.substring(end.exclusiveTo(), start.from()))
                        .append("{[")
                        .append(string.substring(start.from(), start.exclusiveTo()));
            } else {
                result.append(string.substring(end.from(), start.from()))
                        .append("{[")
                        .append(string.substring(start.from(), end.exclusiveTo()))
                        .append("]}")
                        .append(string.substring(end.exclusiveTo(), start.exclusiveTo()));
            }
            result.append("]");
            result.append(string.substring(start.exclusiveTo()));
        }


        return result.toString();
    }
}
