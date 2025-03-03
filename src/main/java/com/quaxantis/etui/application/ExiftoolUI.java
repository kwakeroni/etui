package com.quaxantis.etui.application;

import com.quaxantis.etui.TagSet;

import java.nio.file.Path;
import java.util.Optional;

public interface ExiftoolUI {
    void display();

    void loadTagSet(Path path, TagSet tagSet);

    void closeCurrentTagSet();

    Optional<TagSet> getChanges();
}
