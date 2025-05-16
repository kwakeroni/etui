package com.quaxantis.etui.template.parser;

record IntRange(int from, int to) {

    public IntRange {
        if (from > to) {
            throw new IllegalArgumentException("Cannot create range where from=%d is larger than to=%d".formatted(from, to));
        }
    }

    public IntRange trimLeft(int remove) {
        return new IntRange(from + remove, to);
    }

    public IntRange trimRight(int remove) {
        return new IntRange(from, to - remove);
    }

    public IntRange truncateLeft(IntRange onLeft) {
        return truncateLeft(onLeft, 0);
    }

    public IntRange truncateLeft(IntRange onLeft, int overlap) {
        int truncFrom = Math.min(this.to, Math.max(this.from, onLeft.to - overlap));
        return new IntRange(truncFrom, to);
    }

    public IntRange truncateRight(IntRange onRight) {
        return truncateRight(onRight, 0);
    }

    public IntRange truncateRight(IntRange onRight, int overlap) {
        int truncTo = Math.max(this.from, Math.min(this.to, onRight.from + overlap));
        return new IntRange(from, truncTo);
    }

    @Override
    public String toString() {
        return "(" + from + "," + to + "(";
    }
}
