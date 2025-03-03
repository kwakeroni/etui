package com.quaxantis.etui.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagFamily;
import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.application.config.Configuration;
import com.quaxantis.etui.tag.xml.XMLTagCollection;
import com.quaxantis.support.util.StreamEntry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;

public class TagRepository {

    private static final List<String> STANDARD_COLLECTIONS = List.of(
            "dublin-core.xml",
            "creative-commons.xml",
            "formats.xml",
            "system.xml"
    );

    private final Logger log = LoggerFactory.getLogger(TagRepository.class);

    private final Configuration configuration;
    public TagRepository(Configuration configuration) {
        this.configuration = configuration;
    }

    private final ObjectMapper objectMapper = XmlMapper.builder().build();

    public Tag enrichTag(@Nonnull Tag tag) {
        return findTag(tag).orElse(tag);
    }

    public Optional<Tag> findTagByQualifiedName(String qualifiedName) {
        return findTag(Tag.ofQualifiedName(qualifiedName));
    }

    public Optional<Tag> findTag(Tag referenceTag) {
        return getFamilies()
                .stream()
                .flatMap(family -> family.tags().stream())
                .filter(referenceTag::isSameTagAs)
                .findAny()
                .map(Function.identity());
    }

    public List<? extends TagFamily> getFamilies() {
        try (var collections = Stream.concat(configuredCollections(), standardCollections())) {
            return collections
                    .map(StreamEntry::value)
                    .map(XMLTagCollection::families)
                    .flatMap(List::stream)
                    .toList();
        }
    }

    public SequencedMap<String, List<TagFamily>> getGroupedFamilies() {
        try (var collections = Stream.concat(configuredCollections(), standardCollections())) {
            return collections
                    .map(StreamEntry.mapping(XMLTagCollection::families))
                    .flatMap(StreamEntry.flatMapping(List::stream))
                    .collect(groupingBy(StreamEntry::getKey, LinkedHashMap::new, mapping(StreamEntry::getValue, toList())));
        }


    }

    private Stream<StreamEntry<String, XMLTagCollection>> standardCollections() {
        return STANDARD_COLLECTIONS.stream()
                .map(TagRepository.class::getResource)
                .map(StreamEntry::of)
                .map(entry -> entry.map(this::readCollection))
                .map(StreamEntry.mappingKey(
                        (url, coll) -> (coll.collection() != null)? coll.collection() : getFilename(url)));


    }

    private String getFilename(URL url) {
        return StringUtils.substringAfterLast(url.getPath(), "/");
    }

    Stream<Path> walk(Path path) {
        try {
            return Files.walk(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Stream<StreamEntry<String, XMLTagCollection>> configuredCollections() {
        return configuration.getTagDefinitions().stream()
                .filter(Files::exists)
                .flatMap(this::walk)
                .filter(not(Files::isDirectory))
                .map(StreamEntry::of)
                .map(entry -> entry.map(this::readCollection))
                .map(StreamEntry.mappingKey(
                        (path, coll) -> (coll.collection() != null)? coll.collection() : path.getFileName().toString()));
    }

    private XMLTagCollection readCollection(URL url) {
        try {
            return this.objectMapper.readValue(url, XMLTagCollection.class);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private XMLTagCollection readCollection(Path path) {
        try {
            log.info("Importing tag definitions from {}", path);
            return this.objectMapper.readValue(path.toFile(), XMLTagCollection.class);
        } catch (IOException ioe) {
            throw new UncheckedIOException("Exception reading " + path, ioe);
        }
    }

    public static void main() {
        Configuration configuration = new ConfigOperations().getConfiguration();
        var repo = new TagRepository(configuration);

        repo.getGroupedFamilies()
                .forEach((coll, families) -> {
                    System.out.println("-- " + coll);
                    families.forEach(fam -> {
                        System.out.println("---- " + fam.label());
                        fam.tags().forEach(tag -> System.out.println("------ " + tag));
                    });
                });
    }
}
