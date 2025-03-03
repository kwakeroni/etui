package com.quaxantis.support.swing;

import java.awt.Dimension;

public record AspectRatio(int width, int height) {

    public static AspectRatio of(Dimension dimension) {
        return new AspectRatio(dimension.width, dimension.height);
    }

    public Dimension shrink(Dimension max) {
        int maxHeight = (int) (max.getWidth() * ((double) height) / ((double) width));
        int maxWidth = (int) (max.getHeight() * ((double) width) / ((double) height));

        return new Dimension(Math.min(max.width, maxWidth), Math.min(max.height, maxHeight));
    }

    public Dimension grow(Dimension min) {
        int minHeight = (int) (min.getWidth() * ((double) height) / ((double) width));
        int minWidth = (int) (min.getHeight() * ((double) width) / ((double) height));

        return new Dimension(Math.max(min.width, minWidth), Math.max(min.height, minHeight));
    }
}
