package com.quaxantis.support.swing.form;

import java.awt.event.FocusEvent;

public interface FormFieldListener {
    void onChanged(FormField<?, ?> field);
    void focusGained(FormField<?, ?> field, FocusEvent event);
    void focusLost(FormField<?, ?> field, FocusEvent event);
}
