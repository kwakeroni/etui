package com.quaxantis.etui.application.config;

public record ViewFilter(boolean isReadOnlyHidden) {

    public ViewFilter toggleReadOnlyHidden() {
        return withReadOnlyHidden(! isReadOnlyHidden);
    }

    public ViewFilter withReadOnlyHidden(boolean readOnlyHidden) {
        return new ViewFilter(readOnlyHidden);
    }

}
