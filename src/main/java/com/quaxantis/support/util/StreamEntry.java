package com.quaxantis.support.util;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record StreamEntry<K, V>(K key, V value) implements Map.Entry<K, V> {

    @Override
    public K getKey() {
        return key();
    }

    @Override
    public V getValue() {
        return value();
    }

    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    public static <Q> StreamEntry<Q, Q> of(Q qualifier) {
        return new StreamEntry<>(qualifier, qualifier);
    }

    public static <Q, T> StreamEntry<Q, T> of(Map.Entry<Q, T> entry) {
        return new StreamEntry<>(entry.getKey(), entry.getValue());
    }

    public <S> StreamEntry<K, S> map(Function<V, S> mapper) {
        return new StreamEntry<>(key, mapper.apply(value));
    }

    public static <Q, T, S> Function<StreamEntry<Q, T>, StreamEntry<Q, S>> mapping(Function<T, S> mapper) {
        return entry -> entry.map(mapper);
    }

    public <R> StreamEntry<R, V> mapKey(Function<K, R> mapper) {
        return new StreamEntry<>(mapper.apply(key), value);
    }

    public <R> StreamEntry<R, V> mapKey(BiFunction<K, V, R> mapper) {
        return new StreamEntry<>(mapper.apply(key, value), value);
    }

    public static <Q, T, R> Function<StreamEntry<Q, T>, StreamEntry<R, T>> mappingKey(Function<Q, R> mapper) {
        return entry -> entry.mapKey(mapper);
    }

    public static <Q, T, R> Function<StreamEntry<Q, T>, StreamEntry<R, T>> mappingKey(BiFunction<Q, T, R> mapper) {
        return entry -> entry.mapKey(mapper);
    }

    public Optional<StreamEntry<K, V>> filter(Predicate<V> predicate) {
        return predicate.test(value) ? Optional.of(this) : Optional.empty();
    }

    public boolean isNonNullValue() {
        return value != null;
    }

    public <S> Stream<StreamEntry<K, S>> flatMap(Function<V, Stream<S>> flatMapper) {
        return flatMapper.apply(value)
                .map(s -> new StreamEntry<>(key, s));
    }

    public static <Q, T, S> Function<StreamEntry<Q, T>, Stream<StreamEntry<Q, S>>> flatMapping(Function<T, Stream<S>> flatMapper) {
        return entry -> entry.flatMap(flatMapper);
    }
}
