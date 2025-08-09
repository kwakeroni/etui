package com.quaxantis.etui.application.file;

import com.quaxantis.etui.application.State;
import com.quaxantis.etui.application.StateChangeListener;
import com.quaxantis.etui.tag.TagRepository;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class FileStateMachine {
    private final FileOperations fileOperations;
    private final State<FileState> state = new State<>(FileState.noFile());
    // TODO replace with 'before' StateChangeListener
    private final Set<Runnable> closeFileHandles = new HashSet<>();

    public FileStateMachine(TagSetHandler handler, TagRepository tagRepository) {
        this.fileOperations = new FileOperations(handler, tagRepository);
    }

    public void whenNoChangesLost(Predicate<String> confirmAction, Runnable action) {
        if (this.state.get(state -> state.confirmNoChangesLost(confirmAction))) {
            action.run();
        }
    }

    public void open(Path path, Predicate<String> confirmAction) {
        this.state.transition(state -> state.open(fileOperations, path, confirmAction));
    }

    public void close(Predicate<String> confirmAction) {
        this.state.transition(state -> state.close(fileOperations, confirmAction));
    }

    public void save(Predicate<String> confirmAction) {
        this.closeFileHandles.forEach(Runnable::run);
        this.state.transition(fileState -> fileState.save(fileOperations, confirmAction));
    }

    public void saveAs(Path path, Predicate<String> confirmAction) {
        this.state.transition(state -> state.saveAs(fileOperations, path, confirmAction));
    }

    public void onChanged() {
        this.state.transition(FileState::onChanged);
    }

    public void onRevert() {
        this.state.transition(FileState::onRevert);
    }

    public boolean hasOpenFile() {
        return getOpenFile().isPresent();
    }

    public Optional<Path> getOpenFile() {
        return this.state.get(FileState::getOpenFile);
    }

    public boolean hasOpenFileChanged() {
        return this.state.get(FileState::hasOpenFileChanged);
    }

    public void registerListener(Runnable listener) {
        state.registerListener((_, _) -> listener.run());
    }

    public void registerListener(StateChangeListener<? super FileState> listener) {
        state.registerListener(listener);
    }

    public void unregisterListener(StateChangeListener<? super FileState> listener) {
        state.unregisterListener(listener);
    }

    public void registerCloseFileHandle(Runnable runnable) {
        this.closeFileHandles.add(runnable);
    }

    public void unRegisterCloseFileHandle(Runnable runnable) {
        this.closeFileHandles.remove(runnable);
    }
}
