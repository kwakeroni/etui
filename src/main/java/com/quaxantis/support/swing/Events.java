package com.quaxantis.support.swing;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Optional;

public class Events {
    private Events() {
        throw new UnsupportedOperationException();
    }

    public static Optional<Window> windowAncestor(ActionEvent event) {
        return (event.getSource() instanceof Component component) ?
                Components.windowAncestor(component) : Optional.empty();
    }
}
