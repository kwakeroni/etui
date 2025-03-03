package com.quaxantis.etui.swing.table;

import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.application.config.Configuration;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.support.swing.Dimensions;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static com.quaxantis.support.swing.menu.PopupMenuAdapter.onPopupMenuCanceled;
import static com.quaxantis.support.swing.menu.PopupMenuAdapter.onPopupMenuWillBecomeVisible;

public class TagTableUI {

    private final TagTable table;
    private final JScrollPane scrollPane;
    private final EventListenerList listenerList;

    public TagTableUI(TagSet tagSet, TagRepository tagRepository, Configuration configuration) {
        this.table = createTable(tagSet, tagRepository, configuration);
        this.scrollPane = createScrollPane(this.table);
        this.listenerList = new EventListenerList();
        initialize();
    }

    public Container getUIContainer() {
        return this.scrollPane;
    }

    public void addChangeListener(ChangeListener changeListener) {
        this.listenerList.add(ChangeListener.class, changeListener);
    }

    private void fireChange() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : this.listenerList.getListeners(ChangeListener.class)) {
            listener.stateChanged(event);
        }
    }

    public TagSet getTags() {
        return table.tags()
                .collect(TagSet.toTagSet());
    }

    public TagSet getChanges() {
        return table.tags()
                .mapMulti(EditableTagValue::pushChanges)
                .collect(TagSet.toTagSet());
    }

    public void replaceTagSet(TagSet tagSet) {
        this.table.getModel().replaceTagSet(tagSet);
    }

    public void mergeTags(TagSet tagSet) {
        this.table.mergeTags(tagSet);
    }

    private void initialize() {
        table.setComponentPopupMenu(createPopupMenu(table));
        this.table.getModel().addTableModelListener(e -> fireChange());
    }

    private static TagTable createTable(TagSet tagSet, TagRepository tagRepository, Configuration configuration) {
        TagTable table = TagTable.of(tagSet, tagRepository, configuration);
        return table;
    }

    private static JScrollPane createScrollPane(JTable table) {

        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.setPreferredSize(
                Dimensions.addWidth(table.getPreferredSize(),
                                    scrollPane.getVerticalScrollBar().getPreferredSize().getWidth()));
        return scrollPane;
    }

    private static JPopupMenu createPopupMenu(TagTable table) {
        var menu = new JPopupMenu();
        menu.add(new JMenuItem(table.actions().addTag()));
        menu.add(new JMenuItem(table.actions().editTagWithDialog()));
        menu.addSeparator();
        menu.add(new JMenuItem(table.actions().resetTag()));
        menu.add(new JMenuItem(table.actions().deleteTag()));

        menu.addPopupMenuListener(onPopupMenuCanceled(e -> table.popCellForPopup()));
        menu.addPopupMenuListener(onPopupMenuWillBecomeVisible(e -> {
            for (var component : menu.getComponents()) {
                if (component instanceof TagActionMenuItem tagAction) {
                    tagAction.updateEnabled();
                }
            }
        }));
        return menu;
    }

    private static class TagActionMenuItem extends JMenuItem {
        final BooleanSupplier checkEnabled;

        public TagActionMenuItem(String text, TagTable table,
                                 Predicate<TagTable.Cell> enabled,
                                 BiConsumer<ActionEvent, TagTable.Cell> action) {
            super(text);
            this.checkEnabled = () -> table.peekCellForPopup().filter(enabled).isPresent();
            this.addActionListener(event -> table.popCellForPopup().ifPresent(cell -> action.accept(event, cell)));
        }

        public void updateEnabled() {
            setEnabled(checkEnabled.getAsBoolean());
        }
    }
}
