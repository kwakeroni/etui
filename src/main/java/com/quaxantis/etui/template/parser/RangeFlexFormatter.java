package com.quaxantis.etui.template.parser;

import com.quaxantis.support.util.ANSI;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Map.entry;

public interface RangeFlexFormatter {

    static RangeFlexFormatter defaultFormatter() {
        return withDefaultFormatter(UnaryOperator.identity());
    }

    static void setDefaultFormatter(RangeFlexFormatter formatter) {
        withDefaultFormatter(_ -> formatter);
    }

    private static RangeFlexFormatter withDefaultFormatter(UnaryOperator<RangeFlexFormatter> formatter) {
        class Default {
            private static final AtomicReference<RangeFlexFormatter> VALUE = new AtomicReference<>(COLORS_ONLY);
        }
        return Default.VALUE.updateAndGet(formatter);
    }

    default String format(RangeFlex.Applied appliedRangeFlex) {
        return format(appliedRangeFlex.range(), appliedRangeFlex.string());
    }

    String format(RangeFlex rangeFlex, String string);


    RangeFlexFormatter SEPARATORS_ONLY = new WithAffixes("SEPARATORS_ONLY", "", "{", "{", "", "[", "]", "", "", "[", "]", "", "}", "", "", "", "", "");
    RangeFlexFormatter SEPARATORS_COLORS = new WithAffixes("SEPARATORS_COLORS", ANSI.WHITE, ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND + "{", ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND + "{", ANSI.UNDERLINED, "[" + ANSI.WHITE_BRIGHT + ANSI.GREEN_BACKGROUND, ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND + "]" + ANSI.WHITE, ANSI.WHITE_BRIGHT + ANSI.BOLD + ANSI.DOUBLE_UNDERLINED, ANSI.DEFAULT_COLOR + ANSI.NOT_BOLD + ANSI.UNDERLINED, ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND + "[" + ANSI.WHITE_BRIGHT + ANSI.BLUE_BACKGROUND, ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND + "]", ANSI.NOT_UNDERLINED, "}" + ANSI.WHITE, ANSI.RESET, ANSI.CYAN_BACKGROUND, ANSI.DEFAULT_BACKGROUND, ANSI.WHITE_BRIGHT + ANSI.GREEN_BACKGROUND, ANSI.WHITE_BRIGHT + ANSI.BLUE_BACKGROUND);
    RangeFlexFormatter COLORS_ONLY = new WithAffixes("COLORS_ONLY", ANSI.WHITE, ANSI.DEFAULT_COLOR, ANSI.DEFAULT_COLOR, ANSI.UNDERLINED, ANSI.WHITE_BRIGHT + ANSI.GREEN_BACKGROUND, ANSI.WHITE + ANSI.DEFAULT_BACKGROUND, ANSI.WHITE_BRIGHT + ANSI.BOLD + ANSI.DOUBLE_UNDERLINED, ANSI.DEFAULT_COLOR + ANSI.NOT_BOLD + ANSI.UNDERLINED, ANSI.WHITE_BRIGHT + ANSI.BLUE_BACKGROUND, ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND, ANSI.NOT_UNDERLINED, ANSI.WHITE, ANSI.RESET, ANSI.CYAN_BACKGROUND, ANSI.DEFAULT_BACKGROUND, ANSI.WHITE_BRIGHT + ANSI.GREEN_BACKGROUND, ANSI.WHITE_BRIGHT + ANSI.BLUE_BACKGROUND);
    RangeFlexFormatter UNDERLINE_ONLY = new WithIncludes("UNDERLINE_ONLY", ANSI.UNDERLINED, ANSI.NOT_UNDERLINED);
    RangeFlexFormatter AS_CODE = Fixed.AS_CODE;

    enum Fixed implements RangeFlexFormatter {
        AS_CODE {
            @Override
            public String format(RangeFlex rangeFlex, String string) {
                StringBuilder result = new StringBuilder("RangeFlex.of(%d, %d, %d, %d)"
                                                                 .formatted(rangeFlex.start().from(), rangeFlex.start().to(), rangeFlex.end().from(), rangeFlex.end().to()));
                rangeFlex.minLength().ifPresent(minLength -> result.append(".constrain(Constraint.toMinLength(%d))".formatted(minLength)));
                rangeFlex.maxLength().ifPresent(maxLength -> result.append(".constrain(Constraint.toMaxLength(%d))".formatted(maxLength)));
                result.append(".applyTo(\"%s\")".formatted(string));
                return result.toString();
            }
        }
    }

    record WithIncludes(String name, String startIncluded, String endIncluded) implements RangeFlexFormatter {
        @Override
        public String format(RangeFlex rangeFlex, String string) {
            StringBuilder builder = new StringBuilder();
            boolean contains = false;
            for (int i = 0; i < string.length(); i++) {
                boolean contains_i = rangeFlex.contains(i);
                if (contains ^ contains_i) {
                    builder.append((contains) ? endIncluded : startIncluded);
                    contains = contains_i;
                }
                builder.append(string.charAt(i));
            }
            if (contains) {
                builder.append(endIncluded);
            }
            return builder.toString();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    record WithAffixes(
            String name,
            String startGlobal,
            String startRange,
            String startRangeAfterRangeEnd,
            String startMaxAllowed,
            String startPrefix,
            String endPrefix,
            String startMain,
            String endMain,
            String startSuffix,
            String endSuffix,
            String endMaxAllowed,
            String endRange,
            String endGlobal,
            String startAffixOverlap,
            String endAffixOverlap,
            String continuePrefixAfterOverlap,
            String continueSuffixAfterOverlap
    ) implements RangeFlexFormatter {
        public String format(RangeFlex rangeFlex, String string) {
            StringBuilder result = new StringBuilder();
            result.append(startGlobal);
            if (rangeFlex.start().from() <= rangeFlex.end().from()) {
                result.append(string.substring(0, rangeFlex.start().from()));
                result.append(startRange).append(startPrefix);

                if (rangeFlex.start().exclusiveTo() <= rangeFlex.end().from()) {
                    int mainLength = rangeFlex.end().from() - rangeFlex.start().exclusiveTo();
                    Optional<Integer> fuzzy = rangeFlex.maxLength().map(max -> max - mainLength); // TODO what if negative ?
                    fuzzy.map(f -> Math.min(rangeFlex.start().length(), f)).ifPresentOrElse(
                            f -> result.append(string.substring(rangeFlex.start().from(), rangeFlex.start().exclusiveTo() - f))
                                    .append(startMaxAllowed)
                                    .append(string.substring(rangeFlex.start().exclusiveTo() - f, rangeFlex.start().exclusiveTo())),
                            () -> result.append(startMaxAllowed)
                                    .append(string.substring(rangeFlex.start().from(), rangeFlex.start().exclusiveTo()))
                    );
                    result.append(endPrefix)
                            .append(startMain)
                            .append(string.substring(rangeFlex.start().exclusiveTo(), rangeFlex.end().from()))
                            .append(endMain)
                            .append(startSuffix);
                    fuzzy.map(f -> Math.min(rangeFlex.end().length(), f)).ifPresentOrElse(
                            f -> result.append(string.substring(rangeFlex.end().from(), rangeFlex.end().from() + f))
                                    .append(endMaxAllowed)
                                    .append(string.substring(rangeFlex.end().from() + f, rangeFlex.end().exclusiveTo())),
                            () -> result.append(string.substring(rangeFlex.end().from(), rangeFlex.end().exclusiveTo()))
                                    .append(endMaxAllowed)
                    );
                    result.append(endSuffix).append(endRange);
                    result.append(string.substring(rangeFlex.end().exclusiveTo()));
                } else {
                    int overlap = rangeFlex.start().exclusiveTo() - rangeFlex.end().from();
                    Optional<Integer> fuzzy = rangeFlex.maxLength().map(max -> max - overlap);
                    fuzzy.map(f -> Math.max(0, f)).ifPresentOrElse(
                            f -> result.append(string.substring(rangeFlex.start().from(), rangeFlex.end().from() - f))
                                    .append(startMaxAllowed)
                                    .append(string.substring(rangeFlex.end().from() - f, rangeFlex.end().from()))
                                    .append(endMaxAllowed),
                            () -> result
                                    .append(startMaxAllowed)
                                    .append(string.substring(rangeFlex.start().from(), rangeFlex.end().from()))
                    );

                    result.append(startSuffix)
                            .append(startAffixOverlap);
                    if (rangeFlex.start().exclusiveTo() <= rangeFlex.end().exclusiveTo()) {
                        fuzzy
                                .map(f -> Math.max(0, -f) / 2d)
                                .ifPresentOrElse(
                                        (Double f) -> result.append(string.substring(rangeFlex.end().from(), rangeFlex.end().from() + (int) Math.floor(f)))
                                                .append(startMaxAllowed)
                                                .append(string.substring(rangeFlex.end().from() + (int) Math.floor(f), rangeFlex.start().exclusiveTo() - (int) Math.ceil(f)))
                                                .append(endMaxAllowed)
                                                .append(string.substring(rangeFlex.start().exclusiveTo() - (int) Math.ceil(f), rangeFlex.start().exclusiveTo())),
                                        () -> result.append(string.substring(rangeFlex.end().from(), rangeFlex.start().exclusiveTo()))
                                );

                        result.append(endAffixOverlap)
                                .append(endPrefix)
                                .append(continueSuffixAfterOverlap);
                        fuzzy.map(f -> Math.max(0, f)).ifPresentOrElse(
                                f -> result.append(startMaxAllowed)
                                        .append(string.substring(rangeFlex.start().exclusiveTo(), rangeFlex.start().exclusiveTo() + f))
                                        .append(endMaxAllowed)
                                        .append(string.substring(rangeFlex.start().exclusiveTo() + f, rangeFlex.end().exclusiveTo())),
                                () -> result.append(string.substring(rangeFlex.start().exclusiveTo(), rangeFlex.end().exclusiveTo()))
                                        .append(endMaxAllowed)
                        );
                        result.append(endSuffix)
                                .append(endRange)
                                .append(string.substring(rangeFlex.end().exclusiveTo()));
                    } else {
                        result.append(string.substring(rangeFlex.end().from(), rangeFlex.end().exclusiveTo()))
                                .append(endAffixOverlap)
                                .append(endSuffix)
                                .append(endRange)
                                .append(continuePrefixAfterOverlap)
                                .append(string.substring(rangeFlex.end().exclusiveTo(), rangeFlex.start().exclusiveTo()))
                                .append(endPrefix)
                                .append(endMaxAllowed)
                                .append(string.substring(rangeFlex.start().exclusiveTo()));
                    }
                }
            } else {
                result.append(string.substring(0, rangeFlex.end().from()))
                        .append(startSuffix);
                if (rangeFlex.end().exclusiveTo() <= rangeFlex.start().from()) {
                    result.append(string.substring(rangeFlex.end().from(), rangeFlex.end().exclusiveTo()))
                            .append(endSuffix)
                            .append(endRange)
                            .append(string.substring(rangeFlex.end().exclusiveTo(), rangeFlex.start().from()))
                            .append(startRangeAfterRangeEnd)
                            .append(startPrefix)
                            .append(string.substring(rangeFlex.start().from(), rangeFlex.start().exclusiveTo()))
                            .append(endPrefix)
                            .append(string.substring(rangeFlex.start().exclusiveTo()));
                } else {
                    if (rangeFlex.end().exclusiveTo() <= rangeFlex.start().exclusiveTo()) {
                        result.append(string.substring(rangeFlex.end().from(), rangeFlex.start().from()))
                                .append(startRange)
                                .append(startPrefix)
                                .append(startAffixOverlap)
                                .append(string.substring(rangeFlex.start().from(), rangeFlex.end().exclusiveTo()))
                                .append(endAffixOverlap)
                                .append(endSuffix)
                                .append(endRange)
                                .append(continuePrefixAfterOverlap)
                                .append(string.substring(rangeFlex.end().exclusiveTo(), rangeFlex.start().exclusiveTo()))
                                .append(endPrefix)
                                .append(string.substring(rangeFlex.start().exclusiveTo()));
                    } else {
                        result.append(string.substring(rangeFlex.end().from(), rangeFlex.start().from()))
                                .append(startRange)
                                .append(startPrefix)
                                .append(startAffixOverlap)
                                .append(string.substring(rangeFlex.start().from(), rangeFlex.start().exclusiveTo()))
                                .append(endAffixOverlap)
                                .append(endPrefix)
                                .append(continueSuffixAfterOverlap)
                                .append(string.substring(rangeFlex.start().exclusiveTo(), rangeFlex.end().exclusiveTo()))
                                .append(endSuffix)
                                .append(endRange)
                                .append(string.substring(rangeFlex.end().exclusiveTo()));
                    }
                }
            }
            result.append(endGlobal);
            return result.toString();
        }

        @Override
        public String toString() {
            return name;
        }
    }


    public static void main(String[] args) {
        demo(RangeFlexFormatter.COLORS_ONLY);
    }

    private static void demo(RangeFlexFormatter formatter) {
        System.out.println("DEMO " + formatter.toString());
        System.out.println("-".repeat(formatter.toString().length() + 5));
        Stream.<Map.Entry<String, RangeFlex>>of(
                        entry("fixed range", RangeFlex.ofFixed(3, 7)),
                        entry("range with prefixes and suffixes", RangeFlex.of(2, 4, 7, 9)),
                        entry("range with prefixes and suffixes with maxLength of 4", RangeFlex.of(2, 4, 7, 9).constrain(Constraint.toMaxLength(4))),
                        entry("range without main part", RangeFlex.of(2, 5, 6, 9)),
                        entry("range without main part with maxlength of 3", RangeFlex.of(2, 5, 6, 9).constrain(Constraint.toMaxLength(3))),
                        entry("range without main part with maxlength of 0", RangeFlex.of(2, 5, 6, 9).constrain(Constraint.toMaxLength(0))),
                        entry("range with overlap", RangeFlex.of(2, 6, 5, 9)),
                        entry("range with overlap with maxlength of 2", RangeFlex.of(2, 6, 5, 9).constrain(Constraint.toMaxLength(2))),
                        entry("range with small overlap with maxlength of 3", RangeFlex.of(2, 6, 5, 9).constrain(Constraint.toMaxLength(3))),
                        entry("range with overlap with maxlength of 0", RangeFlex.of(2, 6, 5, 9).constrain(Constraint.toMaxLength(0))),
                        entry("range with large overlap with maxlength of 3", RangeFlex.of(2, 8, 3, 9).constrain(Constraint.toMaxLength(3))),
                        entry("range with overlapped suffix", RangeFlex.of(2, 9, 4, 7)),
                        entry("range with overlapped suffix with maxLength of 2", RangeFlex.of(2, 9, 4, 7).constrain(Constraint.toMaxLength(2))),
                        entry("range with overlapped suffix with maxLength of 6", RangeFlex.of(2, 9, 4, 7).constrain(Constraint.toMaxLength(2))),
                        entry("range with complete overlap", RangeFlex.of(2, 9, 2, 9)),
                        entry("range with complete overlap with maxlength of 3", RangeFlex.of(2, 9, 2, 9).constrain(Constraint.toMaxLength(3))),
                        entry("range with complete overlap with maxlength of 4", RangeFlex.of(2, 9, 2, 9).constrain(Constraint.toMaxLength(4))),
                        entry("range with complete overlap with maxlength of 0", RangeFlex.of(2, 9, 2, 9).constrain(Constraint.toMaxLength(0))),
                        null
                ).filter(Objects::nonNull)
                .forEach(entry -> System.out.println(formatter.format(entry.getValue(), "01234567890") + " : " + entry.getKey()));
        System.out.println();
    }
}
