package com.quaxantis.etui.application.file;

import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.exiftool.Exiftool;
import com.quaxantis.etui.exiftool.ExiftoolOutput;
import com.quaxantis.etui.tag.TagRepository;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

class FileOperations {
    private static final Logger log = LoggerFactory.getLogger(FileOperations.class);

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
                .addArgs("-escapeHTML")
                .addArgs("-api", "NoPDFList")
                .setOutput(ExiftoolOutput.toTagSet(this.tagRepository))
                .run();
    }

    void close() {
        handler.closeCurrentTagSet();
    }

    Path saveAs(Path path) {
        return handler.saveCurrentTagSet(path);
    }

    Path save(Path inputPath) {
        Path outputPath;
        try {
            var tempFile = Files.createTempFile("etui", "." + FilenameUtils.getExtension(inputPath.getFileName().toString()));
            outputPath = handler.saveCurrentTagSet(tempFile);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

        if (outputPath == null) {
            return null;
        }

        try {
            Files.move(inputPath, inputPath.resolveSibling(inputPath.getFileName() + "~"), StandardCopyOption.REPLACE_EXISTING);
            Files.move(outputPath, inputPath);
            return inputPath;
        } catch (IOException ioe) {
            log.error("Unable to back up {}", inputPath);
            return outputPath;
        }
    }
}
