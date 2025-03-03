package com.quaxantis.etui.swing.menu;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.function.Predicate;

import static com.quaxantis.support.swing.Events.windowAncestor;

public class Confirmation {

    private ActionEvent event;
    private String title;

    private Confirmation() {

    }

    public static Confirmation on(ActionEvent event) {
        var confirmation = new Confirmation();
        confirmation.event = event;
        return confirmation;
    }

    public Confirmation confirm(String action) {
        this.title = "Confirm " + action;
        return this;
    }

    public boolean showMessage(String message) {
        return isConfirmed(windowAncestor(event).orElse(null), title, message);
    }

    public static Predicate<String> confirmAction(Component parent, String title) {
        return message -> isConfirmed(parent, title, message);
    }

    private static boolean isConfirmed(Component parent, String title, String message) {
        int choice = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        boolean isConfirmed = choice == JOptionPane.OK_OPTION;
        return isConfirmed;
    }
}
