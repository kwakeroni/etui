package com.quaxantis.etui.application;

public interface StateChangeListener<S> {

    void onStateChange(S oldState, S newState);

}
