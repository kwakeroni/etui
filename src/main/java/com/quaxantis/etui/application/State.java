package com.quaxantis.etui.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class State<S> {

    private final Collection<StateChangeListener<? super S>> listeners;
    private S state;

    public State(S initialState) {
        this.state = initialState;
        this.listeners = new ArrayList<>();
    }

    public void transition(UnaryOperator<S> transition) {
        S oldState = this.state;
        S newState = transition.apply(this.state);

        if (oldState != newState) {
            System.out.printf("File state: %s <= %s%n", newState, oldState);
            this.state = newState;
            fireStateChange(oldState, newState);
        }
    }

    public <R> R get(Function<S, R> getter) {
        return getter.apply(this.state);
    }

    public void registerListener(StateChangeListener<? super S> listener) {
        this.listeners.add(listener);
    }

    public void unregisterListener(StateChangeListener<? super S> listener) {
        this.listeners.remove(listener);
    }

    private void fireStateChange(S oldState, S newState) {
        this.listeners.forEach(listener -> listener.onStateChange(oldState, newState));
    }
}
