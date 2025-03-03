package com.quaxantis.etui.swing.menu;

import javax.annotation.Nonnull;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ActionBuilder extends AbstractAction {

    private BooleanSupplier enabledIf = () -> true;
    private ActionListener action = _ -> {
    };

    private ActionBuilder(String name) {
        super(name);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        action.actionPerformed(e);
    }

    public static ActionBuilder withName(String name) {
        return new ActionBuilder(name);
    }

    public ActionBuilder withAction(Runnable action) {
        return withAction(_ -> action.run());
    }

    public ActionBuilder withAction(ActionListener action) {
        this.action = action;
        return this;
    }

    public ActionBuilder enabledIf(BooleanSupplier enabledIf) {
        this.enabledIf = enabledIf;
        updateEnabled();
        return this;
    }

    public ActionBuilder enabledIf(BooleanSupplier enabledIf, Consumer<Runnable> register) {
        register.accept(this::updateEnabled);
        return enabledIf(enabledIf);
    }

    public void updateEnabled() {
        setEnabled(enabledIf.getAsBoolean());
    }

    public ActionBuilder withMnemonic(char mnemonic) {
        // @see javax.swing.AbstractButton#setMnemonic(char)
        int vk = (int) mnemonic;
        if (vk >= 'a' && vk <= 'z')
            vk -= ('a' - 'A');
        return withMnemonic(vk);
    }

    public ActionBuilder withMnemonic(int mnemonic) {
        putValue(Action.MNEMONIC_KEY, mnemonic);
        return this;
    }

    public ActionBuilder withAccelerator(char c) {
        return withAccelerator(c, 0);
    }

    public ActionBuilder withAccelerator(char c, int extraModifiers) {
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | extraModifiers;
        return withAccelerator(KeyStroke.getKeyStroke(c, modifiers));
    }

    public ActionBuilder withAccelerator(KeyStroke keyStroke) {
        putValue(Action.ACCELERATOR_KEY, keyStroke);
        return this;
    }

    public ActionBuilder bindTo(@Nonnull JComponent component, int condition) {
        Objects.requireNonNull(component, "component");
        KeyStroke accelerator = (KeyStroke) Objects.requireNonNull(getValue(Action.ACCELERATOR_KEY), "Accelerator not set");
        String name = (String) Objects.requireNonNull(getValue(Action.NAME), "Name not set");
        component.getInputMap(condition).put(accelerator, name);
        component.getActionMap().put(name, this);
        return this;
    }
}
