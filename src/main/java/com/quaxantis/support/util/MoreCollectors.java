package com.quaxantis.support.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public final class MoreCollectors {

    public static final BinaryOperator<Object> UNIQUE_KEYS_MERGER = (v1, v2) -> {
        throw duplicateKeyException(v1, v2);
    };

    private MoreCollectors() {
        throw new UnsupportedOperationException();
    }

    public static <K> BinaryOperator<K> uniqueKeysMerger() {
        return (BinaryOperator<K>) UNIQUE_KEYS_MERGER;
    }

    private static IllegalStateException duplicateKeyException(
            Object u, Object v) {
        return new IllegalStateException(String.format(
                "Duplicate key (attempted merging values %s and %s)",
                u, v));
    }


    private static <T, R> Collector<T, ?, R> streaming(Function<? super Stream<T>, ? extends R> function) {
        return Collectors.collectingAndThen(Collectors.toList(),
                                            (List<T> l) -> function.apply(l.stream()));
    }

    public static Collector<String, ?, String> singleDistinctValueOrElseNull() {
        return streaming(stream1 -> stream1
                .filter(Objects::nonNull)
                .filter(not(String::isEmpty))
                .reduce((v1, v2) -> (Objects.equals(v1, v2)) ? v1 : "")
                .filter(not(String::isEmpty))
                .orElse(null));
    }

    private static void t() {
        {
            Function<? super Stream<? super BigDecimal>, ? extends String> function = (Object o) -> o.toString();
            Collector<BigDecimal, ?, String> _ = streaming(function);
            Collector<BigDecimal, ?, String> _ = streaming(Object::toString);
        }{
            Function<? super Stream<? super BigDecimal>, ? extends Long> function = (Stream<?> s) -> s.count();
            Collector<BigDecimal, ?, Long> _ = streaming(function);
            Collector<BigDecimal, ?, Long> _ = streaming(Stream::count);
        }{
            Function<? super Stream<? super BigDecimal>, ? extends Long> function = (Stream<?> s) -> s.count();
            Collector<Object, ?, Long> _ = streaming(function);
            Collector<Object, ?, Long> _ = streaming(Stream::count);
        }{
            Function<? super Stream<BigDecimal>, ? extends Long> function = (Stream<BigDecimal> s) -> s.filter(bd -> bd.precision() > 1).count();
            Collector<BigDecimal, ?, Long> _ = streaming(function);
            Collector<BigDecimal, ?, Long> _ = streaming((Stream<BigDecimal> s) -> s.filter(bd -> bd.precision() > 1).count());
        }



    }
}
