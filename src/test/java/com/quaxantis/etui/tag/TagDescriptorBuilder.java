package com.quaxantis.etui.tag;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagDescriptorBuilder implements Tag, TagDescriptor {
    private Tag tag;
    private boolean readOnly;
    private String label;
    private String description;
    private String formatDescription;
    private List<Example> examples = new ArrayList<>();

    public static TagDescriptorBuilder of(Tag tag) {
        return new TagDescriptorBuilder(tag);
    }

    private TagDescriptorBuilder(Tag tag) {
        this.tag = tag;
    }

    @Override
    public Tag tag() {
        return tag;
    }

    @Nullable
    @Override
    public String groupName() {
        return tag.groupName();
    }

    @Nonnull
    @Override
    public String tagName() {
        return tag.tagName();
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    public TagDescriptorBuilder readOnly() {
        return this.readOnly(true);
    }

    public TagDescriptorBuilder readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    @Override
    public String label() {
        return this.label;
    }

    public TagDescriptorBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

    @Override
    public String description() {
        return this.description;
    }

    public TagDescriptorBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String formatDescription() {
        return this.formatDescription;
    }

    public TagDescriptorBuilder withFormatDescription(String formatDescription) {
        this.formatDescription = formatDescription;
        return this;
    }

    @Override
    public List<? extends Example> examples() {
        return Collections.unmodifiableList(this.examples);
    }

    public TagDescriptorBuilder addExample(Example example) {
        this.examples.add(example);
        return this;
    }

    public TagDescriptorBuilder addExample(String text) {
        return addExample(text, null);
    }

    public TagDescriptorBuilder addExample(String text, String pattern) {
        return addExample(new Example() {
            @Override
            public String text() {
                return text;
            }

            @Override
            public String pattern() {
                return pattern;
            }
        });
    }

}
