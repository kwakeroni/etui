package com.quaxantis.etui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

class TagDescriptorAdapter implements TagDescriptor, Tag {
    private final TagDescriptor delegate;

    public TagDescriptorAdapter(TagDescriptor delegate) {
        this.delegate = delegate;
    }

    public List<? extends TagDescriptor.Example> examples() {
        return delegate.examples();
    }

    public Tag tag() {
        return delegate.tag();
    }

    @Nullable
    @Override
    public String groupName() {
        return tag().groupName();
    }

    @Nonnull
    @Override
    public String tagName() {
        return tag().tagName();
    }

    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    public String formatDescription() {
        return delegate.formatDescription();
    }

    public String label() {
        return delegate.label();
    }

    public String description() {
        return delegate.description();
    }
}
