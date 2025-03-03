package com.quaxantis.support.swing.form;

import org.apache.commons.lang3.StringUtils;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.function.*;

public interface FormFieldInput<C extends JComponent, V> {

    private static int DEFAULT_TEXT_COLS()  { return 20;}

    C component();
    void setValue(V value);
    void clear();
    V getValue();
    boolean isEmpty();
    boolean isDirty();
    void applyPattern(String pattern);
    void addFormFieldListener(FormField<?, ?> formField, FormFieldListener formFieldListener);

    default <WC extends JComponent> FormFieldInput<WC, V> wrappedIn(Function<C, WC> wrapper) {
        return WrappedFormFieldInput.of(wrapper, this);
    }

    static FormFieldInput<JTextField, String> ofTextField() {
        return of(new JTextField(DEFAULT_TEXT_COLS()) {
            @Override
            public void setText(String t) {
                super.setText(t);
                if (t != null && (getColumns() < t.length() || getScrollOffset() > 0)) {
                    setToolTipText(t);
                }
            }
        });
    }

    static FormFieldInput<JTextField, String> of(JTextField textField) {
        class JTextField implements FormFieldInput<javax.swing.JTextField, String> {
            private final FormField.DocumentChangeTracker changeTracker = FormField.DocumentChangeTracker.of(textField);

            @Override
            public javax.swing.JTextField component() {
                return textField;
            }

            @Override
            public void setValue(String value) {
                textField.setText(value);
                textField.setScrollOffset(0);
            }

            @Override
            public void clear() {
                setValue(null);
            }

            @Override
            public String getValue() {
                return textField.getText();
            }

            @Override
            public boolean isEmpty() {
                return StringUtils.isEmpty(textField.getText());
            }

            @Override
            public boolean isDirty() {
                return this.changeTracker.changed;
            }

            @Override
            public void applyPattern(String pattern) {
                setValue(getValue() + pattern);
            }

            @Override
            public void addFormFieldListener(FormField<?, ?> formField, FormFieldListener listener) {
                var textChangeListener = new FormField.TextChangeListenerDelegate(formField, listener);
                textChangeListener.attachTo(textField);
            }
        }

        return new JTextField();
    }

    static FormFieldInput<?, String> ofTextArea() {
        JTextArea textArea = new JTextArea(1, DEFAULT_TEXT_COLS());
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        return FormFieldInput.of(textArea).wrappedIn(ta -> {
            JScrollPane scrollPane = new JScrollPane(ta);
            scrollPane.setBorder(new JTextField().getBorder());
            return scrollPane;
        });
    }

    static FormFieldInput<JTextArea, String> of(JTextArea textArea) {
        class JTextArea implements FormFieldInput<javax.swing.JTextArea, String> {
            private final FormField.DocumentChangeTracker changeTracker = FormField.DocumentChangeTracker.of(textArea);

            @Override
            public javax.swing.JTextArea component() {
                return textArea;
            }

            @Override
            public void setValue(String value) {
                textArea.setText(value);
            }

            @Override
            public void clear() {
                setValue(null);
            }

            @Override
            public String getValue() {
                return textArea.getText();
            }

            @Override
            public boolean isEmpty() {
                return StringUtils.isEmpty(textArea.getText());
            }

            @Override
            public boolean isDirty() {
                return changeTracker.changed;
            }

            @Override
            public void applyPattern(String pattern) {
                setValue(getValue() + pattern);
            }

            @Override
            public void addFormFieldListener(FormField<?, ?> formField, FormFieldListener listener) {
                var textChangeListener = new FormField.TextChangeListenerDelegate(formField, listener);
                textChangeListener.attachTo(textArea);
            }
        }

        return new JTextArea();
    }

}
