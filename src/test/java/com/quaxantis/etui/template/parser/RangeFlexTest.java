package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static com.quaxantis.etui.template.parser.RangeFlexAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RangeFlex")
class RangeFlexTest {

    @Nested
    @DisplayName("Can create a flex")
    class NewRangeFlex {
        @Test
        @DisplayName("between two flexible points")
        void newRangeFlex() {
            // [4..6]-[10..12] -> [4-10]..[6-12]
            var flex = new RangeFlex(4, 6, 10, 12);

            assertThat(IntStream.rangeClosed(4, 10)).allMatch(flex::contains);
            assertThat(IntStream.rangeClosed(6, 12)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 13)).noneMatch(flex::contains);
        }

        @Test
        @DisplayName("between two flexible points that may overlap")
        void newRangeFlexWhenOverlap() {
            // [4..9]-[7..11] -> [4-7]..[9-12]]
            var flex = new RangeFlex(4, 9, 7, 12);

            assertThat(IntStream.rangeClosed(4, 7)).allMatch(flex::contains);
            assertThat(IntStream.rangeClosed(9, 11)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 13)).noneMatch(flex::contains);
        }

        @Test
        @DisplayName("between two fixed points")
        void newRangeFlexWhenFixed() {
            // [4]-[12] -> [4-12]
            var flex = new RangeFlex(4, 4, 12, 12);

            assertThat(IntStream.rangeClosed(4, 12)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 13)).noneMatch(flex::contains);
        }

        @Test
        @DisplayName("between two fixed points that form a pair")
        void newRangeFlexWhenFixedPair() {
            // [4]-[5] -> [4-5]
            var flex = new RangeFlex(4, 4, 5, 5);

            assertThat(IntStream.rangeClosed(4, 5)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 6)).noneMatch(flex::contains);
        }

        @Test
        @DisplayName("between two fixed points that form a singleton")
        void newRangeFlexWhenFixedSingleton() {
            // [4]-[4] -> [4]
            var flex = new RangeFlex(4, 4, 4, 4);

            assertThat(IntStream.of(4)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 5)).noneMatch(flex::contains);
        }

        @Test
        @DisplayName("between two flexible points with negative overlap")
        void newRangeFlexWhenNegativeOverlap() {
            // [7..12]-[4..7] -> [10]
            RangeFlex flex = new RangeFlex(7, 12, 4, 7);

            assertThat(IntStream.rangeClosed(0, 6)).noneMatch(flex::contains);
            assertThat(IntStream.of(7)).allMatch(flex::contains);
            assertThat(IntStream.rangeClosed(8, 13)).noneMatch(flex::contains);
        }

        @Test
        @DisplayName("that is empty")
        void newRangeFlexWhenEmpty() {
            // [7..12]-[4..6] -> []
            RangeFlex flex = new RangeFlex(7, 12, 4, 6);
            assertThat(IntStream.rangeClosed(0, 13)).noneMatch(flex::contains);
        }


        @Test
        @DisplayName("that is empty when left is strictly greater than right")
        void newRangeFlexThrowsWhenLeftIsGreaterThanRight() {
            // [10..12]-[4-6]
            RangeFlex flex = new RangeFlex(10, 12, 4, 6);
            assertThat(IntStream.rangeClosed(0, 13)).noneMatch(flex::contains);
        }
    }

    @Nested
    @DisplayName("Can concatenate two flexes")
    class Concat {
        @Test
        @DisplayName("with an overlap")
        void concatWithOverlap() {
            // [4..6 - 10...12]
            //             [12..14 - 18..20]
            // [4..6        -        18..20]
            var flex1 = new RangeFlex(4, 6, 10, 12);
            var flex2 = new RangeFlex(12, 14, 18, 20);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = RangeFlex.concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 6).hasEndBetween(18, 20);
            assertThat(IntStream.rangeClosed(4, 18)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(6, 20)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 21)).noneMatch(concat::contains);
        }

        @Test
        @DisplayName("that are adjacent")
        void concatAdjacent() {
            // [4..6 - 10...12]
            //                [13..14 - 18..20]
            // [4..6          -         18..20]
            var flex1 = new RangeFlex(4, 6, 10, 12);
            var flex2 = new RangeFlex(13, 14, 18, 20);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = RangeFlex.concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 6).hasEndBetween(18, 20);
            assertThat(IntStream.rangeClosed(4, 18)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(6, 20)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 21)).noneMatch(concat::contains);
        }


        @Test
        @DisplayName("throws when there is a gap")
        void concatThrowsWithGap() {
            // [4..6 - 10...12] X
            //                  X [14 - 18..20]
            // [4..6          ] X [     18..20]
            var flex1 = new RangeFlex(4, 6, 10, 12);
            var flex2 = new RangeFlex(14, 14, 18, 20);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isFalse();
            assertThatThrownBy(() -> RangeFlex.concat(flex1, flex2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("[13 - 13]");
        }

        @Test
        @DisplayName("with a negative overlap")
        void concatWithNegativeOverlap() {
            // [4..6 - 10...12]
            //       [9...11 - 13..15]
            // [4..6 - 10-11 - 13..15]
            var flex1 = new RangeFlex(4, 6, 10, 12);
            var flex2 = new RangeFlex(12, 14, 18, 20);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = RangeFlex.concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 6).hasEndBetween(18, 20);
            assertThat(IntStream.rangeClosed(4, 18)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(6, 20)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 21)).noneMatch(concat::contains);

        }

        @Disabled
        @Test
        @DisplayName("with a full negative overlap")
        void concatWithFullNegativeOverlap() {
            // [4..6 - 4...6]
            //        [4  -  8..10]
            // [4    -       8..10]
            var flex1 = new RangeFlex(4, 6, 4, 6);
            var flex2 = new RangeFlex(4, 4, 8, 10);

//            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = RangeFlex.concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 4).hasEndBetween(8, 10);
            assertThat(IntStream.rangeClosed(4, 8)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(6, 10)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 11)).noneMatch(concat::contains);

        }


        @Test
        @DisplayName("throws when there is a negative gap with overlap")
        void concatThrowsWithNegativeGapWithOverlap() {
            // [4..6 - 10...12]
            //     [8..10  -  13..15]
            // [4..6 - XX  -  13..15]
            var flex1 = new RangeFlex(4, 6, 10, 12);
            var flex2 = new RangeFlex(8, 10, 13, 15);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isFalse();
            assertThatThrownBy(() -> RangeFlex.concat(flex1, flex2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("[10 - 10]");
        }

        @Test
        @DisplayName("throws when there is a negative gap")
        void concatThrowsWithNegativeGap() {
            // [4..6  -  10...12]
            //     [8..9    -    13..15]
            // [4..6 - XXXX -    13..15]
            var flex1 = new RangeFlex(4, 6, 10, 12);
            var flex2 = new RangeFlex(8, 9, 13, 15);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isFalse();
            assertThatThrownBy(() -> RangeFlex.concat(flex1, flex2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("[9 - 10]");
        }

        @Disabled
        @Test
        @DisplayName("with end overlapping start")
        void concatWithEndOverlappingStart() {
            // [4..15 - 4...15]
            //         [9 - 15]
            // [4..9  - 15]
            var flex1 = new RangeFlex(4, 12, 4, 15);
            var flex2 = new RangeFlex(9, 9, 15, 15);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = RangeFlex.concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 9).hasEndBetween(15, 15);
            assertThat(IntStream.rangeClosed(4, 15)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(8, 15)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 16)).noneMatch(concat::contains);
        }

        @Disabled
        @Test
        @DisplayName("with start overlapping end")
        void concatWithStartOverlappingEnd() {
            // [4 - 9]
            //         [7..15 - 7..15]
            // [4 - 9..15]
            var flex1 = new RangeFlex(4, 4, 9, 9);
            var flex2 = new RangeFlex(7, 15, 7, 15);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = RangeFlex.concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 4).hasEndBetween(9, 15);
            assertThat(IntStream.rangeClosed(4, 9)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(4, 15)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 16)).noneMatch(concat::contains);
        }

    }
}
