package com.quaxantis.etui.application.config;

import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.Set;

public interface ConfigurationListener extends EventListener {

    void onConfigurationChanged(Configuration configuration, Set<Item> items);

    public enum Item {
        VIEW_FILTER;
    }
}
