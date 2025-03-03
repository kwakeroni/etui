package com.quaxantis.etui.swing.table;

import com.quaxantis.etui.TagDescriptor;
import com.quaxantis.etui.TagValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

class TagTableCellRenderer extends DefaultTableCellRenderer {
    private  static final Logger log = LoggerFactory.getLogger(TagTableCellRenderer.class);
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setForeground(table.getForeground());
        setBackground(table.getBackground());
        var component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        var tag = ((TagTable) table).getTag(row);
        var col = ((TagTable) table).getColumn(column);
        applyStatusLayout(tag, col, component);

        return component;
    }

    private static void applyStatusLayout(EditableTagValue tag, Column column, Component component) {
        if (component instanceof JLabel label) {
                applyStatusLayout(label, column, tag);
        } else {
            log.warn("Expected JLabel but was " + component.getClass().getName());
        }
    }

    private static void applyStatusLayout(JLabel label, Column column, EditableTagValue tagValue) {
        if (tagValue.isReadOnly()) {
            label.setForeground(Color.GRAY);
        } else if (tagValue.isDeleted()) {
            label.setForeground(Color.RED);
        } else if (tagValue.isAdded()) {
            label.setForeground(Color.BLUE);
        } else if (tagValue.isChanged()) {
            TagValue original = tagValue.reset();
            boolean unchanged = switch (column) {
                case GROUP -> tagValue.groupName().equalsIgnoreCase(original.groupName());
                case TAG -> tagValue.tagName().equalsIgnoreCase(original.tagName());
                case VALUE -> tagValue.value().equals(original.value());
            };
            if (!unchanged) {
                label.setForeground(Color.BLUE);
            }
        }
    }
}
