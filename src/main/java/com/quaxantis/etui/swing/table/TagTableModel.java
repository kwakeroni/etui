package com.quaxantis.etui.swing.table;

import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.TagValue;

import javax.swing.table.AbstractTableModel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

class TagTableModel extends AbstractTableModel {
    private final List<EditableTagValue> tags;

    public TagTableModel(List<EditableTagValue> tags) {
        this.tags = tags;
    }

    static WithStats<TagTableModel> createWithStats(TagSet tagSet) {
        var listWithStats = listWithStats(tagSet);
        var model = new TagTableModel(listWithStats.value());
        return new WithStats<>(model, listWithStats.stats());
    }

    private static WithStats<List<EditableTagValue>> listWithStats(TagSet tagSet) {
        return tagSet.stream()
                .map(EditableTagValue::of)
                .collect(WithStats.collectingTags());
    }

    public EditableTagValue getTag(int rowIndex) {
        return tags.get(rowIndex);
    }

    public void setTag(int rowIndex, EditableTagValue tag) {
        tags.set(rowIndex, tag);
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    int addTag(TagValue tag) {
        int row = this.tags.size();
        this.tags.add(EditableTagValue.ofAdded(tag));
        fireTableRowsInserted(row, row);
        return row;
    }

    public void deleteTag(int rowIndex) {
        tags.remove(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        EditableTagValue tag = tags.get(rowIndex);
        return getColumn(columnIndex).getFrom(tag);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (Column.VALUE.equals(getColumn(columnIndex))) {
            setTag(rowIndex, tags.get(rowIndex).withValue((String) value));
        } else {
            throw new UnsupportedOperationException("Column %s is not editable".formatted(getColumn(columnIndex).header()));
        }
    }

    public Stream<EditableTagValue> tags() {
        return tags.stream();
    }

    Map<Column, String> replaceTagSet(TagSet tagSet) {
        var listWithStats = listWithStats(tagSet);
        this.tags.clear();
        this.tags.addAll(listWithStats.value());
        fireTableStructureChanged();
        fireTableDataChanged();
        return listWithStats.stats();
    }

    public int[] mergeTags(TagSet tagSet) {
        int[] changedRows = new int[tagSet.size()];
        int i = 0;
        for (TagValue tag : tagSet) {
            changedRows[i++] = findRowWithSameTagAs(tag)
                    .map(row -> {
                        System.out.printf("Replacing %s with %s%n", tags.get(row), tag);
                        setTag(row, tags.get(row).mergeWithValue(tag.value()));
                        return row;
                    }).orElseGet(() -> addTag(tag));
        }
        return changedRows;
    }

    Optional<Integer> findRowWithSameTagAs(TagValue tagValue) {
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).isSameTagAs(tagValue)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public Column getColumn(int colIndex) {
        return Column.atIndex(colIndex);
    }

    public int getColumnIndex(Column column) {
        return column.index();
    }

    @Override
    public int getRowCount() {
        return tags.size();
    }

    @Override
    public int getColumnCount() {
        return Column.values().length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return getColumn(columnIndex).header();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == Column.VALUE.ordinal()
               && getTag(rowIndex).canBeEdited();
    }


    static record WithStats<T>(T value, Map<Column, String> stats) {
        private static Collector<EditableTagValue, ?, WithStats<List<EditableTagValue>>> collectingTags() {
            Collector<EditableTagValue, ?, Map<Column, String>> maxLengthCollector = Collector.of(
                    () -> new EnumMap<>(Column.class),
                    (map, tag) -> {
                        map.merge(Column.GROUP, defaultIfEmpty(tag.groupName(), ""), WithStats::longest);
                        map.merge(Column.TAG, defaultIfEmpty(tag.tagName(), ""), WithStats::longest);
                        map.merge(Column.VALUE, defaultIfEmpty(tag.value(), ""), WithStats::longest);
                    },
                    (map1, map2) -> {
                        map1.putAll(map2);
                        return map1;
                    }
            );

            return Collectors.teeing(
                    Collectors.toList(),
                    maxLengthCollector,
                    WithStats::new
            );
        }

        private static String longest(String one, String two) {
            return (one.length() >= two.length()) ? one : two;
        }
    }
}





