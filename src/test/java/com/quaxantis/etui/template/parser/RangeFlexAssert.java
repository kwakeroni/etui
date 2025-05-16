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
        Assertions.assertThat(actual.start()).as("start").isEqualTo(new IntRange(from, to + 1));
//        Assertions.assertThat(actual.start().to()-1).as("start.to").isEqualTo(to); // 'to' is exclusive
        return this;
    }

    public RangeFlexAssert hasEndBetween(int from, int to) {
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.end()).as("end").isEqualTo(new IntRange(from, to + 1));
//        Assertions.assertThat(actual.end().from()).as("end.from").isEqualTo(from);
//        Assertions.assertThat(actual.end().to()-1).as("end.to").isEqualTo(to); // 'to' is exclusive
        return this;
    }


}
