package com.quaxantis.etui.swing.table;

import com.quaxantis.etui.application.config.ViewFilter;

import javax.swing.RowFilter;

public class TagTableRowFilter extends RowFilter<TagTableModel, Integer> {
    private final ViewFilter viewFilter;

    public TagTableRowFilter(ViewFilter viewFilter) {
        this.viewFilter = viewFilter;
    }

    @Override
    public boolean include(Entry<? extends TagTableModel, ? extends Integer> entry) {
        if (viewFilter.isReadOnlyHidden()) {
            TagTableModel model = entry.getModel();
            int row = entry.getIdentifier();
            return ! model.getTag(row).isReadOnly();
        } else {
            return true;
        }
    }
}
