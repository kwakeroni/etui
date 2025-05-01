package com.quaxantis.etui.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagDescriptor;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.application.config.Configuration;
import com.quaxantis.etui.swing.template.TemplateGroup;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.etui.template.xml.ConfiguredTemplate;
import com.quaxantis.etui.template.xml.XMLTemplateCollection;
import com.quaxantis.support.util.GlobMatcher;
import com.quaxantis.support.util.StreamEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class TemplateRepository {

    private final Logger log = LoggerFactory.getLogger(TemplateRepository.class);
    private final ObjectMapper objectMapper = XmlMapper.builder().build();
    private final Configuration configuration;
    private final TagRepository.Cache tagRepository;

    public TemplateRepository(Configuration configuration, TagRepository tagRepository) {
        this.configuration = configuration;
        this.tagRepository = tagRepository.cached();
    }

    public List<Template> templates() {
        try (var templates = Stream.concat(standardCollections(), configuredCollections())) {
            return templates.<Template>map(StreamEntry::value).toList();
        } finally {
            this.tagRepository.clear();
        }
    }

    private static void forAllTemplatesRecursive(TemplateGroup group, Consumer<Template> downStream) {
        group.subGroups().forEach(subGroup -> forAllTemplatesRecursive(subGroup, downStream));
        group.templates().forEach(downStream);
    }

    // TODO: filter by file format
    public List<TemplateGroup> templateGroups() {
        try (var templates = Stream.concat(standardCollections(), configuredCollections())) {
            return templates.collect(TemplateGroupCollector.collector());
        } finally {
            this.tagRepository.clear();
        }
    }

    private Stream<StreamEntry<String, Template>> standardCollections() {
        return tagRepository.getGroupedFamilies()
                .entrySet()
                .stream()
                .map(StreamEntry::of)
                .flatMap(StreamEntry.flatMapping(List::stream))
                .map(StreamEntry.mapping(family -> ofTags(family.label(), family.tags())))
                .flatMap(StreamEntry.flatMapping(Optional::stream))
                .map(StreamEntry.mappingKey(key -> "Tags / " + key));
    }

    private Stream<StreamEntry<String, ConfiguredTemplate>> configuredCollections() {
//        var paths = configuration.getTemplatePaths();
//        for (String path : paths) {
//            GlobMatcher globMatcher = GlobMatcher.of(path);
//            System.out.printf("Matcher: %s = %s%n", path, globMatcher);
//            try (Stream<Path> files = walk(globMatcher.path())) {
//                files.forEach(p -> System.out.printf("%s : %s%n", p, globMatcher.matches(p)));
//            }
//        }

        return configuration.getTemplatePaths()
                .stream()
                .map(pathPattern -> GlobMatcher.of(pathPattern, "**.xml"))
                .filter(gm -> Files.exists(gm.path()))
                .flatMap(GlobMatcher::walk)
                .filter(not(Files::isDirectory))
                .map(StreamEntry::of)
                .map(StreamEntry.mapping(this::readCollection))
                .filter(StreamEntry::isNonNullValue)
                .map(StreamEntry.mappingKey((path, collection) -> (collection.group() != null) ?
                        collection.group() : path.getFileName().toString()))
                .map(StreamEntry.mapping(XMLTemplateCollection::templates))
                .filter(StreamEntry::isNonNullValue)
                .flatMap(entry -> entry.flatMap(List::stream))
                .map(StreamEntry.mapping(xmlTemplate -> ConfiguredTemplate.of(xmlTemplate, this.tagRepository)));
    }

    Stream<Path> walk(Path path) {
        try {
            return Files.walk(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<Template> ofTags(String name, Collection<? extends Tag> tags) {
        return tags.stream()
                .filter(tag -> !TagDescriptor.of(tag).isReadOnly())
                .collect(Collector.of(() -> TemplateSupport.builder(name),
                                      (builder, tag) -> builder.addVariable(new VariableSupport(tag), tag),
                                      (_, _) -> {
                                          throw new UnsupportedOperationException();
                                      },
                                      builder -> Optional.of(builder.build())
                                              .filter(not(template -> template.variables().isEmpty()))));
    }

    @Nullable
    private XMLTemplateCollection readCollection(Path path) {
        try {
            log.info("Importing template definitions from {}", path);
            return this.objectMapper.readValue(path.toFile(), XMLTemplateCollection.class);
        } catch (Exception exc) {
            log.error("Error while reading template collection {}", path, exc);
            return null;
        }
    }


    public static void main(String[] args) {

        var configuration = new ConfigOperations().getConfiguration();
        System.out.println(configuration.getTemplatePaths());
        var repo = new TemplateRepository(configuration, new TagRepository(configuration));

        repo.templates()
                .forEach(System.out::println);
        System.out.println("---");
        repo.templateGroups()
                .forEach(System.out::println);
    }

}
