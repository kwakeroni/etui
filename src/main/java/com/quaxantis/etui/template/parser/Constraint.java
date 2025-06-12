package com.quaxantis.etui.template.parser;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

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

    static PrecedeBy precedeBy(RangeFlex range) {
        return new PrecedeBy(range);
    }

    static SucceedBy succeedBy(RangeFlex range) {
        return new SucceedBy(range);
    }

    static Satisfying satisfying(Consumer<RangeFlex> verification) {
        return new Satisfying(verification);
    }

    static Mapping mapping(UnaryOperator<RangeFlex> operator) {
        return new Mapping(operator);
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

    record PrecedeBy(RangeFlex range) implements Constraint {}

    record SucceedBy(RangeFlex range) implements Constraint {}

    record Satisfying(Consumer<RangeFlex> verification) implements Constraint {}

    record Mapping(UnaryOperator<RangeFlex> operator) implements Constraint {}

    record And(Constraint one, Constraint two) implements Constraint {}
}
