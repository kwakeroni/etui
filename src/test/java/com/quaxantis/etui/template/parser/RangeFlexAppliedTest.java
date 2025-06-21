package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RangeFlexAppliedTest {

    public static final String FULLSTRING = "01234567890123456789";

    @Nested
    class Merge {

        @Test
        void mergeRangesWithMainMismatch() {
            RangeFlex range1 = RangeFlex.of(2, 6, 14, 18);
            RangeFlex range2 = RangeFlex.of(2, 8, 12, 18);
            var result = merge(range1.applyTo(FULLSTRING), range2.applyTo(FULLSTRING.replaceFirst("9", "X")));
            assertThat(result).isEmpty();
        }

        @Test
        void mergeRangesWithExactMatch() {
            RangeFlex range1 = RangeFlex.of(2, 6, 14, 18);
            RangeFlex range2 = RangeFlex.of(2, 8, 12, 18);
            var result = merge(range1, range2);
            assertThat(result).hasValueSatisfying(
                    merged -> assertThat(merged.extract())
                            .returns("23456", RangeFlex.Extract::prefix)
                            .returns("7890123", RangeFlex.Extract::main)
                            .returns("45678", RangeFlex.Extract::suffix)
            );
        }

        @Test
        void mergeRangesWithSmallPrefixMismatch() {
            RangeFlex range1 = RangeFlex.of(2, 6, 14, 18);
            RangeFlex range2 = RangeFlex.of(2, 8, 12, 18);
            var result = merge(range1.applyTo(FULLSTRING), range2.applyTo(FULLSTRING.replaceFirst("7", "X")));
            assertThat(result).isEmpty();
        }

        @Test
        void mergeRangesWithSmallPrefixTooSmall() {
            RangeFlex range1 = RangeFlex.of(2, 6, 14, 18);
            RangeFlex range2 = RangeFlex.of(8, 8, 12, 18);
            var result = merge(range1, range2);
            assertThat(result).isEmpty();
        }

        @Test
        void mergeRangesWithSmallSuffixTooSmall() {
            RangeFlex range1 = RangeFlex.of(2, 6, 14, 18);
            RangeFlex range2 = RangeFlex.of(2, 8, 12, 12);
            var result = merge(range1, range2);
            assertThat(result).isEmpty();
        }


        @Test
        void mergeRangesWithSmallSuffixMismatch() {
            RangeFlex range1 = RangeFlex.of(2, 6, 14, 18);
            RangeFlex range2 = RangeFlex.of(2, 8, 12, 18);
            var result = merge(range1.applyTo(FULLSTRING), range2.applyTo(FULLSTRING.replace("3", "X")));
            assertThat(result).isEmpty();
        }

        @Test
        void mergeRangesWithSmallerSmallPrefix() {
            RangeFlex range1 = RangeFlex.of(2, 6, 14, 18);
            RangeFlex range2 = RangeFlex.of(4, 8, 12, 18);
            var result = merge(range1, range2);
            assertThat(result).hasValueSatisfying(
                    merged -> assertThat(merged.extract())
                            .returns("456", RangeFlex.Extract::prefix)
                            .returns("7890123", RangeFlex.Extract::main)
                            .returns("45678", RangeFlex.Extract::suffix)
            );
        }

        @Test
        void mergeRangesWithSmallerBigPrefix() {
            RangeFlex range1 = RangeFlex.of(4, 6, 14, 18);
            RangeFlex range2 = RangeFlex.of(2, 8, 12, 18);
            var result = merge(range1, range2);
            assertThat(result).hasValueSatisfying(
                    merged -> assertThat(merged.extract())
                            .returns("456", RangeFlex.Extract::prefix)
                            .returns("7890123", RangeFlex.Extract::main)
                            .returns("45678", RangeFlex.Extract::suffix)
            );
        }

        @Test
        void mergeRangesWithSmallerSmallSuffix() {
            RangeFlex range1 = RangeFlex.of(2, 6, 14, 18);
            RangeFlex range2 = RangeFlex.of(2, 8, 12, 16);
            var result = merge(range1, range2);
            assertThat(result).hasValueSatisfying(
                    merged -> assertThat(merged.extract())
                            .returns("23456", RangeFlex.Extract::prefix)
                            .returns("7890123", RangeFlex.Extract::main)
                            .returns("456", RangeFlex.Extract::suffix)
            );
        }

        @Test
        void mergeRangesWithSmallerBigSuffix() {
            RangeFlex range1 = RangeFlex.of(2, 6, 14, 16);
            RangeFlex range2 = RangeFlex.of(2, 8, 12, 18);
            var result = merge(range1, range2);
            assertThat(result).hasValueSatisfying(
                    merged -> assertThat(merged.extract())
                            .returns("23456", RangeFlex.Extract::prefix)
                            .returns("7890123", RangeFlex.Extract::main)
                            .returns("456", RangeFlex.Extract::suffix)
            );
        }

        @Test
        void mergeRangeWithoutMainAndSuffixOverlap() {
            RangeFlex range1 = RangeFlex.of(5, 9, 5, 9);
            RangeFlex range2 = RangeFlex.of(0, 9, 0, 9);
            var result = merge(range1, range2, "0123456789");
            assertThat(result).hasValueSatisfying(
                    merged -> {
                        assertThat(merged.extractMax()).isEqualTo("56789");
                        assertThat(merged.extract())
                                .returns("56789", RangeFlex.Extract::prefix)
                                .returns("", RangeFlex.Extract::main)
                                .returns("56789", RangeFlex.Extract::suffix);
                    });
        }

        @Test
        void mergeRangeWithoutMainAndPrefixOverlap() {
            RangeFlex range1 = RangeFlex.of(0, 5, 0, 5);
            RangeFlex range2 = RangeFlex.of(0, 9, 0, 9);
            var result = merge(range1, range2, "0123456789");
            assertThat(result).hasValueSatisfying(
                    merged -> {
                        assertThat(merged.extractMax()).isEqualTo("012345");
                        assertThat(merged.extract())
                                .returns("012345", RangeFlex.Extract::prefix)
                                .returns("", RangeFlex.Extract::main)
                                .returns("012345", RangeFlex.Extract::suffix);
                    });
        }

        @Test
        void mergeRangeWithoutMainAndPartialPrefixOverlap() {
            RangeFlex range1 = RangeFlex.of(0, 5, 0, 5).constrain(Constraint.toMinLength(1));
            RangeFlex range2 = RangeFlex.of(0, 11, 0, 11).constrain(Constraint.toMinLength(1));
            var result = merge(range1, range2, "prefixsuffix");
            System.out.println(result);
            assertThat(result).hasValueSatisfying(
                    merged -> {
                        assertThat(merged.extractMax()).isEqualTo("prefix");
                        assertThat(merged.extract())
                                .returns("prefix", RangeFlex.Extract::prefix)
                                .returns("", RangeFlex.Extract::main)
                                .returns("prefix", RangeFlex.Extract::suffix);
                    });
        }


        static Optional<RangeFlex.Applied> merge(RangeFlex one, RangeFlex two) {
            return merge(one, two, FULLSTRING);
        }

        static Optional<RangeFlex.Applied> merge(RangeFlex one, RangeFlex two, String fullstring) {
            return merge(one.applyTo(fullstring), two.applyTo(fullstring));
        }

        static Optional<RangeFlex.Applied> merge(RangeFlex.Applied one, RangeFlex.Applied two) {
            System.out.println(one.format());
            System.out.println(two.format());
            var result = RangeFlex.Applied.merge(one, two);
            result.ifPresentOrElse(merged -> System.out.println(merged.format()), () -> System.out.println(Optional.empty()));
            return result;
        }

    }

}
