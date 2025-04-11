package com.quaxantis.etui.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.quaxantis.etui.Diff;
import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.application.file.FileStateMachine;
import com.quaxantis.etui.application.file.TagSetHandler;
import com.quaxantis.etui.exiftool.Exiftool;
import com.quaxantis.etui.exiftool.ExiftoolOutput;
import com.quaxantis.etui.swing.ExiftoolSwingUI;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.etui.template.TemplateRepository;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class EtuiApplication implements TagSetHandler {


    static {
        System.setProperty(Exiftool.PROPERTY_EXIFTOOL_EXECUTABLE, "C:/Programs/bin/exiftool");
    }

    public static void main(String[] args) {
        main((args.length == 0) ? null : Path.of(args[0]));
    }

    public static void main(@Nullable Path path) {
        var application = new EtuiApplication();
        if (path != null) {
            application.fileStateMachine.open(path, _ -> true);
        } else {
            // TODO Remove
            var history = application.configOperations.getConfiguration().getHistory();
            history.stream()
                    .filter(Files::exists)
                    .findFirst()
                    .ifPresent(file -> application.fileStateMachine.open(file, _ -> true));
        }
        application.ui.display();
    }

    private final ConfigOperations configOperations;
    private final FileStateMachine fileStateMachine;
    private final TagRepository tagRepository;
    private final ExiftoolUI ui;

    public EtuiApplication() {
        this.configOperations = new ConfigOperations();
        this.tagRepository = new TagRepository(this.configOperations.getConfiguration());
        this.fileStateMachine = new FileStateMachine(this, tagRepository);
        var templateRepository = new TemplateRepository(this.configOperations.getConfiguration(), this.tagRepository);
        this.ui = new ExiftoolSwingUI(this.configOperations, this.fileStateMachine, this.tagRepository, templateRepository);
    }

    @Override
    public void loadTagSet(Path file, TagSet tagSet) {
        configOperations.pushHistoryEntry(file);
        ui.loadTagSet(file, tagSet);
    }

    @Override
    public void closeCurrentTagSet() {
        ui.closeCurrentTagSet();
    }

    @Nullable
    @Override
    public Path saveCurrentTagSet(Path file) {
        return ui.getChanges()
                .map(changes -> saveChanges(changes, file))
                .orElse(null);
    }

    private Path saveChanges(TagSet changes, Path output) {
        try {
            Files.deleteIfExists(output);
            writeChanges(changes, output);
            printDiff(output);
            return output;
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }


    private void writeChanges(TagSet changes, Path output) throws IOException {
        System.out.println("--- Changes");
        changes
                .forEach(System.out::println);
        System.out.println("---");

        var objectMapper = new ObjectMapper();
        var jsonFactory = new JsonNodeFactory(true);
        var jsonObject = jsonFactory.objectNode();
        changes.forEach(tag -> jsonObject.put(tag.groupName() + ":" + tag.tagName(), StringEscapeUtils.escapeHtml4(tag.value())));

        Path tempFile = Files.createTempFile(FilenameUtils.getBaseName(output.getFileName().toString()) + "-tags", ".json");
        try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            objectMapper.writeValue(writer, jsonObject);
        }

        var inputPath = fileStateMachine.getOpenFile().orElseThrow();
        Exiftool.onFile(inputPath)
                .addArgs("-f")
                .addArgs("-escapeHTML")
                .addArgs("-json=" + tempFile)
                .addArgs("-out", output.toString())
                .run();

        if (!Files.exists(output)) {
            throw new IllegalArgumentException("File does not exist: " + output);
        }

    }

    private void printDiff(Path output) throws IOException {
        var inputPath = fileStateMachine.getOpenFile().orElseThrow();
        Path inTags = Exiftool.onFile(inputPath)
                .addArgs("-G")
                .setOutput(ExiftoolOutput.toFile())
                .run();
        Path outTags = Exiftool.onFile(output)
                .addArgs("-G")
                .setOutput(ExiftoolOutput.toFile())
                .run();

        Diff.diff(inTags, outTags);
    }
}
