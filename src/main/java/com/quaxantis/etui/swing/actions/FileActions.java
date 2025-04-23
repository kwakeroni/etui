package com.quaxantis.etui.swing.actions;

import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.application.file.FileStateMachine;
import com.quaxantis.etui.swing.menu.ActionBuilder;
import com.quaxantis.etui.swing.menu.Confirmation;

import javax.annotation.Nullable;
import javax.swing.Action;
import javax.swing.JFileChooser;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import static com.quaxantis.etui.swing.menu.Confirmation.confirmAction;
import static com.quaxantis.support.swing.Events.windowAncestor;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;

public final class FileActions {

    private final Action open;
    private final Action close;
    private final Action save;
    private final Action saveAs;
    private final Action exit;

    private final FileStateMachine fileStateMachine;
    private final ConfigOperations configOperations;

    public FileActions(FileStateMachine fileStateMachine, ConfigOperations configOperations) {
        this.fileStateMachine = fileStateMachine;
        this.configOperations = configOperations;

        this.open = ActionBuilder.withName("Open")
                .withMnemonic('O')
                .withAccelerator('O')
                .withAction(event -> fileStateMachine.whenNoChangesLost(
                        Confirmation.on(event).confirm("Open file")::showMessage,
                        () -> forceOpenDialog(windowAncestor(event).orElse(null))
                ));

        this.close = ActionBuilder.withName("Close")
                .withMnemonic('C')
                .withAccelerator('W')
                .enabledIf(fileStateMachine::hasOpenFile, fileStateMachine::registerListener)
                .withAction(event -> fileStateMachine.close(
                        Confirmation.on(event).confirm("Close")::showMessage));

        this.save = ActionBuilder.withName("Save")
                .withMnemonic('S')
                .withAccelerator('S')
                .enabledIf(fileStateMachine::hasOpenFileChanged, fileStateMachine::registerListener)
                .withAction(event -> fileStateMachine.save(confirmAction(windowAncestor(event).orElse(null), "Confirm Save")));

        this.saveAs = ActionBuilder.withName("Save as...")
                .withMnemonic('A')
                .withAccelerator('S', SHIFT_DOWN_MASK)
                .enabledIf(fileStateMachine::hasOpenFile, fileStateMachine::registerListener)
                .withAction(event -> saveAsDialog(windowAncestor(event).orElse(null)));

        this.exit = ActionBuilder.withName("Exit")
                .withMnemonic('E')
                .withAccelerator('Q')
                .withAction(event -> fileStateMachine.whenNoChangesLost(
                        Confirmation.on(event).confirm("Exit")::showMessage,
                        () -> windowAncestor(event).ifPresent(Window::dispose)
                ));
    }

    public Action open() {
        return open;
    }

    public Action close() {
        return close;
    }

    public void doOpen(Path path, ActionEvent event) {
        fileStateMachine.open(path, Confirmation.on(event).confirm("Open " + path.getFileName())::showMessage);
    }

    public Action save() {
        return save;
    }

    public Action saveAs() {
        return saveAs;
    }

    public Action exit() {
        return exit;
    }
    
    private void forceOpenDialog(@Nullable Component parent) {
        fileDialog(chooser -> chooser.showOpenDialog(parent),
                   file -> this.fileStateMachine.open(file, _ -> true));
    }

    private void saveAsDialog(@Nullable Component parent) {
        fileDialog(chooser -> chooser.showSaveDialog(parent),
                   path -> fileStateMachine.saveAs(path, confirmAction(parent, "Confirm Save As")));
    }

    private void fileDialog(ToIntFunction<JFileChooser> showDialog, Consumer<Path> action) {
        var chooser = newFileChooser();
        int choice = showDialog.applyAsInt(chooser);
        if (choice == JFileChooser.APPROVE_OPTION) {
            Path file = chooser.getSelectedFile().toPath();
            action.accept(file);
        }
    }

    private JFileChooser newFileChooser() {
        var chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);

        Optional<Path> mostRecentFile = configOperations.getConfiguration().getHistory().stream().findFirst();
        mostRecentFile.map(Path::getParent).map(Path::toFile).ifPresent(chooser::setCurrentDirectory);
        return chooser;
    }

}
