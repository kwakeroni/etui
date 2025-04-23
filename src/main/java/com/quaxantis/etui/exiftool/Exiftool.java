package com.quaxantis.etui.exiftool;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Exiftool<R> {
    public static final String PROPERTY_EXIFTOOL_EXECUTABLE = "exiftool.executable";
    public static final String ENVVAR_EXIFTOOL_EXECUTABLE = "EXIFTOOL_EXECUTABLE";
    private final List<CliArgs> additionalArgs = new ArrayList<>();
    private Path executable;
    private Path inputFile;
    //    private CliArgs outputFormat = CliArgs.none();
//    private ThrowingFunction<Process, R, IOException> outputFunction;
    private Output<R> output;

    private Exiftool(Path inputFile, Output<R> output) {
        this.inputFile = inputFile;
        this.output = output;
        this.executable = detectExecutable().orElse(null);
    }

    private Stream<CliArgs> allArgs() {
        return Stream.concat(
                Stream.concat(
                        Stream.of(
                                CliArgs.of(executable.toString()),
                                output.args()),
                        additionalArgs.stream()),
                Stream.of(CliArgs.of(inputFile.toString())));
    }

    public static Exiftool<Void> onFile(Path inputFile) {
        Objects.requireNonNull(inputFile, "inputFile");
        return new Exiftool<>(inputFile, ExiftoolOutput.inheritSystemIO().toOutput());
    }

    public Exiftool<R> setExecutable(Path executable) {
        this.executable = Objects.requireNonNull(executable, "executable");
        return this;
    }

    public <S> Exiftool<S> setOutput(ExiftoolOutput<S> output) {
        Objects.requireNonNull(output, "output");
        @SuppressWarnings("unchecked") // Dirty way of changing the output type
        Exiftool<S> self = (Exiftool<S>) this;
        self.output = output.toOutput();
        return self;
    }

    public Exiftool<R> addArgs(String arg0, String... remainder) {
        this.additionalArgs.add(CliArgs.of(arg0, remainder));
        return this;
    }

    public R run() {
        if (this.executable == null) {
            throw new ExiftoolException("""
                                        Exiftool executable could not be detected. \
                                        Set the system property '%s' or the environment variable '%s' \
                                        to point to the executable.
                                        """.formatted(PROPERTY_EXIFTOOL_EXECUTABLE, ENVVAR_EXIFTOOL_EXECUTABLE));
        }

        var command = allArgs().mapMulti(CliArgs::addArgs).toList();
        Process process;
        try {
            process = new ProcessBuilder()
                    .command(command)
                    .inheritIO()
                    .redirectOutput(output.redirectOutput())
                    .start();
        } catch (Exception exc) {
            throw new ExiftoolException("Error starting Exiftool", exc, command);
        }
        try {
            process.waitFor(30, TimeUnit.SECONDS);
        } catch (Exception exc) {
            output.handleError(process);
            throw new ExiftoolException("Error while executing Exiftool", exc, command);
        }
        if (process.exitValue() != 0) {
            output.handleError(process);
            throw new ExiftoolException("Exiftool exited with error status=" + process.exitValue(), command);
        }
        try {
            return output.handleOutput(process);
        } catch (Exception exc) {
            throw new ExiftoolException("Error while parsing Exiftool output", exc, command);
        }
    }

    private static Optional<Path> detectExecutable() {
        return Optional.ofNullable(System.getProperty(PROPERTY_EXIFTOOL_EXECUTABLE))
                .or(() -> Optional.ofNullable(System.getenv(PROPERTY_EXIFTOOL_EXECUTABLE)))
                .or(() -> Optional.ofNullable(System.getenv(ENVVAR_EXIFTOOL_EXECUTABLE)))
                .map(Path::of);
    }


    static interface CliArgs {
        void addArgs(Consumer<? super String> consumer);

        default CliArgs and(CliArgs otherArgs) {
            return consumer -> {
                this.addArgs(consumer);
                otherArgs.addArgs(consumer);
            };
        }

        static CliArgs none() {
            return consumer -> {
            };
        }

        static CliArgs of(String arg) {
            return consumer -> consumer.accept(arg);
        }

        static CliArgs of(String arg0, String... remainder) {
            return consumer -> {
                consumer.accept(arg0);
                for (String arg : remainder) {
                    consumer.accept(arg);
                }
            };
        }

        static CliArgs of(Iterable<String> iterable) {
            return iterable::forEach;
        }
    }

    interface Output<R> {
        CliArgs args();

        ProcessBuilder.Redirect redirectOutput();

        R handleOutput(Process process) throws IOException;

        default void handleError(Process process) {

        }
    }

    public static enum OutputFormat implements CliArgs {
        ARGS("-argFormat"),
        BINARY("-b"),
        CSV("-csv"),
        HTML("-htmlFormat"),
        JSON("-json"),
        PHP_ARRAY("-php"),
        SHORT("-short"),
        VERY_SHORT("-veryShort"),
        TAB("-tab"),
        TABLE("-table"),
        XML("-xmlFormat");
        private final String option;

        private OutputFormat(String option) {
            this.option = option;
        }


        @Override
        public void addArgs(Consumer<? super String> consumer) {
            consumer.accept(option);
        }
    }
}
