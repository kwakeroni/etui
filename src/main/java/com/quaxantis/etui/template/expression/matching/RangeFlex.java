package com.quaxantis.etui.template.expression.matching;

import com.quaxantis.support.ide.API;
import com.quaxantis.support.util.Result;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public sealed interface RangeFlex {

    static RangeFlex empty() {
        return ofFixed(0, -1);
    }

    static RangeFlex of(int leftFrom, int leftToIncl, int rightFrom, int rightToIncl) {
        return of(IntRange.ofClosed(leftFrom, leftToIncl), IntRange.ofClosed(rightFrom, rightToIncl));
    }

    static RangeFlex of(IntRange left, IntRange right) {
        return new Simple(left, right);
    }

    static RangeFlex ofFixedStart(int from, int rightFrom, int rightToIncl) {
        return of(IntRange.of(from, from), IntRange.ofClosed(rightFrom, rightToIncl));
    }

    static RangeFlex ofFixedEnd(int leftFrom, int leftToIncl, int to) {
        return of(IntRange.ofClosed(leftFrom, leftToIncl), IntRange.of(to + 1, to + 1));
    }

    static RangeFlex ofFixed(int from, int to) {
        return of(IntRange.of(from, from), IntRange.of(to + 1, to + 1));
    }

    static RangeFlex ofComplete(String string) {
        IntRange completeRange = IntRange.of(0, string.length());
        return of(completeRange, completeRange);
    }

    static RangeFlex ofCompleteFixed(String string) {
        return ofFixed(0, string.length() - 1);
    }

    static RangeFlex expand(Iterable<? extends RangeFlex> ranges) {
        Iterator<? extends RangeFlex> iter = ranges.iterator();
        if (!iter.hasNext()) {
            throw new IllegalArgumentException("ranges cannot be empty");
        }
        RangeFlex result = iter.next();
        while (iter.hasNext()) {
            result = expand(result, iter.next());
        }
        return result;
    }

    static RangeFlex expand(RangeFlex range1, RangeFlex range2) {

        var first = Math.min(range1.start().from(), range2.start().from());
        var lastExcl = Math.max(range1.end().exclusiveTo(), range2.end().exclusiveTo());

        var prefixToExcl = lastExcl;
        var suffixFrom = first;

        var optMain1 = range1.main();
        var optMain2 = range2.main();
        if (optMain1.isPresent() && optMain2.isPresent()) {
            var main1 = optMain1.get();
            var main2 = optMain2.get();
            if (main1.contains(main2.from()) || main2.contains(main1.from())) {
                prefixToExcl = Math.max(main1.from(), main2.from());
                suffixFrom = Math.min(main1.exclusiveTo(), main2.exclusiveTo());
            }
        }

        IntRange prefix = IntRange.of(first, prefixToExcl);
        IntRange suffix = IntRange.of(suffixFrom, lastExcl);

        return RangeFlex.of(prefix, suffix);
    }

    record ConcatResult(RangeFlex left, RangeFlex right, RangeFlex combined) {}

    static Result<ConcatResult, RuntimeException> tryConcat(RangeFlex left, RangeFlex right) {
        return Result.ofTry(() -> concat(left, right));
    }

    static ConcatResult concat(RangeFlex left, RangeFlex right) {
        var leftConstrained = left.tryConstrain(Constraint.succeedBy(right))
                .orElseThrow(exc -> new IllegalArgumentException("Cannot concatenate flexes %s and %s: %s".formatted(left, right, exc.getMessage()), exc));
        var rightConstrained = right.constrain(Constraint.precedeBy(left));
        var minLength = leftConstrained.minLength().map(minLeft -> minLeft + rightConstrained.minLength().orElse(0))
                .orElse(rightConstrained.minLength().orElse(null));

        var maxLength = leftConstrained.maxLength().flatMap(maxLeft -> rightConstrained.maxLength().map(maxRight -> maxLeft + maxRight))
                .orElse(null);


        RangeFlex combined = new RangeFlex.Simple(leftConstrained.start(), rightConstrained.end(), minLength, maxLength);
        return new ConcatResult(leftConstrained, rightConstrained, combined);
    }

    IntRange start();

    IntRange end();

    Optional<Integer> minLength();

    Optional<Integer> maxLength();

    RangeFlex constrain(Constraint constraint);

    default Result<? extends RangeFlex, RuntimeException> tryConstrain(Constraint constraint) {
        return Result.ofTry(() -> constrain(constraint));
    }

    @SuppressWarnings("ConfusingMainMethod")
    default Optional<IntRange> main() {
        if (start().to() < end().from()) {
            return Optional.of(IntRange.of(start().exclusiveTo(), end().from()));
        } else {
            return Optional.empty();
        }
    }

    default boolean contains(int i) {
        return i >= start().from() && i < end().exclusiveTo();
    }

    default IntRange lengthRange() {

        int minRange = end().from() - start().exclusiveTo();
        int maxRange = end().exclusiveTo() - start().from();

        int minimum = this.minLength().map(min -> Math.max(min, minRange)).orElse(minRange);
        int maximum = this.maxLength().map(max -> Math.min(max, maxRange)).orElse(maxRange);
        return IntRange.ofClosed(minimum, maximum);
    }

    default boolean isEmpty() {
        IntRange lengthRange = lengthRange();
        return lengthRange.from() == lengthRange.to();
    }

    default Extract extractFrom(String string) {
        // TODO what about fuzzy strings and other length constraints ?
        if (maxLength().filter(max -> max == 0).isPresent()) {
            return new Extract("", "", "");
        } else if (start().from() <= end().exclusiveTo()) {
            if (start().exclusiveTo() <= end().from()) {
                return new Extract(
                        string.substring(start().from(), start().exclusiveTo()),
                        string.substring(start().exclusiveTo(), end().from()),
                        string.substring(end().from(), end().exclusiveTo())
                );
            } else {
                // Overlap
                return new Extract(
                        string.substring(start().from(), start().exclusiveTo()),
                        "",
                        string.substring(end().from(), end().exclusiveTo())
                );
            }
        } else {
            throw new IllegalStateException("Cannot extract valid string from range: " + applyTo(string));
        }
    }

    default String extractMaxFrom(String string) {
        // TODO what about fuzzy strings and other length constraints ?

        if (maxLength().filter(max -> max == 0).isPresent()) {
            return "";
        } else if (start().from() <= end().exclusiveTo()) {
            return string.substring(start().from(), end().exclusiveTo());
        } else {
            throw new IllegalStateException("Cannot extract valid string from range: " + applyTo(string));
        }
    }

    default Applied applyTo(String string) {
        return new Applied(string, this);
    }

    default String format(String string) {
        return format(string, RangeFlexFormatter.defaultFormatter());
    }

    default String format(String string, RangeFlexFormatter formatter) {
        return formatter.format(this, string);
    }

    private static String toString(IntRange start, IntRange end) {
        return toString(start, end, null, null);
    }

    private static String toString(IntRange start, IntRange end, Integer minLength, Integer maxLength) {
        double log10 = Math.log10(Math.max(start.to(), end.to()));
        int d = Double.isFinite(log10) ? ((int) log10) + 1 : -1;
        String rangePattern = (d > 0) ? "%" + d + "d-%" + d + "d" : "%s..%s";
        String singletonPattern = (d > 0) ? "%" + (d * 2 + 1) + "d" : "%s";
        String empty = (d > 0) ? " ".repeat(d * 2 + 1) : "";
        String leftString = (start.from() == start.to() ? singletonPattern : (start.from() < start.to()) ? rangePattern : empty)
                .formatted(start.from(), start.to());
        String midString = ((start.to() + 1 == end.from() - 1) ? singletonPattern : (start.to() + 1 < end.from() - 1) ? rangePattern : empty).formatted(start.to() + 1, end.from() - 1);
        String midSpace = (midString.isEmpty()) ? "" : " ";
        String rightString = (end.from() == end.to() ? singletonPattern : (end.from() < end.to()) ? rangePattern : empty).formatted(end.from(), end.to());
        String minLengthString = lengthString(minLength, maxLength);
        return "{[" + leftString + "]" + midSpace + midString + midSpace + "[" + rightString + "]" + minLengthString + "}";
    }

    default String lengthString() {
        return lengthString(this.minLength().orElse(null), this.maxLength().orElse(null));
    }

    private static String lengthString(Integer minLength, Integer maxLength) {
        return (minLength == null && maxLength == null) ? "" : ("!" + Objects.toString(minLength, "") + "<" + Objects.toString(maxLength, ""));
    }


    private static <R> R verify(R range, Consumer<? super R> verification) {
        verification.accept(range);
        return range;
    }


    record Simple(IntRange start, IntRange end, Integer minLengthValue, Integer maxLengthValue) implements RangeFlex {

        public Simple {
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            verifyLengthConstraints(start, end, minLengthValue, maxLengthValue).orElseThrow();
        }

        Simple(IntRange start, IntRange end) {
            this(start, end, null, null);
        }

        @Override
        public Optional<Integer> minLength() {
            return Optional.ofNullable(minLengthValue);
        }

        @Override
        public Optional<Integer> maxLength() {
            return Optional.ofNullable(maxLengthValue);
        }

        public RangeFlex constrain(Constraint constraint) {
            return switch (constraint) {
                case Constraint.MinLength(int min) -> constrainMinLength(min);
                case Constraint.MaxLength(int max) -> constrainMaxLength(max);
                case Constraint.LimitLeft(int left) -> limitLeft(left);
                case Constraint.LimitRight(int right) -> limitRight(right);
                case Constraint.RangeLimit(RangeFlex range) -> constrainToRange(range);
                case Constraint.FixedRange _ -> toFixedRange();
                case Constraint.FixPrefix _ -> fixPrefix();
                case Constraint.FixSuffix _ -> fixSuffix();
                case Constraint.PrecedeBy(var predecessor) -> constrainWithPredecessor(predecessor);
                case Constraint.SucceedBy(var successor) -> constrainWithSuccessor(successor);
                case Constraint.Mapping(var operator) -> operator.apply(this);
                case Constraint.Satisfying(var verification) -> verify(this, verification);
                case Constraint.And(var one, var two) -> constrain(one).constrain(two);
            };
        }

        private RangeFlex toFixedRange() {
            int from = start().from();
            int to = end.to();
            return new Simple(IntRange.of(from, from), IntRange.of(to + 1, to + 1));
        }

        private RangeFlex fixPrefix() {
            int from = start.from();
            return new Simple(IntRange.of(from, from), end);
        }

        private RangeFlex fixSuffix() {
            int to = end.to();
            return new Simple(start, IntRange.of(to + 1, to + 1));
        }

        private RangeFlex constrainMinLength(Integer minLength) {
            return new Simple(start, end, (this.minLengthValue == null) ? minLength : Math.max(this.minLengthValue, minLength), this.maxLengthValue);
        }

        private Simple constrainMaxLength(int maxLength) {
            return new Simple(start, end, this.minLengthValue, (this.maxLengthValue == null) ? maxLength : Math.min(this.maxLengthValue, maxLength));
        }

        private RangeFlex limitLeft(int limit) {
            IntRange limitStart = start().limitLeft(limit);
            IntRange limitEnd = end().limitLeft(limit);
            return new RangeFlex.Simple(limitStart, limitEnd, minLengthValue(), maxLengthValue());
        }

        private RangeFlex limitRight(int limit) {
            IntRange limitStart = start().limitRight(limit);
            IntRange limitEnd = end().limitRight(limit);
            return new RangeFlex.Simple(limitStart, limitEnd, minLengthValue(), maxLengthValue());
        }

        private RangeFlex constrainToRange(RangeFlex constraint) {
            int left = constraint.start().from();
            int right = constraint.end().to();
            IntRange limitStart = start().limitLeft(left).limitRight(right);
            IntRange limitEnd = end().limitLeft(left).limitRight(right);
            return new RangeFlex.Simple(limitStart, limitEnd, minLengthValue(), maxLengthValue());
        }

        private RangeFlex constrainWithSuccessor(RangeFlex successor) {
            return tryConstrain(Constraint.limitRight(successor.start().to()))
                    .orElseThrow(cause -> {
                        // TODO: this could be nicer
                        if (cause.getStackTrace()[0].getClassName().equals(IntRange.class.getName())) {
                            return new IllegalArgumentException("incompatible overlap at [%s - %s]".formatted(successor.start().to() + 1, this.end().from() - 1), cause);
                        }
                        return cause;

                    })
                    .tryConstrain(Constraint.satisfying(range -> {
                        if (range.end().exclusiveTo() < successor.start().from()) {
                            throw new IllegalArgumentException("gap at [%s - %s]".formatted(range.end().to() + 1, successor.start().from() - 1));
                        }
                    }))
                    .orElseThrow()
                    .tryConstrain(Constraint.mapping(range -> range.maxLength()
                            .map(maxLength -> range.constrain(Constraint.limitLeft(successor.start().from() - maxLength)))
                            .orElse(range)))
                    .orElseThrow();
        }

        private RangeFlex constrainWithPredecessor(RangeFlex predecessor) {
            // TODO Compared with constrainWithSuccessor it feels like some tests are missing here


            return constrain(
                    Constraint.limitLeft(predecessor.end().from()).and(
                            Constraint.mapping(range -> range.maxLength()
                                    .map(maxLength -> range.constrain(Constraint.limitRight(predecessor.end().exclusiveTo() + maxLength - 1)))
                                    .orElse(range)
                            )));
        }

        @Override
        public String toString() {
            return RangeFlex.toString(start, end, minLengthValue, maxLengthValue);
        }

    }

    private static Result<Void, RuntimeException> verifyLengthConstraints(IntRange start, IntRange end, Integer minLength, Integer maxLength) {
        int lowestLength = end.from() - start.exclusiveTo();
        int highestLength = end.exclusiveTo() - start.from();
        if (minLength != null && minLength > highestLength) {
            return Result.ofFailure(new IllegalArgumentException("Range %s with highest length of %d is strictly shorter than the minimum length of %d".formatted(RangeFlex.toString(start, end), highestLength, minLength)));
        } else if (maxLength != null && maxLength < lowestLength) {
            return Result.ofFailure(new IllegalArgumentException("Range %s with lowest length of %d is strictly longer than the maximum length of %d".formatted(RangeFlex.toString(start, end), lowestLength, maxLength)));
        } else if (minLength != null && maxLength != null && minLength > maxLength) {
            return Result.ofFailure(new IllegalArgumentException("Range %s cannot have length constraints where minimum length of %d is greater than maximum length of %d".formatted(RangeFlex.toString(start, end), minLength, maxLength)));
        } else {
            return Result.ofVoid();
        }
    }

    record Applied(@Nonnull String string, @Nonnull RangeFlex range) {

        public static Applied ofComplete(String string) {
            return new Applied(string, RangeFlex.ofComplete(string));
        }

        public static Applied ofCompleteFixed(String string) {
            return new Applied(string, RangeFlex.ofCompleteFixed(string));
        }

        public String format() {
            return range.format(string);
        }

        public String format(RangeFlexFormatter formatter) {
            return range.format(string, formatter);
        }

        public Extract extract() {
            return range.extractFrom(string);
        }

        public String extractMax() {
            return range.extractMaxFrom(string);
        }

        @Override
        public String toString() {
            return range.format(string, RangeFlexFormatter.SEPARATORS_ONLY);
        }

        public static Optional<RangeFlex.Applied> merge(RangeFlex.Applied one, RangeFlex.Applied two) {
            if (one.range().isEmpty() && two.range().isEmpty()) {
                return Optional.of(RangeFlex.empty().applyTo(""));
            }
            RangeFlex.Extract extract1 = one.extract();
            RangeFlex.Extract extract2 = two.extract();
            if (extract1.main().isEmpty() && isFullFlex(one.range())) {
                if (extract2.main().isEmpty() && isFullFlex(two.range())) {
                    return mergeFullFlex(extract1, extract2);
                } else if (!extract2.main().isEmpty()) {
                    return mergeMainWithFullFlex(extract2, extract1);
                }
            } else if (extract2.main().isEmpty() && isFullFlex(two.range())) {
                if (!extract1.main().isEmpty()) {
                    return mergeMainWithFullFlex(extract1, extract2);
                }
            } else {
                return mergeExtractsWithMain(extract1, extract2);
            }

            return Optional.empty();
        }

        private static Optional<RangeFlex.Applied> mergeMainWithFullFlex(RangeFlex.Extract mainFlex, RangeFlex.Extract fullFlex) {
            // fullFlex.prefix equals fullFlex.suffix
            String full = fullFlex.prefix();
            String main = mainFlex.main();
            int index = full.indexOf(main);
            var optResult = Optional.<RangeFlex.Applied>empty();
            while (index >= 0) {
                Extract newFullFlex = new Extract(full.substring(0, index), full.substring(index, index + main.length()), full.substring(index + main.length()));
                var newResult = mergeExtractsWithContainedMain(newFullFlex, mainFlex);
                optResult = optResult.filter(r -> r.extractMax().length() >= newResult.map(nr -> nr.extractMax().length()).orElse(-1)).or(() -> newResult);
                index = full.indexOf(main, index + 1);
            }

            return optResult;
        }

        private static Optional<RangeFlex.Applied> mergeExtractsWithMain(RangeFlex.Extract extract1, RangeFlex.Extract extract2) {
            if (extract1.main().contains(extract2.main())) {
                return mergeExtractsWithContainedMain(extract1, extract2);
            } else if (extract2.main().contains(extract1.main())) {
                return mergeExtractsWithContainedMain(extract2, extract1);
            }
            return Optional.empty();
        }

        private static Optional<RangeFlex.Applied> mergeExtractsWithContainedMain(RangeFlex.Extract bigExtract, RangeFlex.Extract smallExtract) {
            // big.main contains small.main
            String bigMain = bigExtract.main();
            String smallMain = smallExtract.main();

            // Expand the small main range into its prefix to match the big main range
            String smallPrefix = smallExtract.prefix();
            int smallPrefixToMain = 0;
            for (int i = smallPrefix.length() - 1, j = bigMain.indexOf(smallMain) - 1; j >= 0; i--, j--) {
                if (i >= 0 && bigMain.charAt(j) == smallPrefix.charAt(i)) {
                    smallPrefixToMain++;
                } else {
                    // No match in the main range: cannot merge
                    return Optional.empty();
                }
            }

            // Expand the prefix as long as it matches
            String bigPrefix = bigExtract.prefix();
            int keepSmallPrefix = 0;
            for (int i = smallPrefix.length() - 1 - smallPrefixToMain, j = bigPrefix.length() - 1; i >= 0 && j >= 0; i--, j--) {
                if (bigPrefix.charAt(j) == smallPrefix.charAt(i)) {
                    keepSmallPrefix++;
                } else {
                    break;
                }
            }

            // Expand the small main range into its suffix to match the big main range
            String smallSuffix = smallExtract.suffix();
            int smallSuffixToMain = 0;
            for (int i = 0, j = bigMain.indexOf(smallMain) + smallMain.length(); j < bigMain.length(); i++, j++) {
                if (i < smallSuffix.length() && bigMain.charAt(j) == smallSuffix.charAt(i)) {
                    smallSuffixToMain++;
                } else {
                    // No match in the main range: cannot merge
                    return Optional.empty();
                }
            }

            // Expand the suffix as long as it matches
            String bigSuffix = bigExtract.suffix();
            int keepSmallSuffix = 0;
            for (int i = smallSuffixToMain, j = 0; i < smallSuffix.length() && j < bigSuffix.length(); i++, j++) {
                if (bigSuffix.charAt(j) == smallSuffix.charAt(i)) {
                    keepSmallSuffix++;
                } else {
                    break;
                }
            }

            String newPrefix = smallPrefix.substring(smallPrefix.length() - keepSmallPrefix - smallPrefixToMain, smallPrefix.length() - smallPrefixToMain);
            String newMain = smallPrefix.substring(smallPrefix.length() - smallPrefixToMain) + smallMain + smallSuffix.substring(0, smallSuffixToMain);
            String newSuffix = smallSuffix.substring(smallSuffixToMain, smallSuffixToMain + keepSmallSuffix);
            if (!newMain.equals(bigMain)) {
                throw new IllegalStateException("Unexpected main mismatch when merging ranges. " + newMain + " <> " + bigMain);
            }

            IntRange newPrefixRange = IntRange.of(0, newPrefix.length());
            IntRange newSuffixRange = IntRange.of(newPrefix.length() + newMain.length(), newPrefix.length() + newMain.length() + newSuffix.length());
            return Optional.of(new Applied(newPrefix + newMain + newSuffix, RangeFlex.of(newPrefixRange, newSuffixRange)));
        }

        private static boolean isFullFlex(RangeFlex rangeFlex) {
            return rangeFlex.start().equals(rangeFlex.end());
        }

        private static Optional<RangeFlex.Applied> mergeFullFlex(RangeFlex.Extract extract1, RangeFlex.Extract extract2) {
            String fullOverlap = null;
            if (extract1.prefix().contains(extract2.prefix())) {
                fullOverlap = extract2.prefix();
            } else if (extract2.prefix().contains(extract1.prefix())) {
                fullOverlap = extract1.prefix();
            }
            return Optional.ofNullable(fullOverlap).map(Applied::ofComplete);
        }
    }

    record Extract(@Nonnull String prefix, @Nonnull String main, @Nonnull String suffix) {
        @API
        public String fullString() {
            return prefix + main + suffix;
        }
    }
}
