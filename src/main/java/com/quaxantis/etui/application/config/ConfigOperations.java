package com.quaxantis.etui.application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class ConfigOperations {
    private static final Logger log = LoggerFactory.getLogger("Config");

    private final Path path;
    private final ConfigurationImpl configuration;

    public ConfigOperations() {
        this(defaultConfig());
    }

    public ConfigOperations(Path path) {
        this.path = path;
        this.configuration = ConfigurationImpl.of(loadOptional(path), path.getParent());
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public void setViewFilter(UnaryOperator<ViewFilter> operator) {
        setViewFilter(operator.apply(this.configuration.getViewFilter()));
    }

    public void setViewFilter(ViewFilter viewFilter) {
        store(config -> config.setViewFilter(viewFilter));
    }

    public void setLastSelectedTemplate(String[] path) {
        store(config -> config.setLastSelectedTemplate(path));
    }

    public void pushHistoryEntry(Path path) {
        store(config -> config.pushHistoryEntry(path));
    }

    private void store(Consumer<ConfigurationImpl> modification) {
        modification.accept(this.configuration);
        storeConfiguration();
    }

    private void storeConfiguration() {
        try {
            Files.createDirectories(this.path.getParent());
            try (Writer writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8)) {
                this.configuration.toProperties().store(writer, null);
            }
        } catch (IOException ioe) {
            log.warn("Unable to store config file {}: {}", path, ioe.toString());
        }
    }

    private static Properties loadOptional(Path path) {
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }
        return properties;
    }

    private static Path defaultConfig() {
        String homeDir = System.getProperty("user.home", ".");
        return Path.of(homeDir).resolve(".etui/config.properties");
    }
}
