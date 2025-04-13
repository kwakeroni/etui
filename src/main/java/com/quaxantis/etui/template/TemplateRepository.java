package com.quaxantis.etui.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.quaxantis.etui.TagDescriptor;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.Tag;
import com.quaxantis.etui.application.config.Configuration;
import com.quaxantis.etui.application.config.ConfigurationImpl;
import com.quaxantis.etui.swing.template.TemplateGroup;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.etui.template.xml.ConfiguredTemplate;
import com.quaxantis.etui.template.xml.XMLTemplateCollection;
import com.quaxantis.support.util.StreamEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class TemplateRepository {

    public static void main() {


        var configuration = ConfigurationImpl.of(new Properties(), Path.of("C:\\Users\\Eigenaar\\.etui"));
        var repo = new TemplateRepository(configuration, new TagRepository(configuration));

        repo.templates()
                        .forEach(System.out::println);
        System.out.println("---");
        repo.templateGroups()
                .forEach(System.out::println);
    }

    private final Logger log = LoggerFactory.getLogger(TemplateRepository.class);
    private final ObjectMapper objectMapper = XmlMapper.builder().build();
    private final Configuration configuration;
    private final TagRepository tagRepository;

    public TemplateRepository(Configuration configuration, TagRepository tagRepository) {
        this.configuration = configuration;
        this.tagRepository =  tagRepository;
    }

    public List<Template> templates() {
        try(var templates = Stream.concat(standardCollections(), configuredCollections())) {
            return templates.<Template> map(StreamEntry::value).toList();
        }
    }

    private static void forAllTemplatesRecursive(TemplateGroup group, Consumer<Template> downStream) {
        group.subGroups().forEach(subGroup -> forAllTemplatesRecursive(subGroup, downStream));
        group.templates().forEach(downStream);
    }

    // TODO: filter by file format
    public List<TemplateGroup> templateGroups() {
        try(var templates = Stream.concat(standardCollections(), configuredCollections())) {
            return templates.collect(TemplateGroupCollector.collector());
        }
    }

    //    private Stream<TemplateGroup> standardCollectionsGrouped() {
//        List<Qualified<String, Template>> templates = standardCollections()
//                .toList();
//        return Stream.of(new TemplateGroup("Tags", List.of(), templates));
//    }

    private Stream<StreamEntry<String, Template>> standardCollections() {
        return tagRepository.getGroupedFamilies()
                .entrySet()
                .stream()
                .map(StreamEntry::of)
                .flatMap(StreamEntry.flatMapping(List::stream))
                .map(StreamEntry.mapping(family -> ofTags(family.label(), family.tags())))
                .map(StreamEntry.mappingKey(key -> "Tags / " + key));


//        return tagRepository.getFamilies()
//                .stream()
//                .map(family -> ofTags(family.label(), family.tags()))
//                .map(template -> new StreamEntry<>("Tags", template));
    }

//    private Stream<TemplateGroup> configuredCollectionsGrouped() {
//        Map<String, List<Template>> templates = configuredCollections()
//                .collect(groupingBy(Qualified::key, mapping(Qualified::value, toList())));
//
//        return templates.entrySet().stream()
//                .map(entry -> new TemplateGroup(entry.getKey(), List.of(), entry.getValue()));
//    }

    // TODO: support glob
    private Stream<StreamEntry<String, ConfiguredTemplate>> configuredCollections() {
        return configuration.getTemplateDefinitions()
                .stream()
                .filter(Files::exists)
                .flatMap(this::walk)
                .filter(not(Files::isDirectory))
                .map(StreamEntry::of)
                .map(StreamEntry.mapping(this::readCollection))
                .filter(StreamEntry::isNonNullValue)
                .map(StreamEntry.mappingKey((path, collection) -> (collection.group() != null)?
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

    private static Template ofTags(String name, Collection<? extends Tag> tags) {
        return tags.stream()
                .filter(tag -> ! TagDescriptor.of(tag).isReadOnly())
                .collect(Collector.of(() -> TemplateSupport.builder(name),
                                      (builder, tag) -> builder.addVariable(new VariableSupport(tag), tag),
                                      (b1, b2) -> {
                                          throw new UnsupportedOperationException();
                                      },
                                      TemplateSupport.Builder::build));
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

}
