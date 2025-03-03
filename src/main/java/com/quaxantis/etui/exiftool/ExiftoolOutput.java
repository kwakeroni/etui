package com.quaxantis.etui.exiftool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.TagValue;
import com.quaxantis.etui.tag.TagRepository;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.UnaryOperator;
import java.util.stream.StreamSupport;

public sealed abstract class ExiftoolOutput<R> {

    abstract Exiftool.Output<R> toOutput();

    public static ExiftoolOutput<Void> inheritSystemIO() {
        return InheritSystemIO.INSTANCE;
    }

    public static ExiftoolOutput<Void> inheritSystemIO(Exiftool.OutputFormat outputFormat) {
        return new InheritSystemIO(outputFormat);
    }

    private static final class InheritSystemIO extends ExiftoolOutput<Void> implements Exiftool.Output<Void> {
        static InheritSystemIO INSTANCE = new InheritSystemIO(null);

        private final Exiftool.OutputFormat format;

        private InheritSystemIO(Exiftool.OutputFormat format) {
            this.format = format;
        }

        @Override
        public Exiftool.CliArgs args() {
            return (format == null)? Exiftool.CliArgs.none() : this.format;
        }

        @Override
        public ProcessBuilder.Redirect redirectOutput() {
            return ProcessBuilder.Redirect.INHERIT;
        }

        @Override
        public Void handleOutput(Process process) {
            return null;
        }

        @Override
        Exiftool.Output<Void> toOutput() {
            return this;
        }
    }

    public static ExiftoolOutput<Path> toFile() throws IOException {
        return toFile(Files.createTempFile("exiftool", ".txt"));
    }

    public static ExiftoolOutput<Path> toFile(Path destination) {
        return new FileOutput(destination);
    }

    private static final class FileOutput extends ExiftoolOutput<Path> implements Exiftool.Output<Path> {
        private final Path destination;

        private FileOutput(Path destination) {
            this.destination = destination;
        }

        @Override
        public Exiftool.CliArgs args() {
            return Exiftool.CliArgs.none();
        }

        @Override
        public ProcessBuilder.Redirect redirectOutput() {
            return ProcessBuilder.Redirect.to(destination.toFile());
        }

        @Override
        public Path handleOutput(Process process) throws IOException {
            return destination;
        }

        @Override
        Exiftool.Output<Path> toOutput() {
            return this;
        }
    }

    public static ExiftoolOutput<TagSet> toTagSet() {
        return TagSetOutput.INSTANCE_PLAIN;
    }

    public static ExiftoolOutput<TagSet> toTagSet(TagRepository tagRepository) {
        return new TagSetOutput(tagRepository);
    }

    private static final class TagSetOutput extends ExiftoolOutput<TagSet> implements Exiftool.Output<TagSet> {
        private static final TagSetOutput INSTANCE_PLAIN = new TagSetOutput(null);

        private final TagRepository tagRepository;

        public TagSetOutput(TagRepository tagRepository) {
            this.tagRepository = tagRepository;
        }

        @Override
        public Exiftool.CliArgs args() {
            return Exiftool.OutputFormat.JSON.and(Exiftool.CliArgs.of("-G"));
        }

        @Override
        public ProcessBuilder.Redirect redirectOutput() {
            return ProcessBuilder.Redirect.PIPE;
        }

        @Override
        public TagSet handleOutput(Process process) throws IOException {
            return toTagSet(process.inputReader(), this.tagRepository);
        }

        @Override
        Exiftool.Output<TagSet> toOutput() {
            return this;
        }

        private static TagSet toTagSet(Reader reader, TagRepository tagRepository) throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonArray = objectMapper.readTree(reader);
//            System.out.println(jsonArray);

            if (!jsonArray.isArray()) {
                throw new IllegalStateException("Expected exiftool output to be an array");
            }
            if (jsonArray.size() != 1) {
                throw new IllegalStateException("Expected exiftool output to be an array with size 1 but was " + jsonArray.size());
            }
            JsonNode json = jsonArray.get(0);
            if (!json.isObject()) {
                throw new IllegalStateException("Expected exiftool output to be an array with an object");
            }

            UnaryOperator<Tag> enrich = (tagRepository == null)? UnaryOperator.identity() : tagRepository::enrichTag;
            Spliterator<Map.Entry<String, JsonNode>> spliterator = Spliterators.spliterator(json.fields(), json.size(), Spliterator.NONNULL);
            return StreamSupport.stream(spliterator, false)
                    .map(entry -> ofJsonKey(entry.getKey(), entry.getValue(), enrich))
                    .collect(TagSet.toTagSet());
        }

        private static TagValue ofJsonKey(String key, JsonNode value, UnaryOperator<Tag> enrich) {
            int colon = key.indexOf(':');
            Tag tag = (colon < 0)? Tag.of(null, key) : Tag.of(key.substring(0, colon), key.substring(colon + 1));
            return TagValue.of(enrich.apply(tag), value.asText());
        }
    }
}
