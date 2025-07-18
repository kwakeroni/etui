package com.quaxantis.etui.template.expression.matching;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class RangeFlexAssert extends AbstractAssert<RangeFlexAssert, RangeFlex> {

    public static RangeFlexAssert assertThat(RangeFlex rangeFlex) {
        return new RangeFlexAssert(rangeFlex);
    }

    public RangeFlexAssert(RangeFlex rangeFlex) {
        super(rangeFlex, RangeFlexAssert.class);
    }

    public RangeFlexAssert hasStartExact(int from) {
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.start()).as("start (exact)").isEqualTo(IntRange.of(from, from));
        return this;
    }

    public RangeFlexAssert hasStartBetween(int from, int to) {
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.start()).as("start (pre)").isEqualTo(IntRange.ofClosed(from, to));
        return this;
    }

    public RangeFlexAssert hasEndExact(int toExclusive) {
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.end()).as("end (exact)").isEqualTo(IntRange.of(toExclusive, toExclusive));
        return this;
    }

    public RangeFlexAssert hasEndBetween(int from, int to) {
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.end()).as("end (post)").isEqualTo(IntRange.ofClosed(from, to));
        return this;
    }


}
