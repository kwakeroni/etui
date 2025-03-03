package com.quaxantis.support.swing.form;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class FormField<T, V> {
    private final JLabel label;
	private final @Nullable String description;
    private final FormFieldInput<?, V> component;
    private final Function<T, V> beanGetter;
    private final BiConsumer<T, V> beanSetter;
    private final Object source;

    public FormField(
            @Nonnull JLabel label,
            @Nullable String description,
            @Nonnull FormFieldInput<?, V> component,
            @Nonnull Function<T, V> beanGetter,
            @Nonnull BiConsumer<T, V> beanSetter,
            @Nullable Object source) {
        this.label = Objects.requireNonNull(label, "label");
        this.description = description;
        this.component = Objects.requireNonNull(component, "component");
        this.beanGetter = Objects.requireNonNull(beanGetter, "beanGetter");
        this.beanSetter = Objects.requireNonNull(beanSetter, "beanSetter");
        this.source = source;
    }

    public boolean isEmpty() {
        return component.isEmpty();
    }

    public boolean isDirty() { return component.isDirty(); }

    public void addFormFieldListener(FormFieldListener formFieldListener) {
        this.component.addFormFieldListener(this, formFieldListener);
    }

    public void applyPattern(String pattern) {
        this.component.applyPattern(pattern);
    }

    public JLabel label() {
        return label;
    }

    @Nullable
    public String description() {
        return description;
    }

    @Nullable
    public Object source() {
        return this.source;
    }

    public JComponent component() {
        return component.component();
    }

    public V getValue() {
        return component.getValue();
    }

    public void setValue(V value) {
        component.setValue(value);
    }

    public void clear() {
        component.clear();
    }

    public void setValueFrom(T bean) {
        V value = beanGetter.apply(bean);
        component.setValue(value);
    }

    public void putValueIn(T bean) {
        V value = component.getValue();
        beanSetter.accept(bean, value);
    }

    public static Builder<?, ?> builder() {
        return new Builder<Void, Void>();
    }

    public static <T> Builder<T, String> ofTextField(String label, Function<T, String> getter, BiConsumer<T, String> setter) {
        return builder()
                .withLabel(label)
                .withComponentBinding(FormFieldInput.ofTextField(), getter, setter);
    }

    public static <T> Builder<T, String> ofTextArea(String label, Function<T, String> getter, BiConsumer<T, String> setter) {
        return FormField.builder()
                .withLabel(label)
                .withComponentBinding(FormFieldInput.ofTextArea(), getter, setter);
    }


    public static final class Builder<T, V> {
        private JLabel label;
        private String description;
        private FormFieldInput<?, V> input;
        private Function<T, V> beanGetter;
        private BiConsumer<T, V> beanSetter;
        private Object source;

        public Builder<T, V> withLabel(String label) {
            return withLabel(new JLabel(label));
        }

        public Builder<T, V> withLabel(JLabel label) {
            this.label = label;
            return this;
        }

        public Builder<T, V> withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder<T, V> withSource(Object source) {
            this.source = source;
            return this;
        }

        public <T2, V2> Builder<T2, V2> withComponentBinding(FormFieldInput<?, V2> input, Function<T2, V2> beanGetter, BiConsumer<T2, V2> beanSetter) {
            Builder<T2, V2> self = (Builder<T2, V2>) this;
            self.input = input;
            self.beanGetter = beanGetter;
            self.beanSetter = beanSetter;
            return self;
        }

        public FormField<T, V> build() {
            return new FormField<>(
                    this.label,
                    this.description,
                    this.input,
                    this.beanGetter,
                    this.beanSetter,
                    this.source);
        }
    }

    static class DocumentChangeTracker implements DocumentListener, FocusListener {
        boolean hasFocus = false;
        boolean changed = false;

        static DocumentChangeTracker of(JTextComponent textComponent) {
            var tracker = new DocumentChangeTracker();
            tracker.attachTo(textComponent);
            return tracker;
        }

        void attachTo(JTextComponent textComponent) {
            textComponent.getDocument().addDocumentListener(this);
            textComponent.addFocusListener(this);
        }

        @Override
        @OverridingMethodsMustInvokeSuper
        public void focusGained(FocusEvent e) {
            hasFocus = true;
        }

        @Override
        @OverridingMethodsMustInvokeSuper
        public void focusLost(FocusEvent e) {
            hasFocus = false;
        }

        private void onChange() {
            if (hasFocus) {
                changed = true;
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            onChange();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            onChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            onChange();
        }
    }

    static class TextChangeListenerDelegate extends DocumentChangeTracker {
        private final FormField<?, ?> formField;
        private final FormFieldListener listener;

        TextChangeListenerDelegate(FormField<?, ?> formField, FormFieldListener listener) {
            this.formField = formField;
            this.listener = listener;
        }

        @Override
        public void focusGained(FocusEvent e) {
            super.focusGained(e);
            listener.focusGained(formField, e);
        }

        @Override
        public void focusLost(FocusEvent e) {
            super.focusLost(e);
            if (changed) {
                listener.onChanged(formField);
            }
            changed = false;
            listener.focusLost(formField, e);
        }

    }
}
