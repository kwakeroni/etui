package com.quaxantis.etui.template;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagDescriptor;
import com.quaxantis.etui.Template;

import javax.annotation.Nonnull;
import java.util.List;

public class VariableSupport implements Template.Variable, TagDescriptor {
    private final String name;
    private final TagDescriptor descriptor;

    public VariableSupport(@Nonnull Tag tag) {
        this(tag.qualifiedName(), tag);
    }
    public VariableSupport(String name, @Nonnull Tag tag) {
        this.name = name;
        this.descriptor = TagDescriptor.of(tag);
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
        var label = descriptor.label();
        return (label != null)?  label : descriptor.tag().tagName();
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
        return null;
    }

    @Override
    public boolean hasExpression() {
        return false;
    }
}
