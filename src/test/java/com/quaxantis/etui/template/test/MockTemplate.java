package com.quaxantis.etui.template.test;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.Template;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.Mockito.doReturn;

public interface MockTemplate {

    static MockTemplate mockTemplate() {
        return MockTemplate.of(Mockito.mock(Template.class));
    }

    static MockTemplate of(Template mock) {
        return () -> mock;
    }

    default MockTemplate withVariables(Template.Variable... variables) {
        doReturn(List.of(variables)).when(mock()).variables();
        return this;
    }

    default MockTemplate withMapping(Template.Variable variable, Tag... tags) {
        doReturn(List.of(tags)).when(mock()).tags(variable);
        return this;
    }

    Template mock();
}
