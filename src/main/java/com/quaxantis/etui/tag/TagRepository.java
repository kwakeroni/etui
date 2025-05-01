package com.quaxantis.etui.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagDescriptor;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;

public class TagRepository {

    private static final List<String> STANDARD_COLLECTIONS = List.of(
            "dublin-core.xml",
            "creative-commons.xml",
            "prism.xml",
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
        return findTag(tag)
                .or(() -> enrichByFamily(tag))
                .orElse(tag);
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

    private Optional<Tag> enrichByFamily(@Nonnull Tag tag) {
        System.out.println("NOT FOUND TAG " + tag);
        List<Boolean> readOnlyValues = getFamilies()
                .stream()
                .filter(family -> family.defaultGroup().filter(group -> group.equalsIgnoreCase(tag.groupName())).isPresent())
                .map(TagFamily::defaultReadOnly)
                .distinct()
                .toList();
        if (readOnlyValues.size() == 1 && readOnlyValues.getFirst()) {
            return Optional.of(TagDescriptor.of(tag).asReadOnly().asTag());
        } else {
            return Optional.empty();
        }
    }

    public List<? extends TagFamily> getFamilies() {
        try (var collections = readCollections()) {
            return collections
                    .map(StreamEntry::value)
                    .map(XMLTagCollection::families)
                    .flatMap(List::stream)
                    .toList();
        }
    }

    // TODO: filter by file format
    public SequencedMap<String, List<TagFamily>> getGroupedFamilies() {
        try (var collections = readCollections()) {
            return collections
                    .map(StreamEntry.mapping(XMLTagCollection::families))
                    .flatMap(StreamEntry.flatMapping(List::stream))
                    .collect(groupingBy(StreamEntry::getKey, LinkedHashMap::new, mapping(StreamEntry::getValue, toList())));
        }


    }

    protected Stream<StreamEntry<String, XMLTagCollection>> readCollections() {
        return Stream.concat(configuredCollections(), standardCollections());
    }

    private Stream<StreamEntry<String, XMLTagCollection>> standardCollections() {
        return STANDARD_COLLECTIONS.stream()
                .map(StreamEntry::of)
                .map(StreamEntry.mapping(TagRepository.class::getResource))
                .peek(entry -> {
                    if (entry.isNullValue()) {
                        log.warn("Could not locate tag definitions: {}", entry.getKey());
                    }
                })
                .filter(StreamEntry::isNonNullValue)
                .map(StreamEntry.mappingKey((_, url) -> url))
                .map(entry -> entry.map(this::readCollection))
                .map(StreamEntry.mappingKey(
                        (url, coll) -> (coll.collection() != null) ? coll.collection() : getFilename(url)));


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
                        (path, coll) -> (coll.collection() != null) ? coll.collection() : path.getFileName().toString()));
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

    public Cache cached() {
        return new Cache(this.configuration);
    }

    public class Cache extends TagRepository {
        AtomicReference<List<StreamEntry<String, XMLTagCollection>>> collections = new AtomicReference<>();

        private Cache(Configuration configuration) {
            super(configuration);
        }

        @Override
        protected Stream<StreamEntry<String, XMLTagCollection>> readCollections() {
            return this.collections.updateAndGet(cached -> (cached != null) ? cached : super.readCollections().toList()).stream();
        }

        public Cache clear() {
            this.collections.set(null);
            return this;
        }
    }

    public static void main(String[] args) {
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
