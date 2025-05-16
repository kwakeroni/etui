package com.quaxantis.etui.template.parser;

import java.util.Objects;
import java.util.Optional;

record RangeFlex(IntRange start, IntRange end, int minLength) {
    RangeFlex {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        // TODO check minlength
// TODO Check if needed
//            if (left.from() > right.from()) {
//                throw new IllegalArgumentException("Cannot create flex where left=%s is strictly greater than right=%s".formatted(left, right));
//            }
    }

    RangeFlex(IntRange start, IntRange end) {
        this(start, end, 0);
    }

    RangeFlex(int leftFrom, int leftToIncl, int rightFrom, int rightToIncl) {
        this(IntRange.ofClosed(leftFrom, leftToIncl), IntRange.ofClosed(rightFrom, rightToIncl));
    }

    public boolean contains(int i) {
        return i >= start.from() && i < end.exclusiveTo();
    }

    public RangeFlex withMinLength(int minLength) {
        // TODO only if minlength is smaller than current minLength
        return new RangeFlex(start, end, minLength);
    }

    public static boolean canConcat(RangeFlex left, RangeFlex right) {
        return validateConcat(left, right).isEmpty();
    }

    public static RangeFlex concat(RangeFlex left, RangeFlex right) {
        validateConcat(left, right).ifPresent(exc -> {
            throw exc;
        });

        IntRange start = left.start().truncateRight(right.start(), 0).trimRight(left.minLength);
        IntRange end = right.end().truncateLeft(left.end(), 0).trimLeft(right.minLength);

        return new RangeFlex(start, end);
    }

    private static Optional<RuntimeException> validateConcat(RangeFlex left, RangeFlex right) {
        int leftMaxEnd = left.end().to();
        int rightMinStart = right.start().from();
        if (leftMaxEnd < rightMinStart - 1) {
            // the right flex must start at the very latest just after the left flex ends
            // or: if the highest rightmost value of the left flex is smaller than the smallest leftmost value of the right flex (by at least 1), then there is a gap
            return Optional.of(new IllegalArgumentException("Cannot concatenate flexes %s and %s: gap at [%s - %s]".formatted(left, right, leftMaxEnd + 1, rightMinStart - 1)));
        }
        int rightMaxStart = right.start().to();
        int leftMinEnd = left.end().from();
        if (rightMaxStart <= leftMinEnd) {
            // the right flex cannot start before the left flex ends
            // or: if the highest leftmost value of the right flex is smaller (or the same) as the smallest rightmost value of the left flex, then there is an incompatible overlap
            return Optional.of(new IllegalArgumentException("Cannot concatenate flexes %s and %s: incompatible overlap at [%s - %s]".formatted(left, right, rightMaxStart, leftMinEnd)));
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        String leftString = start.from() + ((start.from() == start.to()) ? "" : ".." + start.to());
        String rightString = end.from() + ((end.from() == end.to()) ? "" : ".." + end.to());
        return "[" + leftString + " - " + rightString + "]";
    }
}
