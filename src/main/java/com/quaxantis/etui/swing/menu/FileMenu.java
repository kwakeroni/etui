package com.quaxantis.etui.swing.menu;

import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.swing.actions.FileActions;
import com.quaxantis.support.swing.menu.PopupMenuAdapter;
import com.quaxantis.support.swing.menu.UpdatableComponent;
import com.quaxantis.support.swing.menu.UpdatableMenu;
import com.quaxantis.support.swing.menu.UpdatableMenuItem;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.function.Predicate.not;

public class FileMenu extends JMenu {

    public FileMenu(FileActions fileActions, ConfigOperations configOperations) {
        super("File");
        this.setMnemonic('F');

        add(new JMenuItem(fileActions.open()));
        add(openRecentMenu(fileActions, configOperations));
        add(new JMenuItem(fileActions.save()));
        add(new JMenuItem(fileActions.saveAs()));
        addSeparator();
        add(new JMenuItem(fileActions.close()));
        add(new JMenuItem(fileActions.exit()));
//        add(preferencesAction());

        this.getPopupMenu().addPopupMenuListener(PopupMenuAdapter.onPopupMenuWillBecomeVisible(_ -> updateAll()));
    }

    private void updateAll() {
        for (Component component : getMenuComponents()) {
            if (component instanceof UpdatableComponent dynamic) {
                dynamic.update();
            }
        }
    }

    private JMenu openRecentMenu(FileActions actions, ConfigOperations configOperations) {
        return new UpdatableMenu("Open Recent", () ->
                configOperations.getConfiguration().getHistory()
                        .stream()
                        .filter(not(Files::notExists))
                        .map((Path path) -> openRecentAction(path, actions)));
    }

    private JMenuItem openRecentAction(Path path, FileActions actions) {
        return UpdatableMenuItem.withText(path.toString())
                .addAction(event -> actions.doOpen(path, event));
    }
}
