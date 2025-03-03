package com.quaxantis.etui.tag;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.application.config.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.*;

@DisplayName("TagRepository")
@ExtendWith(MockitoExtension.class)
class TagRepositoryTest {

    @Mock
    Configuration configuration;
    @InjectMocks
    TagRepository repository;

    @Test
    @DisplayName("Returns built-in tags")
    void returnsBuiltInTagFamilies() {
        assertThat(repository.getFamilies())
                .extracting("name", "group", "label")
                .contains(tuple("dc", "XMP", "Dublin Core"));
    }

    @Test
    @DisplayName("Finds tags by qualified name")
    void findsTagsByQualifiedName() {
        assertThat(repository.findTagByQualifiedName("XMP:attributionName")).hasValueSatisfying(tag -> {
            assertThat(tag)
                    .returns("XMP", Tag::groupName)
                    .returns("attributionName", Tag::tagName);
        });
    }

    @Test
    @DisplayName("Finds tags by qualified name in a case-insensitive way")
    void findsTagsByQualifiedNameCaseInsensitive() {
        assertThat(repository.findTagByQualifiedName("Xmp:AttribuTionNaMe")).hasValueSatisfying(tag -> {
            assertThat(tag)
                    .returns("XMP", Tag::groupName)
                    .returns("attributionName", Tag::tagName);
        });
    }


    @Test
    @DisplayName("Returns tags from a configured file")
    void returnsTagFamiliesFromConfiguredFile(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("test-family.txt");
        Files.writeString(inputFile, /*language=xml*/
                          """
                          <tag-collection>
                              <family name="test-family" group="test-group">
                                  <label>Test Family</label>
                              </family>
                          </tag-collection>
                          """);

        when(configuration.getTagDefinitions()).thenReturn(List.of(inputFile));

        assertThat(repository.getFamilies())
                .extracting("name", "group", "label")
                .contains(tuple("test-family", "test-group", "Test Family"));
    }

    @Test
    @DisplayName("Returns tags from a configured directory")
    void returnsTagFamiliesFromConfiguredDirectory(@TempDir Path tempDir) throws Exception {
        Path inputFile1 = tempDir.resolve("test-family1.txt");
        Path inputFile2 = tempDir.resolve("test-family2.txt");
        Files.writeString(inputFile1, /*language=xml*/
                          """
                          <tag-collection>
                              <family name="test-family1" group="test-group">
                                  <label>Test Family 1</label>
                              </family>
                          </tag-collection>
                          """);
        Files.writeString(inputFile2, /*language=xml*/
                          """
                          <tag-collection>
                              <family name="test-family2" group="test-group">
                                  <label>Test Family 2</label>
                              </family>
                          </tag-collection>
                          """);

        when(configuration.getTagDefinitions()).thenReturn(List.of(tempDir));

        assertThat(repository.getFamilies())
                .extracting("name", "group", "label")
                .contains(tuple("test-family1", "test-group", "Test Family 1"))
                .contains(tuple("test-family2", "test-group", "Test Family 2"));
    }

    @Test
    @DisplayName("Ignores non-existing files")
    void ignoresNonExistingConfiguredFile(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("test-family.txt");

        when(configuration.getTagDefinitions()).thenReturn(List.of(inputFile));
        assertThatCode(() -> repository.getFamilies())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Closes directory streams")
    void closesDirectoryStreams(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("test-family.txt");
        Files.writeString(inputFile, /*language=xml*/
                          """
                          <tag-collection>
                              <family name="test-family" group="test-group">
                                  <label>Test Family</label>
                              </family>
                          </tag-collection>
                          """);

        when(configuration.getTagDefinitions()).thenReturn(List.of(inputFile));

        Map<Path, Runnable> streams = new HashMap<>();

        repository = new TagRepository(configuration) {
            @Override
            Stream<Path> walk(Path path) {
                Runnable mock = mock(Runnable.class);
                Mockito.doAnswer(_ -> {
                    new Exception().printStackTrace();
                    return null; }).when(mock).run();
                streams.merge(path, mock, (_, _) -> { throw new IllegalStateException("Path %s already encountered".formatted(path)); });
                return super.walk(path).onClose(mock::run);
            }
        };

        assertThat(repository.getFamilies())
                .extracting("name", "group", "label")
                .contains(tuple("test-family", "test-group", "Test Family"));


        streams.forEach((path, stream) -> Mockito.verify(stream, description("Stream of path " + path)).run());
    }
}
