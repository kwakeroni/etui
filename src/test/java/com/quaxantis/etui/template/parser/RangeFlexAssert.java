package com.quaxantis.etui.template.parser;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class RangeFlexAssert extends AbstractAssert<RangeFlexAssert, RangeFlex> {

    public static RangeFlexAssert assertThat(RangeFlex rangeFlex) {
        return new RangeFlexAssert(rangeFlex);
    }

    public RangeFlexAssert(RangeFlex rangeFlex) {
        super(rangeFlex, RangeFlexAssert.class);
    }

    public RangeFlexAssert hasStartBetween(int from, int to) {
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.start()).as("start").isEqualTo(IntRange.ofClosed(from, to));
        return this;
    }

    public RangeFlexAssert hasEndBetween(int from, int to) {
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.end()).as("end").isEqualTo(IntRange.ofClosed(from, to));
        return this;
    }


}
