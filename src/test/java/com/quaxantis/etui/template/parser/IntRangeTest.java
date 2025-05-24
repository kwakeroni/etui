package com.quaxantis.etui.template.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IntRange")
class IntRangeTest {

    @Nested
    @DisplayName("Can create a range")
    class NewIntRange {
        @Test
        @DisplayName("from an int to an int")
        void newIntRange() {
            IntRange range = IntRange.ofClosed(7, 17);
            assertThat(range.from()).isEqualTo(7);
            assertThat(range.to()).isEqualTo(17);
            assertThat(range.exclusiveTo()).isEqualTo(18);
            assertThat(range.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("that is empty")
        void newIntRangeEmpty() {
            IntRange range = IntRange.ofClosed(18, 17);
            assertThat(range.from()).isEqualTo(18);
            assertThat(range.to()).isEqualTo(17);
            assertThat(range.exclusiveTo()).isEqualTo(18);
            assertThat(range.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("throws when from > to")
        void newIntRangeWithFromGreaterThanTo() {
            assertThatThrownBy(() -> IntRange.of(18, 7))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("using IntRange.of")
        void of() {
            IntRange range = IntRange.ofClosed(7, 17);
            assertThat(range.from()).isEqualTo(7);
            assertThat(range.to()).isEqualTo(17);
            assertThat(range.exclusiveTo()).isEqualTo(18);
        }

        @Test
        @DisplayName("using IntRange.ofClosed")
        void ofClosed() {
            IntRange range = IntRange.ofClosed(7, 18);
            assertThat(range.from()).isEqualTo(7);
            assertThat(range.to()).isEqualTo(18);
            assertThat(range.exclusiveTo()).isEqualTo(19);
        }

    }

    @Nested
    @DisplayName("Can truncate the right side")
    class TruncateRight {

        @Test
        @DisplayName("without change if there is no overlap")
        void noChangeWithoutOverlap() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateRight(IntRange.of(20, 25));
            assertThat(truncated).isEqualTo(IntRange.ofClosed(7, 17));
        }

        @Test
        @DisplayName("moving the end to the left if there is an overlap")
        void movingEndToLeft() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateRight(IntRange.of(15, 25));
            assertThat(truncated).isEqualTo(IntRange.of(7, 15));
        }

        @Test
        @DisplayName("collapsing to empty range if overlap is complete")
        void toEmptyRange() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateRight(IntRange.of(4, 25));
            assertThat(truncated).isEqualTo(IntRange.of(7, 7));
        }

        @Test
        @DisplayName("with a remaining overlap when requested")
        void withRemainingOverlap() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateRight(IntRange.of(15, 25), 2);
            assertThat(truncated).isEqualTo(IntRange.of(7, 17));
        }

        @Test
        @DisplayName("with a partial remaining overlap when requested")
        void withPartialRemainingOverlap() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateRight(IntRange.of(10, 25), 10);
            assertThat(truncated).isEqualTo(IntRange.ofClosed(7, 17));
        }

        @Test
        @DisplayName("with a no remaining overlap if there is no overlap")
        void withNoRemainingOverlapIfNoOverlap() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateRight(IntRange.of(18, 25), 2);
            assertThat(truncated).isEqualTo(IntRange.ofClosed(7, 17));
        }

        @Test
        @DisplayName("collapsing empty range without remaining overlap if overlap is complete")
        void toRemainingOverlapOnly() {
            // There's a choice to be made here.
            // Either we consider the remaining overlap in the starting range, yielding (7, 9(.
            // Or we consider it in the truncating range, in which case it concerns (4, 6(
            // and the result is still empty.
            // For new we choose the latter option.
            IntRange truncated = IntRange.ofClosed(7, 17).truncateRight(IntRange.ofClosed(4, 24), 2);
            assertThat(truncated).isEqualTo(IntRange.of(7, 7));
        }
    }

    @Nested
    @DisplayName("Can truncate the left side")
    class TruncateLeft {

        @Test
        @DisplayName("without change if there is no overlap")
        void noChangeWithoutOverlap() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateLeft(IntRange.ofClosed(2, 6));
            assertThat(truncated).isEqualTo(IntRange.ofClosed(7, 17));
        }

        @Test
        @DisplayName("moving the start to the right if there is an overlap")
        void movingEndToLeft() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateLeft(IntRange.ofClosed(2, 8));
            assertThat(truncated).isEqualTo(IntRange.ofClosed(9, 17));
        }


        @Test
        @DisplayName("collapsing to empty range if overlap is complete")
        void toEmptyRange() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateLeft(IntRange.ofClosed(2, 24));
            assertThat(truncated).isEqualTo(IntRange.of(18, 18));
        }


        @Test
        @DisplayName("with a remaining overlap when requested")
        void withRemainingOverlap() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateLeft(IntRange.ofClosed(2, 11), 2);
            assertThat(truncated).isEqualTo(IntRange.ofClosed(10, 17));
        }

        @Test
        @DisplayName("with a partial remaining overlap when requested")
        void withPartialRemainingOverlap() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateLeft(IntRange.ofClosed(2, 14), 10);
            assertThat(truncated).isEqualTo(IntRange.ofClosed(7, 17));
        }

        @Test
        @DisplayName("with a no remaining overlap if there is no overlap")
        void withNoRemainingOverlapIfNoOverlap() {
            IntRange truncated = IntRange.ofClosed(7, 17).truncateLeft(IntRange.ofClosed(2, 6), 2);
            assertThat(truncated).isEqualTo(IntRange.ofClosed(7, 17));
        }

        @Test
        @DisplayName("collapsing empty range without remaining overlap if overlap is complete")
        void toRemainingOverlapOnly() {
            // There's a choice to be made here.
            // Either we consider the remaining overlap in the starting range, yielding (16, 18(.
            // Or we consider it in the truncating range, in which case it concerns (23, 25(
            // and the result is still empty.
            // For new we choose the latter option.
            IntRange truncated = IntRange.ofClosed(7, 17).truncateLeft(IntRange.ofClosed(2, 24), 2);
            assertThat(truncated).isEqualTo(IntRange.ofClosed(18, 17));
        }
    }

    @Nested
    @DisplayName("Can clamp the right side")
    class ClampRight {

    }
}
