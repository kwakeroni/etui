package com.quaxantis.etui.template.parser;

import com.quaxantis.support.util.ANSI;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

public interface RangeFlexFormatter {

    static void main(String[] args) {
        System.out.println(defaultFormatter());
        setDefaultFormatter(UNDERLINE_ONLY);
        System.out.println(defaultFormatter());
    }

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

    String format(RangeFlex rangeFlex, String string);


    RangeFlexFormatter SEPARATORS_ONLY = new WithAffixes("SEPARATORS_ONLY", "", "{", "{", "[", "]", "", "", "[", "]", "}", "", "", "", "", "");
    RangeFlexFormatter SEPARATORS_COLORS = new WithAffixes("SEPARATORS_COLORS", ANSI.WHITE, ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND + ANSI.UNDERLINED + "{", ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND + "{", "[" + ANSI.WHITE_BRIGHT + ANSI.GREEN_BACKGROUND, ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND + "]" + ANSI.WHITE, ANSI.WHITE_BRIGHT + ANSI.BOLD, ANSI.DEFAULT_COLOR + ANSI.NOT_BOLD, ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND + "[" + ANSI.WHITE_BRIGHT + ANSI.BLUE_BACKGROUND, ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND + "]", "}" + ANSI.NOT_UNDERLINED + ANSI.WHITE, ANSI.RESET, ANSI.CYAN_BACKGROUND, ANSI.DEFAULT_BACKGROUND, ANSI.WHITE_BRIGHT + ANSI.GREEN_BACKGROUND, ANSI.WHITE_BRIGHT + ANSI.BLUE_BACKGROUND);
    RangeFlexFormatter COLORS_ONLY = new WithAffixes("COLORS_ONLY", ANSI.WHITE, ANSI.DEFAULT_COLOR + ANSI.UNDERLINED, ANSI.DEFAULT_COLOR, ANSI.WHITE_BRIGHT + ANSI.GREEN_BACKGROUND, ANSI.WHITE + ANSI.DEFAULT_BACKGROUND, ANSI.WHITE_BRIGHT + ANSI.BOLD, ANSI.DEFAULT_COLOR + ANSI.NOT_BOLD, ANSI.WHITE_BRIGHT + ANSI.BLUE_BACKGROUND, ANSI.DEFAULT_COLOR + ANSI.DEFAULT_BACKGROUND, ANSI.NOT_UNDERLINED + ANSI.WHITE, ANSI.RESET, ANSI.CYAN_BACKGROUND, ANSI.DEFAULT_BACKGROUND, ANSI.WHITE_BRIGHT + ANSI.GREEN_BACKGROUND, ANSI.WHITE_BRIGHT + ANSI.BLUE_BACKGROUND);
    RangeFlexFormatter UNDERLINE_ONLY = new WithIncludes("UNDERLINE_ONLY", ANSI.UNDERLINED, ANSI.NOT_UNDERLINED);

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
            String startPrefix,
            String endPrefix,
            String startMain,
            String endMain,
            String startSuffix,
            String endSuffix,
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
                    result.append(string.substring(rangeFlex.start().from(), rangeFlex.start().exclusiveTo()))
                            .append(endPrefix)
                            .append(startMain)
                            .append(string.substring(rangeFlex.start().exclusiveTo(), rangeFlex.end().from()))
                            .append(endMain)
                            .append(startSuffix)
                            .append(string.substring(rangeFlex.end().from(), rangeFlex.end().exclusiveTo()));
                    result.append(endSuffix).append(endRange);
                    result.append(string.substring(rangeFlex.end().exclusiveTo()));
                } else {
                    result.append(string.substring(rangeFlex.start().from(), rangeFlex.end().from()))
                            .append(startSuffix)
                            .append(startAffixOverlap);
                    if (rangeFlex.start().exclusiveTo() <= rangeFlex.end().exclusiveTo()) {
                        result.append(string.substring(rangeFlex.end().from(), rangeFlex.start().exclusiveTo()))
                                .append(endAffixOverlap)
                                .append(endPrefix)
                                .append(continueSuffixAfterOverlap)
                                .append(string.substring(rangeFlex.start().exclusiveTo(), rangeFlex.end().exclusiveTo()))
                                .append(endSuffix)
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
}
