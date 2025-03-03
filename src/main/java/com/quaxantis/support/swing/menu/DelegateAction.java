package com.quaxantis.support.swing.menu;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

public interface DelegateAction extends Action {
    Action getDelegate();

    default boolean accept(Object sender) {
        return getDelegate().accept(sender);
    }

    default void removePropertyChangeListener(PropertyChangeListener listener) {
        getDelegate().removePropertyChangeListener(listener);
    }

    default void putValue(String key, Object value) {
        getDelegate().putValue(key, value);
    }

    default void actionPerformed(ActionEvent e) {
        getDelegate().actionPerformed(e);
    }

    default boolean isEnabled() {
        return getDelegate().isEnabled();
    }

    default void addPropertyChangeListener(PropertyChangeListener listener) {
        getDelegate().addPropertyChangeListener(listener);
    }

    default void setEnabled(boolean b) {
        getDelegate().setEnabled(b);
    }

    default Object getValue(String key) {
        return getDelegate().getValue(key);
    }
}
