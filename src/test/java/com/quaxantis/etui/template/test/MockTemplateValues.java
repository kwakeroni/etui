package com.quaxantis.etui.template.test;

import com.quaxantis.etui.Template;
import com.quaxantis.etui.TemplateValues;
import org.mockito.MockSettings;
import org.mockito.Mockito;

import java.util.Optional;

import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;

public interface MockTemplateValues {

    static MockTemplateValues mockValues() {
        return of(Mockito.mock(TemplateValues.class, CALLS_REAL_METHODS));
    }

    static MockTemplateValues mockValues(MockSettings settings) {
        return of(Mockito.mock(TemplateValues.class, settings));
    }

    static MockTemplateValues of(TemplateValues mock) {
        return () -> mock;
    }

    default MockTemplateValues withEntry(Template.Variable variable, String value) {
        var entry = TemplateValues.Entry.of(value);
        doReturn(Optional.of(entry)).when(mock()).get(variable);
        return this;
    }

    TemplateValues mock();

}
