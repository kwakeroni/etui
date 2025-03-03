package com.quaxantis.etui.swing;

import javax.swing.JTabbedPane;
import java.util.EventListener;

public interface TabListener extends EventListener {
    void beforeTabSelected(JTabbedPane tabbedPane, int index);
}
