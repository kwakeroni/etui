package com.quaxantis.etui.swing.table;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.TagValue;
import com.quaxantis.etui.application.config.Configuration;
import com.quaxantis.etui.swing.menu.ActionBuilder;
import com.quaxantis.etui.swing.tagentry.TagEntryUI;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.support.swing.util.SwingDebugUtils;
import com.quaxantis.support.swing.util.TransferHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Frame;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.IntSummaryStatistics;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.quaxantis.etui.application.config.ConfigurationListener.Item.VIEW_FILTER;
import static javax.swing.KeyStroke.getKeyStroke;

class TagTable extends JTable {
    private static final String POPUP_CELL = TagTableUI.class.getName() + "::POPUP_CELL";
    private static final Logger log = LoggerFactory.getLogger(TagTable.class);

    private final Actions actions;

    static TagTable of(TagSet tagSet, TagRepository tagRepository, Configuration configuration) {
        var modelWithStats = TagTableModel.createWithStats(tagSet);
        var table = new TagTable(modelWithStats.value(), tagRepository, configuration);
        var fontMetrics = table.getFontMetrics(table.getFont());
        var columns = table.getColumnModel();
        for (int i = 0; i < columns.getColumnCount(); i++) {
            int stringWidth = fontMetrics.stringWidth(modelWithStats.stats().get(Column.atIndex(i)));
            TableColumn column = columns.getColumn(i);
            column.setPreferredWidth(stringWidth);
            column.setWidth(stringWidth);
        }

        return table;
    }

    private TagTable(TagTableModel model, TagRepository tagRepository, Configuration configuration) {
        super(model);
        setRowSorter(new TableRowSorter<TagTableModel>(model));
        setAutoCreateRowSorter(false);

        setViewFilter(configuration);
        configuration.addConfigurationListener(VIEW_FILTER, this::setViewFilter);

        setDefaultRenderer(String.class, new TagTableCellRenderer());
        setFillsViewportHeight(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setSurrendersFocusOnKeystroke(true);
        setTransferHandler(new TagTransferHandler(super.getTransferHandler()));
        this.actions = new Actions(tagRepository);

    }

    private void setViewFilter(Configuration configuration) {
        getRowSorter().setRowFilter(new TagTableRowFilter(configuration.getViewFilter()));
    }

    @Override
    @SuppressWarnings("unchecked") // model is cast-checked by setRowSorter
    public TableRowSorter<TagTableModel> getRowSorter() {
        return (TableRowSorter<TagTableModel>) super.getRowSorter();
    }

    @Override
    public void setRowSorter(RowSorter<? extends TableModel> sorter) {
        var tableRowSorter = (TableRowSorter<?>) sorter;
        var tagTableModel = (TagTableModel) tableRowSorter.getModel();
        @SuppressWarnings("unchecked") // model is cast-checked
        var tagTableRowSorter = (TableRowSorter<TagTableModel>) tableRowSorter;

        if (tagTableModel != tagTableRowSorter.getModel()) {
            tagTableRowSorter.setModel(tagTableModel);
        }
        super.setRowSorter(tagTableRowSorter);
    }

    public Actions actions() {
        return this.actions;
    }

    @Override
    public TagTransferHandler getTransferHandler() {
        return (TagTransferHandler) super.getTransferHandler();
    }

    @Override
    public TagTableModel getModel() {
        return (TagTableModel) super.getModel();
    }

    @Override
    public void setModel(TableModel dataModel) {
        if (getModel() != null) {
            throw new UnsupportedOperationException("Not allowed to replace TagTable model");
        }
        super.setModel(dataModel);
    }

    Stream<EditableTagValue> tags() {
        return getModel().tags();
    }

    EditableTagValue getTag(int rowIndex) {
        return getModel().getTag(convertRowIndexToModel(rowIndex));
    }

    void setTag(int rowIndex, EditableTagValue tag) {
        getModel().setTag(convertRowIndexToModel(rowIndex), tag);
    }

    void deleteTag(int rowIndex) {
        getModel().deleteTag(convertRowIndexToModel(rowIndex));
    }

    void addTag(TagValue tag) {
        int row = convertRowIndexToView(getModel().addTag(tag));
        scrollToRow(row);
    }

    public void mergeTags(TagSet tagSet) {
        int[] modelRows = getModel().mergeTags(tagSet);
        IntSummaryStatistics stats = IntStream.of(modelRows).summaryStatistics();
        if (stats.getCount() > 0) {
            scrollToRow(convertRowIndexToView(stats.getMax()));
            scrollToRow(convertRowIndexToView(stats.getMin()));
        }
    }

    private void scrollToRow(int row) {
        var cellRect = getCellRect(row, 0, false);
        scrollRectToVisible(cellRect);
    }

    Column getColumn(int viewColumnIndex) {
        return getModel().getColumn(convertColumnIndexToModel(viewColumnIndex));
    }

    int getColumnIndex(Column column) {
        return convertColumnIndexToView(getModel().getColumnIndex(column));
    }

    void setCellForPopup(Cell cell) {
        putClientProperty(POPUP_CELL, cell);
    }

    Optional<Cell> popCellForPopup() {
        Optional<Cell> value = peekCellForPopup();
        setCellForPopup(null);
        return value;
    }

    Optional<Cell> peekCellForPopup() {
        return Optional.ofNullable((Cell) getClientProperty(POPUP_CELL));
    }

    Optional<Cell> peekActionCell() {
        return peekCellForPopup()
                .or(this::selectedCell);
    }

    Optional<Cell> popActionCell(ActionEvent event) {
        if (event.getSource() instanceof JMenuItem) {
            return popCellForPopup();
        } else {
            return selectedCell();
        }
    }

    void addCellSelectionListener(Runnable runnable) {
        addPropertyChangeListener(POPUP_CELL, _ -> runnable.run());
        getSelectionModel().addListSelectionListener(_ -> runnable.run());
    }

    private Optional<Cell> selectedCell() {
        int row = getSelectedRow();
        int col = getSelectedColumn();
        if (row >= 0 && col >= 0) {
            return Optional.of(cellAt(row, col));
        } else {
            return Optional.empty();
        }
    }

    private Cell cellAt(int row, int col) {
        return new Cell(row, getColumn(col), getTag(row));
    }

    @Override
    public void doLayout() {
        var tableheader = getTableHeader();
        if (tableheader.getResizingColumn() == null) {
            TableColumn lastColumn = getColumnModel().getColumn(getColumnCount() - 1);
            tableheader.setResizingColumn(lastColumn);
        }
        super.doLayout();
    }

    @Override
    public Point getPopupLocation(MouseEvent event) {
        int col = columnAtPoint(event.getPoint());
        int row = rowAtPoint(event.getPoint());

        if (row >= 0 && col >= 0) {
            setCellForPopup(cellAt(row, col));
        }
        return super.getPopupLocation(event);
    }

    record Cell(int row, Column column, EditableTagValue tag) {
    }

    private static class TagTransferHandler extends TransferHandlerAdapter {
        enum PasteMode {
            ADD,
            MERGE
        }

        private TagTransferHandler(TransferHandler delegate) {
            super(delegate);
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.getComponent() instanceof TagTable
                   && support.getTransferable().isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            if (comp instanceof TagTable) {
                for (DataFlavor flavor : transferFlavors) {
                    if (DataFlavor.stringFlavor.equals(flavor)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean importData(TransferSupport support) {
            return support.getComponent() instanceof TagTable tagTable
                   && importData(PasteMode.ADD, tagTable, support.getTransferable());
        }

        @Override
        public boolean importData(JComponent comp, Transferable t) {
            return comp instanceof TagTable tagTable
                   && importData(PasteMode.ADD, tagTable, t);
        }

        public boolean importData(PasteMode pasteMode, TagTable tagTable, Transferable transferable) {
            try {
                if (canImport(tagTable, transferable.getTransferDataFlavors())) {
                    TagSet tags = String.valueOf(transferable.getTransferData(DataFlavor.stringFlavor))
                            .lines()
                            .map(this::toTag)
                            .collect(TagSet.toTagSet());

                    switch (pasteMode) {
                        case ADD -> tags.forEach(tagTable::addTag);
                        case MERGE -> tagTable.mergeTags(tags);
                    }

                    return true;
                }
            } catch (Exception exc) {
                log.error("Unable to paste data: {}", exc.toString());
            }
            return false;
        }

        private TagValue toTag(String pasteLine) {
            String[] parts = pasteLine.split("\\t", -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException("Unexpected text format does not match table: \"%s\"".formatted(pasteLine.replaceAll("\\t", "\\\\t")));
            }
            return TagValue.of(Tag.of(parts[0], parts[1]), parts[2]);
        }
    }

    public class Actions {
        private final Action addTag;
        private final Action editTagValue;
        private final Action editTagWithDialog;
        private final Action resetTag;
        private final Action deleteTag;
        private final Action copyTags;
        private final Action pasteTags;
        private final Action pasteMergeTags;

        public Actions(TagRepository tagRepository) {
            disablePressedKeys(0, KeyEvent.VK_INSERT, KeyEvent.VK_ENTER, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE);
            disablePressedKeys(KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_ENTER);

            TagTable table = TagTable.this;

            this.addTag = ActionBuilder.withName("Add tag")
                    .withMnemonic('a')
                    .withAccelerator(getKeyStroke(KeyEvent.VK_INSERT, 0, true))
                    .bindTo(table, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .withAction(() -> {
                        var tag = TagEntryUI.openDialog((Frame) getTopLevelAncestor(), tagRepository, "Add a tag", "Add", true);
                        tag.ifPresent(table::addTag);
                    });

            AtomicBoolean isEditingInline = new AtomicBoolean(false);
            this.editTagValue = ActionBuilder.withName("Edit tag value")
                    .withAccelerator(getKeyStroke(KeyEvent.VK_ENTER, 0, true))
                    .bindTo(table, WHEN_FOCUSED)
                    .enabledIf(() -> activeTag(EditableTagValue::canBeEdited).getAsBoolean() && getCellEditor() == null,
                               update -> {
                                   table.getSelectionModel().addListSelectionListener(_ -> {
                                       isEditingInline.set(false);
                                       update.run();
                                   });
                                   table.addPropertyChangeListener("tableCellEditor", e -> {
                                       if (e.getNewValue() != null) {
                                           isEditingInline.set(true);
                                       }
                                       update.run();
                                   });
                               })
                    .withAction(onActiveCell(cell -> {
                        if (isCellEditable(cell.row(), getColumnIndex(cell.column())) && getCellEditor() == null) {
                            if (isEditingInline.get()) {
                                isEditingInline.set(false);
                            } else {
                                table.editCellAt(cell.row(), getColumnIndex(cell.column()));
                            }
                        }
                    }));

            this.editTagWithDialog = ActionBuilder.withName("Edit tag")
                    .withMnemonic('e')
                    .withAccelerator(getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, true))
                    .bindTo(table, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .enabledIf(activeTag(EditableTagValue::canBeEdited), table::addCellSelectionListener)
                    .withAction(onActiveCell(cell -> {
                        var tag = TagEntryUI.openDialog((Frame) table.getTopLevelAncestor(), tagRepository, "Edit a tag", "Update", true, cell.tag());
                        tag.ifPresent(replacement -> table.setTag(cell.row(), cell.tag().replaceWith(replacement)));
                    }));

            this.resetTag = ActionBuilder.withName("Reset tag")
                    .withMnemonic('r')
                    .withAccelerator(getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, true))
                    .bindTo(table, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .enabledIf(activeTag(EditableTagValue::canBeReset), table::addCellSelectionListener)
                    .withAction(onActiveCell(cell -> table.setTag(cell.row(), cell.tag().reset())));

            // TODO: BUG: using delete while editing in-line cause tag to be deleted
            this.deleteTag = ActionBuilder.withName("Delete tag")
                    .withMnemonic('d')
                    .withAccelerator(getKeyStroke(KeyEvent.VK_DELETE, 0, true))
                    .bindTo(table, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .enabledIf(activeTag(EditableTagValue::canBeDeleted), table::addCellSelectionListener)
                    .withAction(onActiveCell(
                            cell -> cell.tag().delete().ifPresentOrElse(
                                    deletedTag -> table.setTag(cell.row(), deletedTag),
                                    () -> table.deleteTag(cell.row())))
                    );

            this.copyTags = ActionBuilder.withName("Copy tag(s)")
                    .withMnemonic('c')
                    .withAccelerator(getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, true))
                    .enabledIf(() -> table.getSelectedRows().length > 0, table::addCellSelectionListener)
                    .withAction(e -> TransferHandler.getCopyAction().actionPerformed(
                            new ActionEvent(table, e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers())
                    ));

            this.pasteTags = ActionBuilder.withName("Paste as new tag(s)")
                    .withMnemonic('p')
                    .withAccelerator(getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK, true))
                    .enabledIf(() -> table.getTransferHandler().canImport(table, table.getToolkit().getSystemClipboard().getAvailableDataFlavors()))
                    .withAction(e -> TransferHandler.getPasteAction().actionPerformed(
                            new ActionEvent(table, e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers())
                    ));

            this.pasteMergeTags = ActionBuilder.withName("Paste as merged tag(s)")
                    .withMnemonic('p')
                    .withAccelerator(getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK, true))
                    .bindTo(table, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .enabledIf(() -> table.getTransferHandler().canImport(table, table.getToolkit().getSystemClipboard().getAvailableDataFlavors()))
                    .withAction(e -> {
                        Transferable transferable = table.getToolkit().getSystemClipboard().getContents(null);
                        table.getTransferHandler().importData(TagTransferHandler.PasteMode.MERGE, table, transferable);
                    });

            ActionBuilder.withName("info")
                    .withAccelerator(getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK, true))
                    .bindTo(table, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .withAction(e -> {
                        System.out.println("Table Keys");
                        SwingDebugUtils.getKeyMappings(table).forEach(System.out::println);
                    });
        }

        private BooleanSupplier activeCell(Predicate<Cell> predicate) {
            return () -> TagTable.this.peekActionCell().filter(predicate).isPresent();
        }

        private BooleanSupplier activeTag(Predicate<EditableTagValue> predicate) {
            return activeCell(cell -> predicate.test(cell.tag()));
        }

        private ActionListener onActiveCell(Consumer<Cell> action) {
            return event -> TagTable.this.popActionCell(event).ifPresent(action);
        }

        private void disablePressedKeys(int modifiers, int... vkKeys) {
            TagTable.this.getActionMap().put("none", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            });
            InputMap inputMap = TagTable.this.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            for (int vkKey : vkKeys) {
                inputMap.put(getKeyStroke(vkKey, modifiers, false), "none");
            }
        }

        public Action addTag() {
            return addTag;
        }

        public Action editTagValue() {
            return editTagValue;
        }

        public Action editTagWithDialog() {
            return editTagWithDialog;
        }

        public Action resetTag() {
            return resetTag;
        }

        public Action deleteTag() {
            return deleteTag;
        }

        public Action copyTags() {
            return copyTags;
        }

        public Action pasteTags() {
            return pasteTags;
        }

        public Action pasteMergeTags() {
            return pasteMergeTags;
        }
    }
}
