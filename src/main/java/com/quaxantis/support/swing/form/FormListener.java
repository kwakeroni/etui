package com.quaxantis.support.swing.form;

import java.awt.event.FocusEvent;
import java.util.EventListener;
import java.util.function.Consumer;

public interface FormListener<F extends AbstractForm<?, ?>> extends EventListener {
    void onChanged(F form);
    void focusGained(F form, FormField<?, ?> field, FocusEvent event);
    void focusLost(F form, FormField<?, ?> field, FocusEvent event);

    abstract class Adapter<F extends AbstractForm<?, ?>> implements FormListener<F> {
        @Override
        public void onChanged(F form) {
        }

        @Override
        public void focusGained(F form, FormField<?, ?> field, FocusEvent event) {
        }

        @Override
        public void focusLost(F form, FormField<?, ?> field, FocusEvent event) {
        }
    }

    static <F extends AbstractForm<?, ?>> FormListener<F> onChanged(Consumer<F> listener) {
        return new Adapter<>() {
            @Override
            public void onChanged(F form) {
                listener.accept(form);
            }
        };
    }
}
