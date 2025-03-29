package com.quaxantis.etui.application.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

public interface FileState {

    default FileState open(FileOperations operations, Path path, Predicate<String> confirmAction) {
        if (confirmNoChangesLost(confirmAction)) {
            operations.open(path);
            return FileState.unchangedFile(path);
        } else {
            return this;
        }
    }

    default FileState close(FileOperations operations, Predicate<String> confirmAction) {
        if (confirmNoChangesLost(confirmAction)) {
            operations.close();
            return FileState.noFile();
        } else {
            return this;
        }
    }

    default FileState save(FileOperations operations) {
        throw new IllegalStateException("Unsupported operation in state " + this.getClass().getSimpleName());
    }

    default FileState saveAs(FileOperations operations, Path path, Predicate<String> confirmAction) {
        throw new IllegalStateException("Unsupported operation in state " + this.getClass().getSimpleName());
    }

    default FileState onChanged() {
        throw new IllegalStateException("Unsupported operation in state " + this.getClass().getSimpleName());
    }

    default FileState onRevert() {
        throw new IllegalStateException("Unsupported operation in state " + this.getClass().getSimpleName());
    }

    default Optional<Path> getOpenFile() {
        return Optional.empty();
    }

    default boolean hasOpenFileChanged() {
        return false;
    }

    default boolean confirmNoChangesLost(Predicate<String> confirmAction) {
        return true;
    }

    private static boolean confirmNoChangesLost(FileState currentState, Predicate<String> confirmAction) {
        return confirmAction.test("There are unsaved changes in the current file. Any changes will be lost.\n\nDo you want to continue ?");
    }

    private static FileState save(FileState currentState,  FileOperations operations) {
        var inputPath = currentState.getOpenFile().orElseThrow();
        Path savedpath = operations.save(inputPath);
        if (savedpath != null) {
            return currentState.open(operations, savedpath, _ -> true);
        } else {
            System.err.println("File could not be saved");
        }
        return currentState;
    }


    private static FileState saveAs(FileState currentState, FileOperations operations, Path destination, Predicate<String> confirmAction) {
        if (Files.notExists(destination) ||
            confirmAction.test("%s already exists.\n\nDo you want to replace it ?".formatted(destination.getFileName()))) {
            Path savedpath = operations.saveAs(destination);
            if (savedpath != null) {
                return currentState.open(operations, savedpath, _ -> true);
            } else {
                System.err.println("File could not be saved");
            }
        }
        return currentState;
    }


    static FileState noFile() {
        class NoFile implements FileState {
            @Override
            public String toString() {
                return "NoFile[]";
            }
        }
        return new NoFile();
    }

    private static FileState unchangedFile(Path file) {
        class UnchangedFile implements FileState {
            @Override
            public Optional<Path> getOpenFile() {
                return Optional.of(file);
            }

            @Override
            public FileState onChanged() {
                return FileState.changedFile(file);
            }

            @Override
            public FileState onRevert() {
                return this;
            }

            @Override
            public FileState saveAs(FileOperations operations, Path path, Predicate<String> confirmAction) {
                return FileState.saveAs(this, operations, path, confirmAction);
            }

            @Override
            public String toString() {
                return "Unchanged[%s]".formatted(file);
            }
        }

        return new UnchangedFile();
    }

    private static FileState changedFile(Path file) {
        class ChangedFile implements FileState {
            @Override
            public boolean confirmNoChangesLost(Predicate<String> confirmAction) {
                return FileState.confirmNoChangesLost(this, confirmAction);
            }

            @Override
            public Optional<Path> getOpenFile() {
                return Optional.of(file);
            }

            @Override
            public FileState onChanged() {
                return this;
            }

            @Override
            public FileState onRevert() {
                return FileState.unchangedFile(file);
            }

            @Override
            public boolean hasOpenFileChanged() {
                return true;
            }

            @Override
            public FileState save(FileOperations operations) {
                return FileState.save(this, operations);
            }

            @Override
            public FileState saveAs(FileOperations operations, Path path, Predicate<String> confirmAction) {
                return FileState.saveAs(this, operations, path, confirmAction);
            }

            @Override
            public String toString() {
                return "Changed[%s]".formatted(file);
            }
        }

        return new ChangedFile();
    }


}
