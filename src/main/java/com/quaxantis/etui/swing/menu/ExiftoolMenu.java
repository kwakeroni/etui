package com.quaxantis.etui.swing.menu;

import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.swing.actions.FileActions;
import com.quaxantis.etui.swing.actions.ViewActions;

import javax.swing.JMenuBar;

public class ExiftoolMenu extends JMenuBar {

    public ExiftoolMenu(FileActions fileActions, ViewActions viewActions, ConfigOperations configOperations) {
        add(new FileMenu(fileActions, configOperations));
        add(new ViewMenu(viewActions));
    }

}
