package com.quaxantis.etui.application.file;

import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.exiftool.Exiftool;
import com.quaxantis.etui.exiftool.ExiftoolOutput;
import com.quaxantis.etui.tag.TagRepository;

import java.nio.file.Path;

class FileOperations {
    private final TagSetHandler handler;
    private final TagRepository tagRepository;

    FileOperations(TagSetHandler handler, TagRepository tagRepository) {
        this.handler = handler;
        this.tagRepository = tagRepository;
    }

    void open(Path path) {
        var tagSet = readTagSet(path);
        handler.loadTagSet(path, tagSet);
    }

    private TagSet readTagSet(Path path) {
        return Exiftool.onFile(path)
                .setOutput(ExiftoolOutput.toTagSet(this.tagRepository))
                .run();
    }

    void close() {
        handler.closeCurrentTagSet();
    }

    Path saveAs(Path path) {
        return handler.saveCurrentTagSet(path);
    }
}
