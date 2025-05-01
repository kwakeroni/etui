package com.quaxantis.etui;

import java.util.List;
import java.util.Optional;

abstract class TagFamilyAdapter implements TagFamily {
    private final TagFamily delegate;

    protected TagFamilyAdapter(TagFamily delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<String> defaultGroup() {
        return delegate.defaultGroup();
    }

    @Override
    public boolean defaultReadOnly() {
        return delegate.defaultReadOnly();
    }

    @Override
    public List<? extends Tag> tags() {
        return delegate.tags();
    }

    @Override
    public String label() {
        return delegate.label();
    }
}
