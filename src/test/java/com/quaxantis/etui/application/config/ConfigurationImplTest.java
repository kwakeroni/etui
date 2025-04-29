package com.quaxantis.etui.application.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static com.quaxantis.etui.application.config.ConfigurationImpl.escapeStringArray;
import static com.quaxantis.etui.application.config.ConfigurationImpl.unescapeStringArray;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Configuration")
class ConfigurationImplTest {
    private static final Path ROOT = Path.of("/").toAbsolutePath();

    private static ConfigurationImpl emptyConfiguration() {
        return ConfigurationImpl.of(new Properties(), Path.of("."));
    }
    private static ConfigurationImpl configuration(String property, String value) {
        return configuration(Map.of(property, value));
    }

    private static ConfigurationImpl configuration(Map<String, String> map) {
        var properties = new Properties();
        map.forEach(properties::setProperty);
        return ConfigurationImpl.of(properties, Path.of("."));
    }

    @Nested
    @DisplayName("option ExifTool Executable")
    class ExifToolExecutable {

        @Test
        @DisplayName("does not have a default")
        void testNoDefault() {
            assertThat(emptyConfiguration().getExiftoolExecutable()).isEmpty();
        }

        @Test
        @DisplayName("is read from property exiftool.executable")
        void testFromProperty() {
            var configuration = configuration("exiftool.executable", "/usr/bin/exiftooltest");
            assertThat(configuration.getExiftoolExecutable()).hasValue(Path.of("/usr/bin/exiftooltest"));
        }

        @Test
        @DisplayName("is written to property exiftool.executable")
        void testToProperty() {
            var configuration = emptyConfiguration()
                    .setExiftoolExecutable(Path.of("/usr/bin/exiftooltest"));

            assertThat(configuration.toProperties())
                    .containsEntry("exiftool.executable", Path.of("/usr/bin/exiftooltest").toString());
        }
    }

    @Test
    @DisplayName("is created from Properties")
    void testIsCreatedFromProperties() {
        var properties = new Properties();
        properties.setProperty("history.size", "18");

        var configuration = ConfigurationImpl.of(properties, Path.of("."));

        assertThat(configuration.getHistoryMaxSize()).hasValue(18);
    }


    @Test
    @DisplayName("is created from empty Properties")
    void testIsCreatedFromEmptyProperties() {
        var configuration = ConfigurationImpl.of(new Properties(), Path.of("."));

        assertThat(configuration.getHistoryMaxSize()).isEmpty();
        assertThat(configuration.getHistory()).isNotNull().isEmpty();
    }


    @Test
    @DisplayName("exports Properties")
    void testExportProperties() {
        var configuration = ConfigurationImpl.of(new Properties(), Path.of("."))
                .setMaxHistorySize(18)
                .pushHistoryEntry(ROOT.resolve("file1.png"))
                .pushHistoryEntry(ROOT.resolve("file2.png"));

        Properties properties = configuration.toProperties();
        assertThat(properties).containsExactlyInAnyOrderEntriesOf(Map.of(
                "history.size", "18",
                "history.entry[0]", ROOT.resolve("file2.png").toString(),
                "history.entry[1]", ROOT.resolve("file1.png").toString()
        ));
    }

    @Test
    @DisplayName("returns the configuration directory")
    void testGetConfigDirectory(@TempDir Path tempDir) {
        var configuration = ConfigurationImpl.of(new Properties(), tempDir);
        assertThat(configuration.getConfigDir()).isEqualTo(tempDir);
    }

    @Nested
    class TagDefinitionList {
        @Test
        @DisplayName("has default tag definitions directory")
        void testGetDefaultTagDefinitionDirectory(@TempDir Path tempDir) {
            var configuration = ConfigurationImpl.of(new Properties(), tempDir);
            assertThat(configuration.getTagDefinitions()).containsExactly(tempDir.resolve("tags"));
            // Ensure the result remains the same when called multiple times
            assertThat(configuration.getTagDefinitions()).containsExactly(tempDir.resolve("tags"));
        }

        @Test
        @DisplayName("does not export the default tag definitions directory")
        void testDoNotExportDefaultTagDefinitionDirectory(@TempDir Path tempDir) {
            var configuration = ConfigurationImpl.of(new Properties(), tempDir);

            assertThat(configuration.getTagDefinitions()).containsExactly(tempDir.resolve("tags"));


            Properties properties = configuration.toProperties();
            assertThat(properties.toString()).doesNotContain("tags");
        }

        @Test
        @DisplayName("get tag definitions from Properties")
        void testGetTagDefinitionListFromProperties() {
            var properties = new Properties();
            properties.setProperty("tags.entry[0]", Path.of("file2.png").toString());
            properties.setProperty("tags.entry[1]", Path.of("file1.png").toString());
            properties.setProperty("another.entry[2]", Path.of("file3.png").toString());

            var configuration = ConfigurationImpl.of(properties, Path.of("."));

            assertThat(configuration.getTagDefinitions()).containsExactly(
                    Path.of("./tags"),
                    Path.of("file2.png"),
                    Path.of("file1.png"));
        }

        @Test
        @DisplayName("get tag definitions from Properties ignoring missing indexes")
        void testGetTagDefinitionListFromPropertiesIgnoringMissingIndexes() {
            var properties = new Properties();
            properties.setProperty("tags.entry[0]", Path.of("file2.png").toString());
            properties.setProperty("tags.entry[5]", Path.of("file1.png").toString());

            var configuration = ConfigurationImpl.of(properties, Path.of("."));

            assertThat(configuration.getTagDefinitions()).containsExactly(
                    Path.of("./tags"),
                    Path.of("file2.png"),
                    Path.of("file1.png"));
        }
    }

    @Nested
    class TemplateList {
        @Test
        @DisplayName("has default templates directory")
        void testGetDefaultTemplatesDirectory(@TempDir Path tempDir) {
            var configuration = ConfigurationImpl.of(new Properties(), tempDir);
            assertThat(configuration.getTemplatePaths()).containsExactly(tempDir.resolve("templates").toString());
            // Ensure the result remains the same when called multiple times
            assertThat(configuration.getTemplatePaths()).containsExactly(tempDir.resolve("templates").toString());
        }

        @Test
        @DisplayName("does not export the default templates directory")
        void testDoNotExportDefaultTemplatesDirectory(@TempDir Path tempDir) {
            var configuration = ConfigurationImpl.of(new Properties(), tempDir);

            assertThat(configuration.getTemplatePaths()).containsExactly(tempDir.resolve("templates").toString());

            Properties properties = configuration.toProperties();
            assertThat(properties.toString()).doesNotContain("templates");
        }

        @Test
        @DisplayName("get templates from Properties")
        void testTemplateListFromProperties() {
            var properties = new Properties();
            properties.setProperty("templates.entry[0]", Path.of("file2.png").toString());
            properties.setProperty("templates.entry[1]", Path.of("file1.png").toString());
            properties.setProperty("another.entry[2]", Path.of("file3.png").toString());

            var configuration = ConfigurationImpl.of(properties, Path.of("."));

            assertThat(configuration.getTemplatePaths()).containsExactly(
                    "./templates",
                    "file2.png",
                    "file1.png");
        }
//
//        @Test
//        @DisplayName("get tag definitions from Properties ignoring missing indexes")
//        void testGetTagDefinitionListFromPropertiesIgnoringMissingIndexes() {
//            var properties = new Properties();
//            properties.setProperty("tags.entry[0]", Path.of("file2.png").toString());
//            properties.setProperty("tags.entry[5]", Path.of("file1.png").toString());
//
//            var configuration = ConfigurationImpl.of(properties, Path.of("."));
//
//            assertThat(configuration.getTagDefinitions()).containsExactly(
//                    Path.of("./tags"),
//                    Path.of("file2.png"),
//                    Path.of("file1.png"));
//        }
    }

    @Nested
    class HistoryList {

        @Test
        @DisplayName("get history List settings from Properties")
        void testGetHistoryListFromProperties() {
            var properties = new Properties();
            properties.setProperty("history.entry[0]", Path.of("file2.png").toString());
            properties.setProperty("history.entry[1]", Path.of("file1.png").toString());
            properties.setProperty("another.entry[2]", Path.of("file3.png").toString());

            var configuration = ConfigurationImpl.of(properties, Path.of("."));

            assertThat(configuration.getHistory()).containsExactly(Path.of("file2.png"), Path.of("file1.png"));
        }

        @Test
        @DisplayName("get history List without duplicates from Properties")
        void testGetHistoryListWithoutDuplicatesFromProperties() {
            var properties = new Properties();
            properties.setProperty("history.entry[0]", Path.of("file2.png").toString());
            properties.setProperty("history.entry[1]", Path.of("file2.png").toString());
            properties.setProperty("history.entry[2]", Path.of("file1.png").toString());
            properties.setProperty("history.entry[3]", Path.of("file2.png").toString());

            var configuration = ConfigurationImpl.of(properties, Path.of("."));

            assertThat(configuration.getHistory()).containsExactly(Path.of("file2.png"), Path.of("file1.png"));
        }


        @Test
        @DisplayName("get List settings from Properties ignoring missing indexes")
        void testGetListFromPropertiesIgnoringMissingIndexes() {
            var properties = new Properties();
            properties.setProperty("history.entry[0]", Path.of("file2.png").toString());
            properties.setProperty("history.entry[5]", Path.of("file1.png").toString());

            var configuration = ConfigurationImpl.of(properties, Path.of("."));

            assertThat(configuration.getHistory()).containsExactly(Path.of("file2.png"), Path.of("file1.png"));
        }

        @Test
        @DisplayName("get List settings from Properties in the right order")
        void testGetListFromPropertiesInOrder() {
            var properties = new Properties();
            properties.setProperty("history.entry[50]", Path.of("file2.png").toString());
            properties.setProperty("history.entry[100]", Path.of("file1.png").toString());

            var configuration = ConfigurationImpl.of(properties, Path.of("."));

            assertThat(configuration.getHistory()).containsExactly(Path.of("file2.png"), Path.of("file1.png"));
        }

        @Test
        @DisplayName("pushes history entries as absolute paths")
        void testPushAbsoluteHistoryEntries() {
            Path projectDir = Path.of(".").toAbsolutePath();

            var configuration = ConfigurationImpl.of(new Properties(), Path.of("."))
                    .pushHistoryEntry(Path.of("file1.png"));

            assertThat(configuration.getHistory())
                    .containsExactly(projectDir.resolve("file1.png").normalize());
        }

        @Test
        @DisplayName("pushes history entries to the top of the list")
        void testPushHistoryEntriesToTop() {
            var configuration = ConfigurationImpl.of(new Properties(), Path.of("."))
                    .pushHistoryEntry(ROOT.resolve("file1.png"))
                    .pushHistoryEntry(ROOT.resolve("file2.png"));

            assertThat(configuration.getHistory()).containsExactly(
                    ROOT.resolve("file2.png"),
                    ROOT.resolve("file1.png")
            );
        }

        @Test
        @DisplayName("pushes history entries without duplicates")
        void testPushesHistoryEntriesWithoutDuplicates() {
            var configuration = ConfigurationImpl.of(new Properties(), Path.of("."))
                    .pushHistoryEntry(ROOT.resolve("file1.png"))
                    .pushHistoryEntry(ROOT.resolve("file2.png"))
                    .pushHistoryEntry(ROOT.resolve("file1.png"));

            assertThat(configuration.getHistory()).containsExactly(
                    ROOT.resolve("file1.png"),
                    ROOT.resolve("file2.png")
            );
        }

        @Test
        @DisplayName("pushes history entries with default maximum number of 5 entries")
        void testPushesHistoryEntriesWithDefaultMaximum() {
            var configuration = ConfigurationImpl.of(new Properties(), Path.of("."))
                    .pushHistoryEntry(ROOT.resolve("file1.png"))
                    .pushHistoryEntry(ROOT.resolve("file2.png"))
                    .pushHistoryEntry(ROOT.resolve("file3.png"))
                    .pushHistoryEntry(ROOT.resolve("file4.png"))
                    .pushHistoryEntry(ROOT.resolve("file5.png"))
                    .pushHistoryEntry(ROOT.resolve("file6.png"));

            assertThat(configuration.getHistory()).containsExactly(
                    ROOT.resolve("file6.png"),
                    ROOT.resolve("file5.png"),
                    ROOT.resolve("file4.png"),
                    ROOT.resolve("file3.png"),
                    ROOT.resolve("file2.png")
            );
        }

        @Test
        @DisplayName("pushes history entries with configured maximum number of entries")
        void testPushesHistoryEntriesWithConfiguredMaximum() {
            var configuration = ConfigurationImpl.of(new Properties(), Path.of("."))
                    .setMaxHistorySize(2)
                    .pushHistoryEntry(ROOT.resolve("file1.png"))
                    .pushHistoryEntry(ROOT.resolve("file2.png"))
                    .pushHistoryEntry(ROOT.resolve("file3.png"))
                    .pushHistoryEntry(ROOT.resolve("file4.png"))
                    .pushHistoryEntry(ROOT.resolve("file5.png"))
                    .pushHistoryEntry(ROOT.resolve("file6.png"));

            assertThat(configuration.getHistory()).containsExactly(
                    ROOT.resolve("file6.png"),
                    ROOT.resolve("file5.png")
            );
        }
    }

    @Nested
    class ViewFilterTest {
        @Test
        @DisplayName("returns a default ViewFilter")
        void returnsDefaultViewFilter() {
            var configuration = ConfigurationImpl.of(new Properties(), Path.of("."));
            var filter = configuration.getViewFilter();
            assertThat(filter.isReadOnlyHidden()).isFalse();
        }

        @Test
        @DisplayName("is read from filter.view properties")
        void testFromProperties() {
            var configuration = configuration("filter.view.read-only-hidden", "true");
            assertThat(configuration.getViewFilter())
                    .returns(true, ViewFilter::isReadOnlyHidden);
        }


        @Test
        @DisplayName("ignores invalid properties")
        void testInvalidProperties() {
            var configuration = configuration("filter.view.read-only-hidden", "blah");
            assertThat(configuration.getViewFilter())
                    .returns(false, ViewFilter::isReadOnlyHidden);
        }

        @Test
        @DisplayName("is written to filter.view properties")
        void testToProperty() {
            var configuration = emptyConfiguration()
                    .setViewFilter(new ViewFilter(true));

            assertThat(configuration.toProperties())
                    .containsEntry("filter.view.read-only-hidden", "true");
        }
    }

    @Nested
    class LastSelectedTemplateTest {
        @Test
        @DisplayName("returns no default path")
        void returnsNoDefault() {
            var configuration = ConfigurationImpl.of(new Properties(), Path.of("."));
            var path = configuration.getLastSelectedTemplate();
            assertThat(path).isEmpty();
        }

        @Test
        @DisplayName("is read from template.last.selected properties")
        void testFromProperties() {
            var configuration = configuration("template.last.selected", "One,Two&#44; or Deux,Trois");
            assertThat(configuration.getLastSelectedTemplate())
                    .hasValueSatisfying(array -> assertThat(array).containsExactly("One", "Two, or Deux", "Trois"));
        }


        @Test
        @DisplayName("is written to template.last.selected properties")
        void testToProperty() {
            var configuration = emptyConfiguration()
                    .setLastSelectedTemplate(new String[]{"One", "Two, or Deux", "Trois"});

            assertThat(configuration.toProperties())
                    .containsEntry("template.last.selected", "One,Two&#44; or Deux,Trois");
        }
    }

    @Nested
    class EscapeStringArray {
        @Test
        void throwsNullPointerException() {
            assertThatThrownBy(() -> escapeStringArray(null))
                    .isInstanceOf(NullPointerException.class);
        }
        @Test
        void emptyArray() {
            assertThat(escapeStringArray(new String[]{})).isEqualTo("");
        }

        @Test
        void singleElementArray() {
            assertThat(escapeStringArray(new String[]{"HELLO"})).isEqualTo("HELLO");
        }

        @Test
        void multiElementArray() {
            assertThat(escapeStringArray(new String[]{"HELLO", "THERE", "WORLD"})).isEqualTo("HELLO,THERE,WORLD");
        }

        @Test
        void escapedElements() throws Exception {
            assertThat(escapeStringArray(new String[]{"HELLO", "THERE,", "WORLD, and others"})).isEqualTo("HELLO,THERE&#44;,WORLD&#44; and others");
        }
    }

    @Nested
    class UnescapeStringArray {
        @Test
        void throwsNullPointerException() {
            assertThatThrownBy(() -> ConfigurationImpl.unescapeStringArray(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void emptyArray() {
            assertThat(unescapeStringArray("")).isEmpty();
        }

        @Test
        void singleElementArray() {
            assertThat(unescapeStringArray("HELLO")).containsExactly("HELLO");
        }

        @Test
        void multiElementArray() {
            assertThat(unescapeStringArray("HELLO,THERE,WORLD")).containsExactly("HELLO", "THERE", "WORLD");
        }

        @Test
        void escapedElements() {
            assertThat(unescapeStringArray("HELLO,THERE&#44;,WORLD&#44; and others")).containsExactly("HELLO", "THERE,", "WORLD, and others");
        }
    }
}
