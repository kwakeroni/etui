package com.quaxantis.etui.swing.menu;

import com.quaxantis.etui.swing.actions.ViewActions;

import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

public class ViewMenu extends JMenu {
    public ViewMenu(ViewActions viewActions) {
        super("View");
        this.setMnemonic('V');
        add(viewActions.hideReadOnly(JCheckBoxMenuItem::new).apply(AbstractButton::setSelected));
    }
}
