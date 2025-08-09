package com.quaxantis.etui;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface TemplateValues {
    Optional<Entry> get(Template.Variable var);

    default Optional<String> getValue(Template.Variable var) {
        return get(var).flatMap(Entry::value);
    }

    void set(Template.Variable var, String value);

    void set(Template.Variable var, Entry entry);

    interface Entry {

        Optional<String> value();

        Optional<String> info();

        Collection<Source> sources();

        default Map<String, String> sourceValues() {
            return sources().stream().collect(Collectors.toUnmodifiableMap(Source::name, Source::value));
        }

        Entry withValue(String newValue);

        static Entry EMPTY = Entry.of(null);

        static Entry of(String value) {
            return Entry.of(value, List.of());
        }

        static Entry of(String value, Collection<Source> originalSources) {
            return of(value, originalSources, null);
        }

        static Entry of(String value, Collection<Source> originalSources, String info) {
            List<Source> sources = List.copyOf(originalSources);

            return new Entry() {

                @Override
                public Optional<String> value() {
                    return Optional.ofNullable(value);
                }

                @Override
                public Optional<String> info() {
                    return Optional.ofNullable(info);
                }

                @Override
                public Entry withValue(String newValue) {
                    return Entry.of(newValue, sources);
                }

                @Override
                public Collection<Source> sources() {
                    return sources;
                }

                @Override
                public String toString() {
                    return getClass().getSimpleName() + '[' +
                           "value=" + value + ", " +
                           "sources=" + sources + ']';
                }
            };
        }
    }

    interface Source {
        String name();

        String value();

        static Source of(String name, String value) {
            return new Source() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public String value() {
                    return value;
                }
            };
        }
    }
}
