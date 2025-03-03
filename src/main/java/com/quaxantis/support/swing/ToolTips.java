package com.quaxantis.support.swing;

import javax.swing.JComponent;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Objects;

public class ToolTips {
    private static final char[] M = new char[]{'m'};

    private ToolTips() {
        throw new UnsupportedOperationException();
    }

    public static void setToolTipBackground(Color color) {
        UIManager.put("ToolTip.background", color);
    }

    public static void setToolTipAsHtml(JComponent component, String text) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(text, "text");
        component.setToolTipText(asHtml(component, text, component.getFont()));
    }

    private static String asHtml(Component component, String text, Font font) {
        if (text.matches("^\\s*<\\s*html[>\\s]")) {
            return text;
        }
        int toolTipWidth = tooltipWidth(component, font);
        return "<html><body style='width: %dpx;'>".formatted(toolTipWidth) + text.replaceAll("\\v", "<br>") + "</body></html>";
    }

    private static int tooltipWidth(Component component, Font font) {
        return 30 * font.createGlyphVector(component.getFontMetrics(font).getFontRenderContext(), M)
                .getGlyphPixelBounds(0, null, 0, 0).getSize().width;
    }
}
