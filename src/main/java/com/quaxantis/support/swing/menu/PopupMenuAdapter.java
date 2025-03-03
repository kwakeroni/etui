package com.quaxantis.support.swing.menu;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.util.function.Consumer;

public interface PopupMenuAdapter extends PopupMenuListener {
    @Override
    default void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    }

    @Override
    default void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }

    @Override
    default void popupMenuCanceled(PopupMenuEvent e) {
    }

    static PopupMenuAdapter onPopupMenuWillBecomeVisible(Consumer<PopupMenuEvent> action) {
        return new PopupMenuAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                action.accept(e);
            }
        };
    }

    static PopupMenuAdapter onPopupMenuWillBecomeInvisible(Consumer<PopupMenuEvent> action) {
        return new PopupMenuAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                action.accept(e);
            }
        };
    }

    static PopupMenuAdapter onPopupMenuCanceled(Consumer<PopupMenuEvent> action) {
        return new PopupMenuAdapter() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                action.accept(e);
            }
        };
    }
}
