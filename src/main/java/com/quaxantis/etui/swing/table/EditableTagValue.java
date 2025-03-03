package com.quaxantis.etui.swing.table;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagDescriptor;
import com.quaxantis.etui.TagValue;

import javax.annotation.CheckReturnValue;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

abstract sealed class EditableTagValue implements TagValue {

    private EditableTagValue() {
    }

    public static EditableTagValue of(TagValue original) {
        return new OriginalTagValue(original);
    }

    public static EditableTagValue ofAdded(TagValue added) {
        return new AddedTagValue(added);
    }

    public abstract String displayValue();

    public abstract boolean isAdded();

    public abstract boolean isChanged();

    public abstract boolean isDeleted();

    public void pushChanges(Consumer<? super TagValue> consumer) {
        if (isChanged()) {
            consumer.accept(this);
        }
    }

    public final boolean isReadOnly() {
        return TagDescriptor.isReadOnly(tag());
    }

    public final boolean canBeEdited() {
        return isStatusEditable() && ! isReadOnly();
    }

    protected boolean isStatusEditable() {
        return true;
    }

    public final boolean canBeDeleted() {
        return isStatusDeletable() && ! isReadOnly();
    }

    protected boolean isStatusDeletable() {
        return true;
    }

    public boolean canBeReset() {
        return true;
    }

    @CheckReturnValue
    public abstract EditableTagValue withValue(String value);

    @CheckReturnValue
    public EditableTagValue mergeWithValue(String value) {
        return withValue(value);
    }

    @CheckReturnValue
    public abstract EditableTagValue reset();

    @CheckReturnValue
    public abstract Optional<EditableTagValue> delete();

    @CheckReturnValue
    public EditableTagValue replaceWith(TagValue tagValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return asString();
    }

    private static sealed class OriginalTagValue extends EditableTagValue {
        final TagValue original;

        private OriginalTagValue(TagValue original) {
            this.original = original;
        }

        @Override
        public Tag tag() {
            return original.tag();
        }

        @Override
        public String value() {
            return original.value();
        }

        @Override
        public String displayValue() {
            return original.value();
        }

        @Override
        public boolean isAdded() {
            return false;
        }

        @Override
        public boolean isChanged() {
            return false;
        }

        @Override
        public boolean isDeleted() {
            return false;
        }

        public boolean canBeReset() {
            return false;
        }

        @Override
        public EditableTagValue withValue(String value) {
            if (Objects.equals(value, this.displayValue())) {
                return this;
            } else {
                return new EditedTagValue(this, value);
            }
        }

        @Override
        public EditableTagValue replaceWith(TagValue tagValue) {
            if (this.isEqualTo(tagValue)) {
                return this;
            } else {
                return new ReplacedTagValue(this, tagValue);
            }
        }

        @Override
        public EditableTagValue reset() {
            return this;
        }

        @Override
        public Optional<EditableTagValue> delete() {
            return Optional.of(new DeletedTagValue(this));
        }
    }

    private static final class AddedTagValue extends OriginalTagValue {
        private AddedTagValue(TagValue original) {
            super(original);
        }

        @Override
        public boolean isAdded() {
            return true;
        }

        @Override
        public boolean isChanged() {
            return true;
        }

        @Override
        public Optional<EditableTagValue> delete() {
            return Optional.empty();
        }
    }

    private static abstract sealed class ChangedTagValue extends EditableTagValue {
        final EditableTagValue original;

        private ChangedTagValue(EditableTagValue original) {
            this.original = original;
        }

        @Override
        public Tag tag() {
            return original.tag();
        }

        @Override
        public boolean isAdded() {
            return original.isAdded();
        }

        @Override
        public boolean isChanged() {
            return true;
        }

        @Override
        public EditableTagValue withValue(String value) {
            return original.withValue(value);
        }

        @Override
        public EditableTagValue replaceWith(TagValue tagValue) {
            return original.replaceWith(tagValue);
        }

        @Override
        public EditableTagValue reset() {
            return original;
        }

        @Override
        public Optional<EditableTagValue> delete() {
            return original.delete();
        }
    }

    private static final class EditedTagValue extends ChangedTagValue {
        private final String changedValue;

        private EditedTagValue(EditableTagValue original, String changedValue) {
            super(original);
            this.changedValue = changedValue;
        }

        @Override
        public String value() {
            return this.changedValue;
        }

        @Override
        public String displayValue() {
            return this.changedValue;
        }

        @Override
        public boolean isDeleted() {
            return false;
        }
    }

    private static final class DeletedTagValue extends ChangedTagValue {
        private DeletedTagValue(EditableTagValue original) {
            super(original);
        }

        @Override
        public String value() {
            return "-";
        }

        @Override
        public String displayValue() {
            return original.value();
        }

        @Override
        public boolean isDeleted() {
            return true;
        }

        @Override
        protected boolean isStatusEditable() {
            return false;
        }

        @Override
        protected boolean isStatusDeletable() {
            return false;
        }

        @Override
        public EditableTagValue withValue(String value) {
            return this;
        }

        @Override
        public EditableTagValue mergeWithValue(String value) {
            return super.withValue(value);
        }

        @Override
        public EditableTagValue replaceWith(TagValue tagValue) {
            return this;
        }

        @Override
        public Optional<EditableTagValue> delete() {
            return Optional.of(this);
        }
    }

    private static final class ReplacedTagValue extends ChangedTagValue {
        final TagValue replacement;

        private ReplacedTagValue(EditableTagValue original, TagValue replacement) {
            super(original);
            this.replacement = replacement;
        }

        @Override
        public Tag tag() {
            return replacement.tag();
        }

        @Override
        public String value() {
            return replacement.value();
        }

        @Override
        public String displayValue() {
            return replacement.value();
        }

        @Override
        public boolean isDeleted() {
            return false;
        }

        @Override
        public EditableTagValue withValue(String value) {
            return original.replaceWith(TagValue.of(replacement.tag(), value));
        }

        @Override
        public void pushChanges(Consumer<? super TagValue> consumer) {
            original.delete().ifPresent(consumer);
            consumer.accept(this);
        }
    }
}
