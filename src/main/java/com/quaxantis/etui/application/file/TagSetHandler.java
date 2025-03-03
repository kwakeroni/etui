package com.quaxantis.etui.application.file;

import com.quaxantis.etui.TagSet;

import javax.annotation.Nullable;
import java.nio.file.Path;

public interface TagSetHandler {
    void loadTagSet(Path file, TagSet tagSet);

    @Nullable
    Path saveCurrentTagSet(Path file);

    void closeCurrentTagSet();
}
