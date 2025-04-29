package com.quaxantis.etui.application.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface Configuration {
    Optional<Path> getExiftoolExecutable();
    ViewFilter getViewFilter();
    Optional<String[]> getLastSelectedTemplate();

    List<Path> getHistory();
    List<Path> getTagDefinitions();
    List<String> getTemplatePaths();

    void addConfigurationListener(ConfigurationListener listener);

    default void addConfigurationListener(ConfigurationListener.Item item, Consumer<Configuration> listener) {
        addConfigurationListener(item, (config, _) -> listener.accept(config));
    }

    default void addConfigurationListener(ConfigurationListener.Item item, ConfigurationListener listener) {
        addConfigurationListener((configuration, items) -> {
            if (items.contains(item)) {
                listener.onConfigurationChanged(configuration, items);
            }
        });
    }
}
