package com.quaxantis.etui.template.parser;

record IntRange(int from, int exclusiveTo) {

    public static IntRange of(int from, int exclusiveTo) {
        return new IntRange(from, exclusiveTo);
    }

    public static IntRange ofClosed(int from, int inclusiveTo) {
        return new IntRange(from, inclusiveTo + 1);
    }

    IntRange {
        if (from > exclusiveTo) {
            throw new IllegalArgumentException("Cannot create range where from=%d is larger than to=%d".formatted(from, exclusiveTo));
        }
    }

    public int to() {
        return exclusiveTo - 1;
    }

    public int exclusiveTo() {
        return exclusiveTo;
    }

    public boolean isEmpty() {
        return from == exclusiveTo;
    }

    public IntRange trimLeft(int remove) {
        return new IntRange(from + remove, exclusiveTo);
    }

    public IntRange trimRight(int remove) {
        return new IntRange(from, exclusiveTo - remove);
    }

    public IntRange truncateLeft(IntRange onLeft) {
        return truncateLeft(onLeft, 0);
    }

    public IntRange truncateLeft(IntRange onLeft, int overlap) {
        int truncFrom = Math.min(this.exclusiveTo, Math.max(this.from, onLeft.exclusiveTo - overlap));
        return new IntRange(truncFrom, exclusiveTo);
    }

    public IntRange truncateRight(IntRange onRight) {
        return truncateRight(onRight, 0);
    }

    public IntRange truncateRight(IntRange onRight, int overlap) {
        int truncTo = Math.max(this.from, Math.min(this.exclusiveTo, onRight.from + overlap));
        return new IntRange(from, truncTo);
    }

    @Override
    public String toString() {
        return "(" + from + "," + exclusiveTo + "(";
    }
}
