package com.quaxantis.etui.swing.actions;

import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.swing.menu.ActionBuilder;

import javax.swing.AbstractButton;
import javax.swing.Action;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ViewActions {
    private final Action hideReadOnly;
    private final Supplier<Boolean> getHideReadOnly;

    public ViewActions(ConfigOperations configOperations) {
        this.getHideReadOnly = () -> configOperations.getConfiguration().getViewFilter().isReadOnlyHidden();
        this.hideReadOnly = ActionBuilder.withName("Hide read-only tags")
                .withMnemonic('h')
                .withAction(event -> configOperations.setViewFilter(
                        filter -> event.getSource() instanceof AbstractButton button ?
                                filter.withReadOnlyHidden(button.isSelected()) :
                                filter.toggleReadOnlyHidden()));
    }

    public Action hideReadOnly() {
        return this.hideReadOnly;
    }

    public <R> Function<BiConsumer<R, Boolean>, R> hideReadOnly(Function<Action, R> factory) {
        return initializer -> {
            R result = factory.apply(hideReadOnly());
            initializer.accept(result, getHideReadOnly.get());
            return result;
        };
    }

}
