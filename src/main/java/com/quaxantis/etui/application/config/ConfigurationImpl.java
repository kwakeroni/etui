package com.quaxantis.etui.application.config;

import org.apache.commons.text.translate.NumericEntityEscaper;
import org.apache.commons.text.translate.NumericEntityUnescaper;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.quaxantis.etui.application.config.ConfigurationListener.Item.VIEW_FILTER;
import static java.util.function.Predicate.not;

public class ConfigurationImpl implements Configuration {

    public static ConfigurationImpl of(Properties properties, Path configDir) {
        EnumMap<Setting, Object> map = new EnumMap<>(Setting.class);
        for (Setting setting : Setting.values()) {
            setting.from(properties).ifPresent(value -> map.put(setting, value));
        }
        return new ConfigurationImpl(map, configDir);
    }


    private final EnumMap<Setting, Object> settings;
    private final Path configDir;
    private final List<ConfigurationListener> listenerList = new ArrayList<>();
    private ConfigurationImpl(EnumMap<Setting, Object> map, Path configDir) {
        this.settings = map;
        this.configDir = configDir;
    }

    Path getConfigDir() {
        return this.configDir;
    }

    public void addConfigurationListener(ConfigurationListener listener) {
        this.listenerList.add(listener);
        System.out.println("Configuration listeners: " + this.listenerList.size());
    }

    public Optional<Path> getExiftoolExecutable() {
        return get(Setting.EXIFTOOL_EXECUTABLE, Path.class);
    }

    ConfigurationImpl setExiftoolExecutable(Path path) {
        return set(Setting.EXIFTOOL_EXECUTABLE, path);
    }

    @Override
    public ViewFilter getViewFilter() {
        return new ViewFilter(
                get(Setting.VIEW_FILTER_READONLY_HIDDEN, Boolean.class).orElse(false)
        );
    }

    ConfigurationImpl setViewFilter(ViewFilter viewFilter) {
        return set(Setting.VIEW_FILTER_READONLY_HIDDEN, viewFilter.isReadOnlyHidden());
    }

    @Override
    public Optional<String[]> getLastSelectedTemplate() {
        return get(Setting.TEMPLATE_LAST_SELECTED, String[].class);
    }

    ConfigurationImpl setLastSelectedTemplate(String[] selectionPath) {
        return set(Setting.TEMPLATE_LAST_SELECTED, selectionPath);
    }

    OptionalInt getHistoryMaxSize() {
        return get(Setting.HISTORY_SIZE, Integer.class)
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    }

    ConfigurationImpl setMaxHistorySize(int i) {
        return set(Setting.HISTORY_SIZE, i);
    }

    ConfigurationImpl pushHistoryEntry(Path path) {

        List<Path> history = getPathListSetting(Setting.HISTORY);
        if (!path.isAbsolute()) {
            history.remove(path);
            path = path.toAbsolutePath();
        }
        history.remove(path);
        history.addFirst(path);
        truncate(history, getHistoryMaxSize().orElse(5));
        return this;
    }

    public List<Path> getHistory() {
        return Collections.unmodifiableList(getPathListSetting(Setting.HISTORY));
    }

    @Override
    public List<Path> getTagDefinitions() {
        List<Path> list = new LinkedList<>();
        list.add(this.configDir.resolve("tags"));
        list.addAll(getPathListSetting(Setting.TAG_DEFINITIONS));
        return Collections.unmodifiableList(list);
    }

    @Override
    public List<Path> getTemplateDefinitions() {
        List<Path> list = new LinkedList<>();
        list.add(this.configDir.resolve("templates"));
        list.addAll(getPathListSetting(Setting.TEMPLATE_DEFINITIONS));
        return Collections.unmodifiableList(list);
    }

    @SuppressWarnings("unchecked")
    private List<Path> getPathListSetting(Setting setting) {
        return (List<Path>) settings.computeIfAbsent(setting, ignored -> new LinkedList<>());
    }

    private ConfigurationImpl set(Setting setting, Object object) {
        settings.put(setting, setting.cast(object));
        setting.item().ifPresent(item -> {
            Set<ConfigurationListener.Item> items = EnumSet.of(item);
            this.listenerList.forEach(listener -> listener.onConfigurationChanged(this, items));
        });
        return this;
    }

    private <T> Optional<T> get(Setting setting, Class<T> type) {
        return Optional.ofNullable(type.cast(get(setting)));
    }

    private Object get(Setting setting) {
        return settings.get(setting);
    }

    Properties toProperties() {
        Properties properties = new Properties();
        for (Setting setting : Setting.values()) {
            setting.addTo(properties, get(setting));
        }
        return properties;
    }

    private static void truncate(List<?> list, int maxSize) {
        if (list.size() > maxSize) {
            list.subList(maxSize, list.size()).clear();
        }
    }

    private interface TypedSetting<T> {
        Optional<T> from(Properties properties);

        public void addTo(Properties properties, Object value);

        T cast(Object o);
    }

    private static class SimpleSetting<T> implements TypedSetting<T> {
        private final String property;
        private final Class<T> type;
        private final Function<String, ? extends T> parser;
        private final Function<? super T, String> stringifier;

        public SimpleSetting(String property, Class<T> type, Function<String, ? extends T> parser, Function<? super T, String> stringifier) {
            this.property = property;
            this.type = type;
            this.parser = parser;
            this.stringifier = stringifier;
        }

        @Override
        public Optional<T> from(Properties properties) {
            return Optional.ofNullable(properties.getProperty(property))
                    .map(parser);
        }

        public void addTo(Properties properties, Object value) {
            if (value != null) {
                properties.setProperty(property, stringifier.apply(cast(value)));
            }
        }

        @Override
        public T cast(Object o) {
            return type.cast(o);
        }
    }

    private static class ListSetting<T> implements TypedSetting<List<T>> {
        private final String property;
        private final Class<T> type;
        private final Function<String, T> parser;
        private final Function<? super T, String> stringifier;


        public ListSetting(String property, Class<T> type, Function<String, T> parser, Function<? super T, String> stringifier) {
            this.property = property;
            this.type = type;
            this.parser = parser;
            this.stringifier = stringifier;
        }

        private static final Pattern INDEX_PATTERN = Pattern.compile("([^\\s\\[]+)\\[(\\d+)]");
        private static final Comparator<Matcher> INDEX_MATCH_COMPARATOR = Comparator.comparing(matcher -> Integer.parseInt(matcher.group(2)));

        @Override
        public Optional<List<T>> from(Properties properties) {

            return Optional.of(properties.keySet()
                                       .stream()
                                       .map(Object::toString)
                                       .map(INDEX_PATTERN::matcher)
                                       .filter(Matcher::matches)
                                       .filter(matcher -> property.equals(matcher.group(1)))
                                       .sorted(INDEX_MATCH_COMPARATOR)
                                       .map(Matcher::group)
                                       .map(properties::getProperty)
                                       .map(parser)
                                       .distinct()
                                       .collect(Collectors.<T, List<T>>toCollection(LinkedList::new)))
                    .filter(not(List::isEmpty));
        }

        @Override
        public void addTo(Properties properties, Object object) {
            if (object != null) {
                List<T> list = cast(object);
                for (int i = 0; i < list.size(); i++) {
                    properties.setProperty(property + "[" + i + "]", stringifier.apply(list.get(i)));
                }
            }
        }

        @SuppressWarnings("unchecked") // All elements are checked
        @Override
        public List<T> cast(Object o) {
            List<?> list = (List<?>) o;
            for (Object e : list) {
                type.cast(e);
            }
            return (List<T>) list;
        }
    }

    public static final NumericEntityEscaper COMMA_ESCAPER = NumericEntityEscaper.between(',', ',');
    public static final NumericEntityUnescaper COMMA_UNESCAPER = new NumericEntityUnescaper();

    @Nonnull
    static String escapeStringArray(@Nonnull String[] array) {
        return Arrays.stream(array)
                .map(COMMA_ESCAPER::translate)
                .collect(Collectors.joining(","));
    }

    @Nonnull
    static String[] unescapeStringArray(@Nonnull String escaped) {
        if (escaped.isEmpty()) {
            return new String[0];
        }
        return Arrays.stream(escaped.split(","))
                .map(COMMA_UNESCAPER::translate)
                .toArray(String[]::new);
    }


    private enum Setting {
        EXIFTOOL_EXECUTABLE(new SimpleSetting<>("exiftool.executable", Path.class, Path::of, Path::toString)),

        VIEW_FILTER_READONLY_HIDDEN(VIEW_FILTER, new SimpleSetting<>("filter.view.read-only-hidden", Boolean.class, Boolean::parseBoolean, Object::toString)),
        TEMPLATE_LAST_SELECTED(new SimpleSetting<>("template.last.selected", String[].class, ConfigurationImpl::unescapeStringArray, ConfigurationImpl::escapeStringArray)),

        HISTORY_SIZE(new SimpleSetting<>("history.size", Integer.class, Integer::parseInt, Object::toString)),
        HISTORY(new ListSetting<>("history.entry", Path.class, Path::of, Path::toString)),

        TAG_DEFINITIONS(new ListSetting<>("tags.entry", Path.class, Path::of, Path::toString)),
        TEMPLATE_DEFINITIONS(new ListSetting<>("templates.entry", Path.class, Path::of, Path::toString)),
        ;

        private final ConfigurationListener.Item item;
        private final TypedSetting<?> typedSetting;

        Setting(TypedSetting<?> typedSetting) {
            this(null, typedSetting);
        }
        Setting(ConfigurationListener.Item item, TypedSetting<?> typedSetting) {
            this.item = item;
            this.typedSetting = typedSetting;
        }

        public Optional<ConfigurationListener.Item> item() {
            return Optional.ofNullable(this.item);
        }

        public void addTo(Properties properties, Object value) {
            typedSetting.addTo(properties, value);
        }

        public Object cast(Object o) {
            return typedSetting.cast(o);
        }

        public Optional<?> from(Properties properties) {
            return typedSetting.from(properties);
        }
    }
}
