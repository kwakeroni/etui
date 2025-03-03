package com.quaxantis.support.swing.menu;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.util.function.BooleanSupplier;

public class UpdatableMenuItem extends JMenuItem implements UpdatableComponent {
    private BooleanSupplier checkEnabled;

    public static UpdatableMenuItem withText(String text) {
        return new UpdatableMenuItem(text);
    }

    public UpdatableMenuItem(String text) {
        super(text);
        this.checkEnabled = () -> true;
    }

    public void update() {
        setEnabled(checkEnabled.getAsBoolean());
    }

    public UpdatableMenuItem enabledWhen(BooleanSupplier supplier) {
        this.checkEnabled = supplier;
        return this;
    }

    public UpdatableMenuItem addAction(Runnable action) {
        return addAction(_ -> action.run());
    }

    public UpdatableMenuItem addAction(ActionListener actionListener) {
        this.addActionListener(actionListener);
        return this;
    }

    public UpdatableMenuItem withMnemonic(char c) {
        setMnemonic(c);
        return this;
    }

    public UpdatableMenuItem withAccelerator(char c) {
        return withAccelerator(c, 0);
    }

    public UpdatableMenuItem withAccelerator(char c, int extraModifiers) {
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | extraModifiers;
        return withAccelerator(KeyStroke.getKeyStroke(c, modifiers));
    }

    public UpdatableMenuItem withAccelerator(KeyStroke keyStroke) {
        setAccelerator(keyStroke);
        return this;
    }
}
