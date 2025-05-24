package com.quaxantis.support.util;

import java.util.function.Function;

public sealed interface Result<T, E extends Exception> {

    boolean isSuccess();

    boolean isFailure();

    T orElseThrow() throws E;

    <F extends Exception> T orElseThrow(Function<E, F> exceptionMapper) throws F;

    static <T, E extends Exception> Result<T, E> of(T t) {
        return new Success<>(t);
    }

    static <T, E extends Exception> Result<T, E> ofFailure(E exception) {
        return new Failure<>(exception);
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
        public T orElseThrow() throws E {
            throw exception;
        }

        @Override
        public <F extends Exception> T orElseThrow(Function<E, F> exceptionMapper) throws F {
            throw exceptionMapper.apply(exception);
        }
    }

}
