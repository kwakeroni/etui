package com.quaxantis.support.swing.form;

import com.quaxantis.support.swing.ToolTips;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JComponent;
import java.awt.*;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class InfoMarker extends JComponent {
    private static final int PAD = 5;
    private static final Font MONOSPACE = new Font(Font.MONOSPACED, Font.BOLD, 12);

    @Nonnull
    private final char[] infoCharacters;
    @Nonnull
    private Type type = Type.INFO;
    @Nullable
    private Shape shape;
    @Nullable
    private Color foreground;
    @Nullable
    private Color background;

    public InfoMarker(char infoCharacter) {
        this(infoCharacter, null);
    }

    public InfoMarker(String tooltip) {
        this('i', tooltip);
    }

    public InfoMarker(char infoCharacter, String tooltip) {
        this.infoCharacters = new char[]{infoCharacter};
        this.setFont(MONOSPACE);
        setToolTipAsHtml(tooltip);
    }

    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        this.foreground = fg;
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        this.background = bg;
    }

    public InfoMarker setShape(Shape shape) {
        this.shape = shape;
        return this;
    }

    public InfoMarker setType(@Nonnull Type type) {
        this.type = requireNonNull(type, "type");
        return this;
    }

    public InfoMarker setToolTipAsHtml(String tooltip) {
        if (tooltip != null) {
            ToolTips.setToolTipAsHtml(this, tooltip);
        } else {
            this.setToolTipText(null);
        }
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return this.calculatedSize();
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return this.calculatedSize();
    }

    @Override
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        return this.calculatedSize();
    }

    private Dimension calculatedSize() {
        Dimension textSize = textSize(getFont(), infoCharacters);
        int size = Math.max(textSize.width, textSize.height) + 2 * PAD;
        return new Dimension(size, size);
    }

    private Dimension textSize(@Nonnull Font font, @Nonnull char[] chars) {
        return font.createGlyphVector(getFontMetrics(font).getFontRenderContext(), chars)
                .getGlyphPixelBounds(0, null, 0, 0).getSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (g instanceof Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        var width = getSize().width - 1;
        var height = getSize().height - 1;
        var text = new String(infoCharacters);
        var textWidth = g.getFontMetrics().stringWidth(text);
        var textDescent = g.getFontMetrics().getDescent();
        var textX = getWidth()/2 - textWidth/2;
        var textY = getHeight()/2 + textDescent;
        var fgColor = firstNonNull(this.foreground, this.type.foreground);
        var bgColor = firstNonNull(this.background, this.type.background);
        g.setFont(getFont());


        super.paintComponent(g);

        g.setColor(bgColor);
        if (isOpaque()) {
            g.fillRect(0, 0, width, height);
        }

        switch (firstNonNull(this.shape, this.type.shape)) {
            case null -> {}
            case Shape.CIRCLE -> {
                g.fillOval(0, 0, width, height);
                g.setColor(fgColor);
                g.drawOval(0, 0, width, height);
                g.drawString(text, textX, textY);
            }
            case Shape.SQUARE -> {
                g.fillRect(0, 0, width, height);
                g.setColor(fgColor);
                g.drawRect(0, 0, width, height);
                g.drawString(text, textX, textY);
            }
            case Shape.TRIANGLE -> {
                var triangle = new Polygon(new int[]{getWidth()/2, width, 0}, new int[]{0, height, height}, 3);
                g.fillPolygon(triangle);
                g.setColor(fgColor);
                g.drawPolygon(triangle);
                g.drawString(text, textX, textDescent/3 + 1 + textY);
            }
        }



    }

    public enum Shape {
        CIRCLE,
        SQUARE,
        TRIANGLE;
    }
    public enum Type {
        INFO(Shape.CIRCLE, new Color(102, 102, 153), new Color(204, 204, 255, 255)),
        WARNING(Shape.TRIANGLE, new Color(61, 32, 23, 255), new Color(255, 143, 70, 255));

        @Nonnull
        private final Shape shape;
        @Nonnull
        private final Color foreground;
        @Nonnull
        private final Color background;

        Type(@Nonnull Shape shape, @Nonnull Color foreground, @Nonnull Color background) {
            this.shape = shape;
            this.background = background;
            this.foreground = foreground;
        }
    }

}
