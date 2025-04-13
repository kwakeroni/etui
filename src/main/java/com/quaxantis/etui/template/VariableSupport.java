package com.quaxantis.etui.template;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagDescriptor;
import com.quaxantis.etui.Template;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class VariableSupport implements Template.Variable, TagDescriptor {
    private final String name;
    private final TagDescriptor descriptor;
    private final Optional<Template.Variable> delegate;

    public VariableSupport(@Nonnull Tag tag) {
        this(tag.qualifiedName(), tag);
    }
    private VariableSupport(String name, @Nonnull Tag tag) {
        this.name = name;
        this.descriptor = TagDescriptor.of(tag);
        this.delegate = Optional.empty();
    }
    public VariableSupport(Template.Variable variable, Tag tag) {
        this.name = variable.name();
        this.descriptor = TagDescriptor.of(tag);
        this.delegate = Optional.of(variable);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Tag tag() {
        return descriptor.tag();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String label() {
        return this.delegate.map(Template.Variable::label)
                .or(() -> Optional.ofNullable(descriptor.label()))
                .orElseGet(() -> descriptor.tag().tagName());
    }

    @Override
    public String description() {
        return descriptor.description();
    }

    @Override
    public String formatDescription() {
        return descriptor.formatDescription();
    }

    @Override
    public List<? extends Example> examples() {
        return descriptor.examples();
    }

    @Override
    public String toString() {
        return descriptor.toString();
    }

    @Override
    public String expression() {
        return delegate.map(Template.Variable::expression).orElse(null);
    }

    @Override
    public boolean hasExpression() {
        return delegate.map(Template.Variable::hasExpression).orElse(false);
    }
}
