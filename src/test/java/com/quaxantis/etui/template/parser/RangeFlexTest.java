package com.quaxantis.etui.template.parser;

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
            var flex = RangeFlex.of(4, 6, 10, 12);

            assertThat(IntStream.rangeClosed(4, 10)).allMatch(flex::contains);
            assertThat(IntStream.rangeClosed(6, 12)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123{[456]789[012]}3456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(3, 9));
            assertThat(flex.toString()).isEqualTo("{[ 4- 6]  7- 9 [10-12]}");
        }

        @Test
        @DisplayName("between two flexible points that may overlap")
        void newRangeFlexWhenOverlap() {
            // [4..9]-[7..12] -> [4-7]..[9-12]]
            var flex = RangeFlex.of(4, 9, 7, 12);

            assertThat(IntStream.rangeClosed(4, 7)).allMatch(flex::contains);
            assertThat(IntStream.rangeClosed(9, 11)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123{[456[789]012]}3456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(-3, 9));
            assertThat(flex.toString()).isEqualTo("{[ 4- 9]       [ 7-12]}");
        }

        @Test
        @DisplayName("between two flexible points where suffix is contained in prefix")
        void newRangeFlexWhenSuffixInPrefix() {
            // [4..12]-[7..9] -> [4-9]
            var flex = RangeFlex.of(4, 12, 7, 9);

            assertThat(IntStream.rangeClosed(4, 9)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 10)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123{[456[789]}012]3456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(-6, 6));
            assertThat(flex.toString()).isEqualTo("{[ 4-12]       [ 7- 9]}");
        }


        @Test
        @DisplayName("between two flexible points where prefix is contained in suffix")
        void newRangeFlexWhenPrefixInSuffix() {
            // [7..9]-[4..12] -> [7-12]]
            var flex = RangeFlex.of(7, 9, 4, 12);

            assertThat(IntStream.rangeClosed(7, 12)).allMatch(flex::contains);
            assertThat(IntStream.of(6, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123[456{[789]012]}3456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(-6, 6));
            assertThat(flex.toString()).isEqualTo("{[ 7- 9]       [ 4-12]}");
        }


        @Test
        @DisplayName("between two single affixes")
        void newRangeFlexWithSingleAffixes() {
            // [4]-[12] -> [4-12]
            var flex = RangeFlex.of(4, 4, 12, 12);

            assertThat(IntStream.rangeClosed(4, 12)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123{[4]5678901[2]}3456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(7, 9));
            assertThat(flex.toString()).isEqualTo("{[    4]  5-11 [   12]}");
        }

        @Test
        @DisplayName("between two fixed points")
        void newRangeFlexWhenFixed() {
            // [4]-[12] -> [4-12]
            var flex = RangeFlex.ofFixed(4, 12);

            assertThat(IntStream.rangeClosed(4, 12)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123{[]456789012[]}3456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(9, 9));
            assertThat(flex.toString()).isEqualTo("{[     ]  4-12 [     ]}");
        }

        @Test
        @DisplayName("with a fixed start")
        void newRangeFlexWhenFixedStart() {
            // [4]-[7-12] -> [4-12]
            var flex = RangeFlex.ofFixedStart(4, 7, 12);

            assertThat(IntStream.rangeClosed(4, 12)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123{[]456[789012]}3456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(3, 9));
            assertThat(flex.toString()).isEqualTo("{[     ]  4- 6 [ 7-12]}");
        }

        @Test
        @DisplayName("with a fixed end")
        void newRangeFlexWhenFixedEnd() {
            // [4-7]-[12] -> [4-12]
            var flex = RangeFlex.ofFixedEnd(4, 7, 12);

            assertThat(IntStream.rangeClosed(4, 12)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123{[4567]89012[]}3456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(5, 9));
            assertThat(flex.toString()).isEqualTo("{[ 4- 7]  8-12 [     ]}");
        }

        @Test
        @DisplayName("between two single points that form a pair")
        void newRangeFlexWhenSinglePair() {
            // [4]-[5] -> [4-5]
            var flex = RangeFlex.of(4, 4, 5, 5);

            assertThat(IntStream.rangeClosed(4, 5)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 6)).noneMatch(flex::contains);
            assertThat(apply(flex, "0123456789"))
                    .isEqualTo("0123{[4][5]}6789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(0, 2));
            assertThat(flex.toString()).isEqualTo("{[  4]     [  5]}");
        }

        @Test
        @DisplayName("between two fixed points that form a pair")
        void newRangeFlexWhenFixedPair() {
            // [4]-[5] -> [4-5]
            var flex = RangeFlex.ofFixed(4, 5);

            assertThat(IntStream.rangeClosed(4, 5)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 6)).noneMatch(flex::contains);
            assertThat(apply(flex, "0123456789"))
                    .isEqualTo("0123{[]45[]}6789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(2, 2));
            assertThat(flex.toString()).isEqualTo("{[   ] 4-5 [   ]}");
        }

        @Test
        @DisplayName("between two single points that form a singleton")
        void newRangeFlexWhenSingleSingleton() {
            // [4]-[4] -> [4]
            var flex = RangeFlex.of(4, 4, 4, 4);

            assertThat(IntStream.of(4)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 5)).noneMatch(flex::contains);
            assertThat(apply(flex, "0123456789"))
                    .isEqualTo("0123{[[4]]}56789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(-1, 1));
            assertThat(flex.toString()).isEqualTo("{[  4]     [  4]}");
        }

        @Test
        @DisplayName("between two fixed points that form a singleton")
        void newRangeFlexWhenFixedSingleton() {
            // [4]-[4] -> [4]
            var flex = RangeFlex.ofFixed(4, 4);

            assertThat(IntStream.of(4)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 5)).noneMatch(flex::contains);
            assertThat(apply(flex, "0123456789"))
                    .isEqualTo("0123{[]4[]}56789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(1, 1));
            assertThat(flex.toString()).isEqualTo("{[   ]   4 [   ]}");
        }


        @Test
        @DisplayName("between two flexible points with negative overlap")
        void newRangeFlexWhenNegativeOverlap() {
            // [7..12]-[4..7] -> [7]
            RangeFlex flex = RangeFlex.of(7, 12, 4, 7);

            assertThat(IntStream.rangeClosed(0, 6)).noneMatch(flex::contains);
            assertThat(IntStream.of(7)).allMatch(flex::contains);
            assertThat(IntStream.rangeClosed(8, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123[456{[7]}89012]3456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(-9, 1));
            assertThat(flex.toString()).isEqualTo("{[ 7-12]       [ 4- 7]}");
        }

        @Test
        @DisplayName("that is empty")
        void newEmptyRangeFlex() {
            RangeFlex flex = RangeFlex.empty();
            assertThat(IntStream.rangeClosed(0, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("{[][]}01234567890123456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(0, 0));
            assertThat(flex.toString()).isEqualTo("{[][]}");
        }

        @Test
        @DisplayName("that is empty when left comes after right")
        void newRangeFlexIsEmptyWhenLeftIsAfterRight() {
            // [7..12]-[4..6] -> []
            RangeFlex flex = RangeFlex.of(7, 12, 4, 6);
            assertThat(IntStream.rangeClosed(0, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123[456]}{[789012]3456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(-9, 0));
            assertThat(flex.toString()).isEqualTo("{[ 7-12]       [ 4- 6]}");
        }


        @Test
        @DisplayName("that is empty when left comes after right with a gap")
        void newRangeFlexIsEmptyWhenLeftIsAfterRightWithGap() {
            // [10..12]-[4-6]
            RangeFlex flex = RangeFlex.of(10, 12, 4, 6);
            assertThat(IntStream.rangeClosed(0, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123[456]}789{[012]3456789");
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(-9, -3));
            assertThat(flex.toString()).isEqualTo("{[10-12]       [ 4- 6]}");
        }

        @Test
        @DisplayName("with a minimal length")
        void withMinimalLength() {
            // [4..6]-[10..12] -> [4-10]..[6-12]
            var flex = RangeFlex.of(4, 6, 10, 12).withMinLength(9);

            assertThat(IntStream.rangeClosed(4, 10)).allMatch(flex::contains);
            assertThat(IntStream.rangeClosed(6, 12)).allMatch(flex::contains);
            assertThat(IntStream.of(3, 13)).noneMatch(flex::contains);
            assertThat(apply(flex, "01234567890123456789"))
                    .isEqualTo("0123{[456]789[012]}3456789");
            assertThat(flex.minLength()).isEqualTo(9);
            assertThat(flex.lengthRange()).isEqualTo(IntRange.ofClosed(9, 9));
            assertThat(flex.toString()).isEqualTo("{[ 4- 6]  7- 9 [10-12]!9}");
        }

        @Test
        @DisplayName("cannot create a rangeflex smaller than the minimal length")
        void throwsWhenSmallerThanMinimalLength() {
            assertThatThrownBy(() -> RangeFlex.of(4, 6, 10, 12).withMinLength(10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("highest length of 9 is strictly shorter than the minimum length of 10");

        }

    }

    @Nested
    @DisplayName("Can concatenate two flexes")
    class Concat {

        static RangeFlex concat(RangeFlex left, RangeFlex right) {
            try {
                RangeFlex result = RangeFlex.concat(left, right);
                System.out.printf("""
                                    %s
                                  + %s
                                  = %s
                                  """, left, right, result);
                return result;
            } catch (Exception exc) {
                System.out.printf("""
                                    %s
                                  + %s
                                  = %s
                                  """, left, right, exc);
                throw exc;
            }
        }

        @Test
        @DisplayName("with an overlap")
        void concatWithOverlap() {
            // [4..6 - 10...12]
            //             [12..14 - 18..20]
            // [4..6        -        18..20]
            var flex1 = RangeFlex.of(4, 6, 10, 12);
            var flex2 = RangeFlex.of(12, 14, 18, 20);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 6).hasEndBetween(18, 20);
            assertThat(IntStream.rangeClosed(4, 18)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(6, 20)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 21)).noneMatch(concat::contains);
            assertThat(apply(concat, "012345678901234567890123456789"))
                    .isEqualTo("0123{[456]78901234567[890]}123456789");
            System.out.println(concat.toString());
        }

        @Test
        @DisplayName("that are adjacent")
        void concatAdjacent() {
            // [4..6 - 10...12]
            //                [13..15 - 19..21]
            // [4..6          -         19..21]
            var flex1 = RangeFlex.of(4, 6, 10, 12);
            var flex2 = RangeFlex.of(13, 15, 19, 21);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 6).hasEndBetween(19, 21);
            assertThat(IntStream.rangeClosed(4, 19)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(6, 21)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 22)).noneMatch(concat::contains);
            assertThat(apply(concat, "012345678901234567890123456789"))
                    .isEqualTo("0123{[456]789012345678[901]}23456789");

        }

        @Test
        @DisplayName("throws when there is a gap")
        void concatThrowsWithGap() {
            // [4..6 - 10...12] X
            //                  X [14..16 - 20..22]
            // [4..6          ] X [         20..22]
            var flex1 = RangeFlex.of(4, 6, 10, 12);
            var flex2 = RangeFlex.of(14, 16, 20, 22);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isFalse();
            assertThatThrownBy(() -> concat(flex1, flex2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("[13 - 13]");
        }

        @Test
        @DisplayName("with a negative overlap (with infixes)")
        void concatWithNegativeOverlap() {
            // [4..6 - 10...12]
            //       [9...11 - 15..17]
            // [4..6   10-11   15..17]
            var flex1 = RangeFlex.of(4, 6, 10, 12);
            var flex2 = RangeFlex.of(9, 11, 15, 17);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 6).hasEndBetween(15, 17);
            assertThat(IntStream.rangeClosed(4, 15)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(6, 17)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 18)).noneMatch(concat::contains);
            assertThat(apply(concat, "01234567890123456789"))
                    .isEqualTo("0123{[456]78901234[567]}89");

        }

        @Test
        @DisplayName("with a negative overlap (internally adjacent without infixes)")
        void concatInternallyAdjacent() {
            // [4..6  -  10...12]
            //     [7..9    -   13..15]
            // [4..6     -      13..15]
            var flex1 = RangeFlex.of(4, 6, 10, 12);
            var flex2 = RangeFlex.of(7, 9, 13, 15);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 6).hasEndBetween(13, 15);
            assertThat(IntStream.rangeClosed(4, 13)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(6, 15)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 16)).noneMatch(concat::contains);
            assertThat(apply(concat, "01234567890123456789"))
                    .isEqualTo("0123{[456]789012[345]}6789");
        }

        @Test
        @DisplayName("throws when there is a negative gap")
        void concatThrowsWithNegativeGap() {
            // [4..6   -  10..12]
            //    [6..8   -   12..14]
            // [4..6 -8 X 10- 12..14]
            var flex1 = RangeFlex.of(4, 6, 10, 12);
            var flex2 = RangeFlex.of(6, 8, 12, 14);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isFalse();
            assertThatThrownBy(() -> concat(flex1, flex2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("[9 - 9]");
        }

        @Test
        @DisplayName("with an optional left flex and an overlap on the right prefix")
        void concatLeftOptFlexWithAffixOverlap() {
            // [4..9 - 4..9]
            //          [8..10 - 14..16]
            // [4.........9 - 14..16]
            var flex1 = RangeFlex.of(4, 9, 4, 9);
            var flex2 = RangeFlex.of(8, 10, 14, 16
            );

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 9).hasEndBetween(14, 16);
            assertThat(IntStream.rangeClosed(4, 14)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(9, 16)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 17)).noneMatch(concat::contains);
            assertThat(apply(concat, "01234567890123456789"))
                    .isEqualTo("0123{[456789]0123[456]}789");
        }

        @Test
        @DisplayName("with an optional left flex and an overlap on the right main part")
        void concatLeftOptFlexWithMainOverlap() {
            // [4..9 - 4..9]
            //   [5..7  -  11..13]
            // [4....7  -  11..13]
            var flex1 = RangeFlex.of(4, 9, 4, 9);
            var flex2 = RangeFlex.of(5, 7, 11, 13);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 7).hasEndBetween(11, 13);
            assertThat(IntStream.rangeClosed(4, 11)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(7, 13)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 14)).noneMatch(concat::contains);
            assertThat(apply(concat, "01234567890123456789"))
                    .isEqualTo("0123{[4567]890[123]}456789");
        }


        @Test
        @DisplayName("with an optional left flex reduced to empty by the right main part")
        void concatLeftOptFlexWithCompleteMainOverlap() {
            // [4..9 - 4..9]
            // [4    - 7..9]
            // [4    - 7..9]
            var flex1 = RangeFlex.of(4, 9, 4, 9);
            var flex2 = RangeFlex.ofFixedStart(4, 7, 9);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartExact(4).hasEndBetween(7, 9);
            assertThat(IntStream.rangeClosed(4, 7)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(4, 9)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 10)).noneMatch(concat::contains);
            assertThat(apply(concat, "01234567890123456789"))
                    .isEqualTo("0123{[]456[789]}0123456789");
        }


        @Test
        @DisplayName("with an optional left flex reduced to empty by the right main part with a prefix")
        void concatLeftOptFlexWithCompleteMainOverlapWithPrefix() {
            //  [4..9 - 4..9]
            // [3..3 - 7..8]
            //  [4    - 7..8]
            var flex1 = RangeFlex.of(4, 9, 4, 9);
            var flex2 = RangeFlex.of(3, 3, 7, 8);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartExact(4).hasEndBetween(7, 8);
            assertThat(IntStream.rangeClosed(4, 7)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(4, 8)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 9)).noneMatch(concat::contains);
            assertThat(apply(concat, "01234567890123456789"))
                    .isEqualTo("0123{[]456[78]}90123456789");
        }


        @Test
        @DisplayName("with an optional left flex reduced to empty with a negative overlap by the right main part")
        void concatLeftOptFlexWithCompleteNegativeMainOverlap() {
            //  [4..9 - 4..9]
            // [3  -  6..8]
            // [X  - 6..8]
            var flex1 = RangeFlex.of(4, 9, 4, 9);
            var flex2 = RangeFlex.ofFixedStart(3, 6, 8);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isFalse();
            assertThatThrownBy(() -> concat(flex1, flex2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("[3 - 3]");
        }


        @Test
        @DisplayName("with an optional right flex and an overlap on the left suffix")
        void concatRightOptFlexWithAffixOverlap() {
            // [4..6 - 10..12]
            //          [11..16 - 11..16]
            // [4..6  -  11..16]
            var flex1 = RangeFlex.of(4, 6, 10, 12);
            var flex2 = RangeFlex.of(11, 16, 11, 16);

//            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 6).hasEndBetween(11, 16);
            assertThat(IntStream.rangeClosed(4, 11)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(6, 16)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 17)).noneMatch(concat::contains);
            assertThat(apply(concat, "01234567890123456789"))
                    .isEqualTo("0123{[456]7890[123456]}789");
        }

        @Test
        @DisplayName("with an optional right flex and an overlap on the left main part")
        void concatRightOptFlexWithMainOverlap() {
            // [7..9 - 13..15]
            //       [11..16 - 11..16]
            // [7..9  -  13..16]
            var flex1 = RangeFlex.of(7, 9, 13, 15);
            var flex2 = RangeFlex.of(11, 16, 11, 16);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartBetween(7, 9).hasEndBetween(13, 16);
            assertThat(IntStream.rangeClosed(7, 13)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(9, 16)).allMatch(concat::contains);
            assertThat(IntStream.of(6, 17)).noneMatch(concat::contains);
            assertThat(apply(concat, "01234567890123456789"))
                    .isEqualTo("0123456{[789]012[3456]}789");
        }


        @Test
        @DisplayName("with an optional right flex reduced to empty by the left main part")
        void concatRightOptFlexWithCompleteMainOverlap() {
            // [11..13 - 16]
            // [11..16 - 11..16]
            // [11..13 - 16]
            var flex1 = RangeFlex.ofFixedEnd(11, 13, 16);
            var flex2 = RangeFlex.of(11, 16, 11, 16);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartBetween(11, 13).hasEndExact(17);
            assertThat(IntStream.rangeClosed(11, 16)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(13, 16)).allMatch(concat::contains);
            assertThat(IntStream.of(10, 17)).noneMatch(concat::contains);
            assertThat(apply(concat, "01234567890123456789"))
                    .isEqualTo("01234567890{[123]456[]}789");
        }


        @Test
        @DisplayName("with an optional right flex reduced to empty by the left main part with a suffix")
        void concatRightOptFlexWithCompleteMainOverlapWithSuffix() {
            // [11..13   -   17..19]
            // [11..16 - 11..16]
            // [11..13 -     16]
            var flex1 = RangeFlex.of(11, 13, 17, 19);
            var flex2 = RangeFlex.of(11, 16, 11, 16);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartBetween(11, 13).hasEndExact(17);
            assertThat(IntStream.rangeClosed(11, 16)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(13, 16)).allMatch(concat::contains);
            assertThat(IntStream.of(10, 17)).noneMatch(concat::contains);
            assertThat(apply(concat, "01234567890123456789"))
                    .isEqualTo("01234567890{[123]456[]}789");
        }

        @Test
        @DisplayName("with an optional right flex reduced to empty with a negative overlap by the left main part")
        void concatRightOptFlexWithCompleteNegativeMainOverlap() {
            //  [12..14     -    18..20]
            // [11..16 - 11..16]
            //  [12..14    -   X 18..20]
            var flex1 = RangeFlex.of(12, 14, 18, 20);
            var flex2 = RangeFlex.of(11, 16, 11, 16);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isFalse();
            assertThatThrownBy(() -> concat(flex1, flex2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("[17 - 17]");
        }

        @Test
        @DisplayName("with the right prefix overlapping the left main part")
        void concatRightPrefixMainOverlap() {
            // [4..6 - 10...12]
            //    [6.....11-12..14]
            // [4..6   -    12..14]
            var flex1 = RangeFlex.of(4, 6, 10, 12);
            var flex2 = RangeFlex.of(6, 11, 12, 14);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();
            var concat = concat(flex1, flex2);

            assertThat(concat).hasStartBetween(4, 6).hasEndBetween(12, 14);
            assertThat(IntStream.rangeClosed(4, 10)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(6, 14)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 15)).noneMatch(concat::contains);
            assertThat(apply(concat, "012345678901234567890"))
                    .isEqualTo("0123{[456]78901[234]}567890");
        }

        @Test
        @DisplayName("with the right prefix and suffix overlapping the left main part")
        void concatRightPrefixAndSuffixMainOverlap() {
            // [4..6  -  10...12]
            //    [6..8-9.......14]
            // [4..6   -X.......14]
            var flex1 = RangeFlex.of(4, 6, 10, 12);
            var flex2 = RangeFlex.of(6, 8, 9, 14);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isFalse();
            assertThatThrownBy(() -> concat(flex1, flex2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("[9 - 9]");
        }


        @Test
        @DisplayName("with the right prefix and suffix overlapping the left prefix")
        void concatRightPrefixAndSuffixLeftPrefixOverlap() {
            // [4....6 - 10...12]
            // [4..5-6.....11]
            // [4..5-X.....11]
            var flex1 = RangeFlex.of(4, 6, 10, 12);
            var flex2 = RangeFlex.of(4, 5, 6, 11);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isFalse();
            assertThatThrownBy(() -> concat(flex1, flex2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("[6 - 9]");
        }


        @Test
        @DisplayName("with minimum lengths")
        void concatAccountsForMinLength() {
            // [4..6  -  10...12]
            //     [7..9    -   13..15]
            // [4..6     -      13..15]
            var flex1 = RangeFlex.of(4, 6, 10, 12).withMinLength(5);
            var flex2 = RangeFlex.of(7, 9, 13, 15).withMinLength(5);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isTrue();

            System.out.println(flex1.lengthRange());
            System.out.println(flex2.lengthRange());
            var concat = concat(flex1, flex2);
            System.out.println(concat.lengthRange());

            assertThat(concat).hasStartBetween(4, 6).hasEndBetween(13, 15);
            assertThat(IntStream.rangeClosed(4, 13)).allMatch(concat::contains);
            assertThat(IntStream.rangeClosed(6, 15)).allMatch(concat::contains);
            assertThat(IntStream.of(3, 16)).noneMatch(concat::contains);
            assertThat(apply(concat, "01234567890123456789"))
                    .isEqualTo("0123{[456]789012[345]}6789");
            assertThat(concat.lengthRange()).isEqualTo(IntRange.ofClosed(10, 12));
        }

        @Test
        @DisplayName("throws when left length too small")
        void concatThrowsWhenLeftLengthIsTooSmall() {
            // [4..6  -  10...12]
            //     [7..9    -   13..15]
            // [4..6     -      13..15]
            var flex1 = RangeFlex.of(4, 6, 10, 12).withMinLength(7);
            var flex2 = RangeFlex.of(7, 9, 13, 15);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isFalse();
            assertThatThrownBy(() -> concat(flex1, flex2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("shorter than the minimum length of 7");

        }

        @Test
        @DisplayName("throws when right length too small")
        void concatThrowsWhenRightLengthIsTooSmall() {
            // [4..6  -  10...12]
            //     [7..9    -   13..15]
            // [4..6     -      13..15]
            var flex1 = RangeFlex.of(4, 6, 10, 12);
            var flex2 = RangeFlex.of(7, 9, 13, 15).withMinLength(7);

            assertThat(RangeFlex.canConcat(flex1, flex2)).isFalse();
            assertThatThrownBy(() -> concat(flex1, flex2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("shorter than the minimum length of 7");

        }
    }

    private static String apply(RangeFlex flex, String string) {
        System.out.println(flex);
        System.out.println(RangeFlexFormatter.defaultFormatter().format(flex, string));
        System.out.println(RangeFlexFormatter.UNDERLINE_ONLY.format(flex, string));
        return flex.applyTo(string);
    }
}
