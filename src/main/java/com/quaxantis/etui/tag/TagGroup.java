package com.quaxantis.etui.tag;

import com.quaxantis.etui.TagFamily;

import java.util.List;

public record TagGroup(String name, List<TagGroup> subGroups, List<TagFamily> families) {
    public TagGroup {
        subGroups = List.copyOf(subGroups);
        families = List.copyOf(families);
    }
}
