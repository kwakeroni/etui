package com.quaxantis.etui.exiftool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.TagValue;
import com.quaxantis.etui.tag.TagRepository;
import org.apache.commons.text.StringEscapeUtils;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
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
            return (format == null) ? Exiftool.CliArgs.none() : this.format;
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

    public static ExiftoolOutput<TagSet> toTagSet(TagRepository tagRepository) {
        return new TagSetOutput(tagRepository);
    }

    private static final class TagSetOutput extends ExiftoolOutput<TagSet> implements Exiftool.Output<TagSet> {

        private final TagRepository tagRepository;
        private final Path tempFile;

        public TagSetOutput(TagRepository tagRepository) {
            this.tagRepository = tagRepository;
            try {
                this.tempFile = Files.createTempFile("etui", ".out");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public Exiftool.CliArgs args() {
            return Exiftool.OutputFormat.JSON.and(Exiftool.CliArgs.of("-G"));
        }

        @Override
        public ProcessBuilder.Redirect redirectOutput() {
            return ProcessBuilder.Redirect.to(this.tempFile.toFile());
        }

        @Override
        public void handleError(Process process) {
            try {
                Files.copy(this.tempFile, System.out);
            } catch (IOException e) {
                System.out.println("Unable to output process log");
            } finally {
                try {
                    Files.delete(this.tempFile);
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        @Override
        public TagSet handleOutput(Process process) throws IOException {
            try (Reader reader = Files.newBufferedReader(this.tempFile)) {
                return toTagSet(reader, this.tagRepository);
            } finally {
                try {
                    Files.delete(this.tempFile);
                } catch (IOException e) {
                    // Ignore
                }
            }
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

            UnaryOperator<Tag> enrich = (tagRepository == null) ? UnaryOperator.identity() : tagRepository.cached()::enrichTag;
            Spliterator<Map.Entry<String, JsonNode>> spliterator = Spliterators.spliterator(json.fields(), json.size(), Spliterator.NONNULL);
            return StreamSupport.stream(spliterator, false)
                    .map(entry -> ofJsonKey(entry.getKey(), entry.getValue(), enrich))
                    .collect(TagSet.toTagSet());
        }

        private static TagValue ofJsonKey(String key, JsonNode jsonValue, UnaryOperator<Tag> enrich) {
            String value = StringEscapeUtils.unescapeHtml4(jsonValue.asText());
            int colon = key.indexOf(':');
            Tag tag = (colon < 0) ? Tag.of(null, key) : Tag.of(key.substring(0, colon), key.substring(colon + 1));
            return TagValue.of(enrich.apply(tag), value);
        }
    }

    private static class SpyingReader extends FilterReader {
        public SpyingReader(Reader in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int c = super.read();
            System.out.print((char) c);
            return c;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int count = super.read(cbuf, off, len);
            if (count > 0) {
                System.out.print(new String(cbuf, off, count));
            } else if (count < 0) {
                System.out.println();
            }
            return count;
        }
    }
}
