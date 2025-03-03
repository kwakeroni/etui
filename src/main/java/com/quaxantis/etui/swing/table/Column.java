package com.quaxantis.etui.swing.table;

import java.util.Objects;
import java.util.function.Function;

enum Column {
    GROUP("Group", EditableTagValue::groupName),
    TAG("Tag", EditableTagValue::tagName),
    VALUE("Value", EditableTagValue::displayValue);

    private final String header;
    private final Function<EditableTagValue, Object> getter;

    Column(String header, Function<EditableTagValue, Object> getter) {
        this.header = header;
        this.getter = getter;
    }

    public String header() {
        return this.header;
    }

    public Object getFrom(EditableTagValue tag) {
        Objects.requireNonNull(tag, "tag");
        return getter.apply(tag);
    }

    public int index() {
        return ordinal();
    }

    public static Column atIndex(int columnIndex) {
        return Column.values()[columnIndex];
    }
}
