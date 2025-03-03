package com.quaxantis.etui.swing.template;

import com.quaxantis.etui.Template;

import java.util.List;

public record TemplateGroup(String name, List<TemplateGroup> subGroups, List<Template> templates) {
    public TemplateGroup {
        subGroups = List.copyOf(subGroups);
        templates = List.copyOf(templates);
    }
}
