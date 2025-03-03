package com.quaxantis.support.swing;

import javax.swing.JPopupMenu;
import java.awt.Component;
import java.awt.Window;
import java.util.Optional;

public class Components {
    private Components() {
        throw new UnsupportedOperationException();
    }

    public static Optional<Window> windowAncestor(Component component) {
        Component parent = component.getParent();
        while (parent != null) {
            if (parent instanceof Window window) {
                return Optional.of(window);
            } else if (parent.getParent() != null) {
                parent = parent.getParent();
            } else if (parent instanceof JPopupMenu popupMenu) {
                parent = popupMenu.getInvoker();
            } else {
                parent = null;
            }
        }
        return Optional.empty();
    }
}
