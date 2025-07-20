package com.quaxantis.support.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ANSI {
    // Reset
    public static final String RESET = "\033[0m";  // Text Reset

    // Colors
    public static final String BLACK = "\033[30m";   // BLACK
    public static final String RED = "\033[31m";     // RED
    public static final String GREEN = "\033[32m";   // GREEN
    public static final String YELLOW = "\033[33m";  // YELLOW
    public static final String BLUE = "\033[34m";    // BLUE
    public static final String PURPLE = "\033[35m";  // PURPLE
    public static final String CYAN = "\033[36m";    // CYAN
    public static final String WHITE = "\033[37m";   // WHITE
    public static final String BLACK_BRIGHT = "\033[90m";  // BLACK
    public static final String RED_BRIGHT = "\033[91m";    // RED
    public static final String GREEN_BRIGHT = "\033[92m";  // GREEN
    public static final String YELLOW_BRIGHT = "\033[93m"; // YELLOW
    public static final String BLUE_BRIGHT = "\033[94m";   // BLUE
    public static final String PURPLE_BRIGHT = "\033[95m"; // PURPLE
    public static final String CYAN_BRIGHT = "\033[96m";   // CYAN
    public static final String WHITE_BRIGHT = "\033[97m";  // WHITE

    // Background
    public static final String BLACK_BACKGROUND = "\033[40m";  // BLACK
    public static final String RED_BACKGROUND = "\033[41m";    // RED
    public static final String GREEN_BACKGROUND = "\033[42m";  // GREEN
    public static final String YELLOW_BACKGROUND = "\033[43m"; // YELLOW
    public static final String BLUE_BACKGROUND = "\033[44m";   // BLUE
    public static final String PURPLE_BACKGROUND = "\033[45m"; // PURPLE
    public static final String CYAN_BACKGROUND = "\033[46m";   // CYAN
    public static final String WHITE_BACKGROUND = "\033[47m";  // WHITE


    public static final String DEFAULT_COLOR = "\033[39m";
    public static final String DEFAULT_BACKGROUND = "\033[49m";
    public static final String BOLD = "\033[1m";
    public static final String FAINT = "\033[2m";
    public static final String ITALIC = "\033[3m";
    public static final String UNDERLINED = "\033[4m";
    public static final String DOUBLE_UNDERLINED = "\033[21m";
    public static final String NOT_BOLD = "\033[22m";
    public static final String NOT_ITALIC = "\033[23m";
    public static final String NOT_UNDERLINED = "\033[24m";

    private static final Pattern REGEX = Pattern.compile("\033\\[\\d+(?:;\\d*)*m");

    public static String strip(String ansiString) {
        return REGEX.matcher(ansiString).replaceAll("");
    }

    public static String escape(String ansiString) {
        return ansiString.replace('\033', '\\');
    }

    public static String toHTML(String ansiString) {
        ansiString = ansiString.replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;");
        StyleStack stack = new StyleStack();
        StringBuilder result = new StringBuilder();
        Matcher matcher = REGEX.matcher(ansiString);
        int i = 0;
        while (matcher.find()) {
            i++;
            matcher.appendReplacement(result, replace(matcher.group(), stack));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replace(String ansi, StyleStack stack) {
        return switch (ansi) {
            case BLACK -> stack.replaceColor("black");
            case RED -> stack.replaceColor("maroon");
            case GREEN -> stack.replaceColor("green");
            case YELLOW -> stack.replaceColor("olive");
            case BLUE -> stack.replaceColor("navy");
            case PURPLE -> stack.replaceColor("purple");
            case CYAN -> stack.replaceColor("teal");
            case WHITE -> stack.replaceColor("silver");
            case BLACK_BRIGHT -> stack.replaceColor("gray");
            case RED_BRIGHT -> stack.replaceColor("red");
            case GREEN_BRIGHT -> stack.replaceColor("lime");
            case YELLOW_BRIGHT -> stack.replaceColor("yellow");
            case BLUE_BRIGHT -> stack.replaceColor("blue");
            case PURPLE_BRIGHT -> stack.replaceColor("fuchsia");
            case CYAN_BRIGHT -> stack.replaceColor("aqua");
            case WHITE_BRIGHT -> stack.replaceColor("white");
            case DEFAULT_COLOR -> stack.replaceColor(null);
            case BLACK_BACKGROUND -> stack.replaceBgColor("black");
            case RED_BACKGROUND -> stack.replaceBgColor("maroon");
            case GREEN_BACKGROUND -> stack.replaceBgColor("green");
            case YELLOW_BACKGROUND -> stack.replaceBgColor("olive");
            case BLUE_BACKGROUND -> stack.replaceBgColor("navy");
            case PURPLE_BACKGROUND -> stack.replaceBgColor("purple");
            case CYAN_BACKGROUND -> stack.replaceBgColor("teal");
            case WHITE_BACKGROUND -> stack.replaceBgColor("silver");
            case DEFAULT_BACKGROUND -> stack.replaceBgColor(null);
            case BOLD -> stack.replaceWeight("bold");
            case FAINT -> stack.replaceWeight("200");
            case NOT_BOLD -> stack.replaceWeight(null);
            case ITALIC -> stack.replaceItalic("italic");
            case NOT_ITALIC -> stack.replaceItalic(null);
            case UNDERLINED -> stack.replaceUnderline("solid");
            case DOUBLE_UNDERLINED -> stack.replaceUnderline("double");
            case NOT_UNDERLINED -> stack.replaceUnderline(null);
            case RESET -> stack.resetAll();
            default -> "";
        };
    }

    private static class StyleStack {
        private final Map<String, String> current = new HashMap<>();
        private final Stack<String> active = new Stack<>();

        private String replaceColor(String color) {
            return replaceStyle("color", "<span style='color:%s;'>", color);
        }

        private String replaceBgColor(String bgColor) {
            return replaceStyle("bgColor", "<span style='background-color:%s;'>", bgColor);
        }

        private String replaceWeight(String weight) {
            return replaceStyle("weight", "<span style='font-weight:%s;'>", weight);
        }

        private String replaceItalic(String italic) {
            return replaceStyle("italic", "<span style='font-style:%s;'>", italic);
        }

        private String replaceUnderline(String underline) {
            return replaceStyle("underline", "<span style='text-decoration: underline %s;'>", underline);
        }

        private String replaceStyle(String type, String stylePattern, String styleArg) {
            String closeSpan = reset(type);
            return (styleArg == null) ? closeSpan : closeSpan + push(type, stylePattern.formatted(styleArg));
        }

        private String reset(String type) {
            if (contains(type)) {
                return pop(type, "</span>");
            } else {
                return "";
            }
        }

        private String resetAll() {
            StringBuilder builder = new StringBuilder();
            while (!active.isEmpty()) {
                builder.append("</span>");
                current.remove(active.pop());
            }
            return builder.toString();
        }

        private String push(String type, String text) {
            active.push(type);
            return current.merge(type, text, (old, _) -> {
                throw new IllegalStateException(type + " is already set to " + old);
            });
        }

        private boolean contains(String type) {
            return active.contains(type);
        }

        private String pop(String type, String closer) {
            Stack<String> backup = new Stack<>();
            StringBuilder builder = new StringBuilder();
            while (!type.equals(active.peek())) {
                builder.append(closer);
                backup.push(active.pop());
            }
            builder.append(closer);
            active.pop();
            current.remove(type);
            while (!backup.isEmpty()) {
                builder.append(current.get(active.push(backup.pop())));
            }
            return builder.toString();
        }
    }


    public static void main(String[] args) {
//        System.out.println(RESET + "HAHA" + "HAHA" + "HAHA");
//        System.out.println(RESET + "HAHA" + "\033[9m" + "HAHA" + "\033[29m" + "HAHA");
//        System.out.println(RESET + "HAHA" + "\033[7m" + "HAHA" + "\033[27m" + "HAHA");
//        System.out.println(RESET + "HAHA" + "\033[21m" + "HAHA" + UNDERLINED + "HAHA" + "\033[24m" + "HAHA");
//        System.out.println(RESET + UNDERLINED + "HAHA" + "\033[58:5:13m" + "HAHA" + "\033[59m" + "HAHA");
//        System.out.println(RESET + UNDERLINED + "HAHA" + "\033[53m" + "\033[8m" + "\033[11m" + "\033[18m" + "\033[20m" + "\033[73m" + "HAHA" + "\033[59m" + "HAHA");
        String string = "HAHA " + WHITE + "HAHA" + BLUE_BACKGROUND + "HAHA" + RED_BRIGHT + "HA" + GREEN_BACKGROUND + "HA" + DEFAULT_COLOR + "HA" + DEFAULT_BACKGROUND + "HA";
        System.out.println(string + NOT_UNDERLINED);
        System.out.println(strip(string));
        System.out.println(toHTML(string));
    }
}
