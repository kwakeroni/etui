package com.quaxantis.etui.template.parser;

sealed interface Constraint {

    static RangeLimit toRange(RangeFlex range) {
        return new RangeLimit(range);
    }

    static FixedRange toFixedRange() {
        return new FixedRange();
    }

    static FixPrefix fixPrefix() {
        return new FixPrefix();
    }

    static FixSuffix fixSuffix() {
        return new FixSuffix();
    }

    static LimitLeft limitLeft(int leftPosition) {
        return new LimitLeft(leftPosition);
    }

    static LimitRight limitRight(int rightPosition) {
        return new LimitRight(rightPosition);
    }

    static MinLength toMinLength(int minLength) {
        return new MinLength(minLength);
    }

    static MaxLength toMaxLength(int maxLength) {
        return new MaxLength(maxLength);
    }

    default Constraint and(Constraint constraint) {
        return new And(this, constraint);
    }

    record RangeLimit(RangeFlex range) implements Constraint {}

    record FixedRange() implements Constraint {}

    record FixPrefix() implements Constraint {}

    record FixSuffix() implements Constraint {}

    record LimitLeft(int leftPosition) implements Constraint {}

    record LimitRight(int rightPosition) implements Constraint {}

    record MinLength(int minLength) implements Constraint {}

    record MaxLength(int maxLength) implements Constraint {}

    record And(Constraint one, Constraint two) implements Constraint {}
}
