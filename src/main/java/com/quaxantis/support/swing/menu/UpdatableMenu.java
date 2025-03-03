package com.quaxantis.support.swing.menu;

import javax.swing.JComponent;
import javax.swing.JMenu;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class UpdatableMenu extends JMenu implements UpdatableComponent {
    final Supplier<Stream<? extends JComponent>> componentSupplier;

    public UpdatableMenu(String text, Supplier<Stream<? extends JComponent>> componentSupplier) {
        super(text);
        this.componentSupplier = componentSupplier;
    }

    @Override
    public void update() {
        removeAll();
        componentSupplier.get().forEach(this::add);
        this.setEnabled(this.getItemCount() > 0);
    }
}
