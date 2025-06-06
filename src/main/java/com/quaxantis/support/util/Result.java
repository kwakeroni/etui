package com.quaxantis.support.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Result<T, E extends Exception> {

    boolean isSuccess();

    boolean isFailure();

    Optional<T> ifSuccess();

    T orElseThrow() throws E;

    <F extends Exception> T orElseThrow(Function<E, F> exceptionMapper) throws F;

    static <E extends Exception> Result<Void, E> ofVoid() {
        return of(null);
    }

    static <T, E extends Exception> Result<T, E> of(T t) {
        return new Success<>(t);
    }

    static <T, E extends Exception> Result<T, E> ofFailure(E exception) {
        return new Failure<>(exception);
    }

    static <T, E extends RuntimeException> Result<T, E> ofTry(Supplier<? extends T> supplier) {
        try {
            return of(supplier.get());
        } catch (RuntimeException rte) {
            return ofFailure((E) rte);
        }
    }

    record Success<T, E extends Exception>(T value)
            implements Result<T, E> {
        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Optional<T> ifSuccess() {
            return Optional.ofNullable(value);
        }

        @Override
        public T orElseThrow() throws E {
            return value;
        }

        @Override
        public <F extends Exception> T orElseThrow(Function<E, F> exceptionMapper) throws F {
            return value;
        }
    }

    record Failure<T, E extends Exception>(E exception)
            implements Result<T, E> {
        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<T> ifSuccess() {
            return Optional.empty();
        }

        @Override
        public T orElseThrow() throws E {
            throw exception;
        }

        @Override
        public <F extends Exception> T orElseThrow(Function<E, F> exceptionMapper) throws F {
            throw exceptionMapper.apply(exception);
        }
    }

}
