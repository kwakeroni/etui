package com.quaxantis.support.swing;

import java.awt.Dimension;
import java.awt.Insets;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

public class Dimensions {

    private Dimensions() {
        throw new UnsupportedOperationException();
    }

    public static final Dimension ZERO = new Dimension(0, 0);

    public static Dimension withWidth(Dimension base, int width) {
        return new Dimension(width, base.height);
    }

    public static Dimension addWidth(Dimension base, int width) {
        return add(base, width, 0);
    }

    public static Dimension addWidth(Dimension base, double width) {
        return addWidth(base, (int) width);
    }

    public static Dimension updateWidth(Dimension base, IntUnaryOperator operator) {
        return new Dimension(operator.applyAsInt(base.width), base.height);
    }

    public static Dimension withHeight(Dimension base, int height) {
        return new Dimension(base.width, height);
    }

    public static Dimension addHeight(Dimension base, int height) {
        return add(base, 0, height);
    }

    public static Dimension addHeight(Dimension base, double height) {
        return addHeight(base, (int) height);
    }

    public static Dimension updateHeight(Dimension base, IntUnaryOperator operator) {
        return new Dimension(base.width, operator.applyAsInt(base.height));
    }

    public static Dimension add(Dimension base, int width, int height) {
        return new Dimension(base.width + width, base.height + height);
    }

    public static Dimension add(Dimension base, double width, double height) {
        return add(base, (int) width, (int) height);
    }

    public static Dimension withMinimumWidthAndHeight(Dimension dimension, int minimumWidth, int minimumHeight) {
        return new Dimension(Math.max(dimension.width, minimumWidth), Math.max(dimension.height, minimumHeight));
    }

    public static Dimension withMaximumWidthAndHeight(Dimension dimension, int maximumWidth, int maximumHeight) {
        return new Dimension(Math.min(dimension.width, maximumWidth), Math.min(dimension.height, maximumHeight));
    }

    public static int maxWidth(Dimension... dimensions) {
        return maxWidth(Function.identity(), dimensions);
    }

    @SafeVarargs
    public static <T> int maxWidth(Function<T, Dimension> function, T... ts) {
        return max(t -> function.apply(t).width, ts);
    }

    public static int maxHeight(Dimension... dimensions) {
        return maxHeight(Function.identity(), dimensions);
    }

    @SafeVarargs
    public static <T> int maxHeight(Function<T, Dimension> function, T... ts) {
        return max(t -> function.apply(t).height, ts);
    }

    @SafeVarargs
    private static <T> int max(ToIntFunction<T> function, T... ts) {
        return reduce(0, Math::max, function, ts);
    }

    public static int sumWidth(Dimension... dimensions) {
        return sumWidth(Function.identity(), dimensions);
    }

    @SafeVarargs
    public static <T> int sumWidth(Function<T, Dimension> function, T... ts) {
        return reduce(0, Integer::sum, t -> function.apply(t).width, ts);
    }

    @SafeVarargs
    private static <T> int reduce(int identity, IntBinaryOperator reducer, ToIntFunction<T> function, T... ts) {
        int result = identity;
        for (var t : ts) {
            int value = function.applyAsInt(t);
            result = reducer.applyAsInt(result, value);
        }
        return result;
    }

    public static boolean fitsInto(Dimension small, Dimension large) {
        return small.height <= large.height && small.width <= large.width;
    }

    public static Dimension cropTo(Dimension large, Dimension small) {
        return new Dimension(
                Math.min(large.width, small.width),
                Math.min(large.height, small.height)
        );
    }

    public static Dimension minus(Dimension base, Dimension subtrahend) {
        return new Dimension(base.width - subtrahend.width, base.height - subtrahend.height);
    }

    public static Dimension minus(Dimension base, int subtrahend) {
        return new Dimension(base.width - subtrahend, base.height - subtrahend);
    }

    public static Dimension ofInsets(Insets insets) {
        return new Dimension(insets.left + insets.right, insets.top + insets.bottom);
    }

}
