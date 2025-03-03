package com.quaxantis.etui.template;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagValue;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.Template.Variable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("A TemplateSupport")
class TemplateSupportTest extends AbstractTemplateTest {

    @Override
    protected TemplateBuilder builder() {
        return new Builder();
    }

    private static class Builder extends AbstractTemplateTest.TemplateBuilder {
        @Override
        public Template build() {
            TemplateSupport.Builder builder = TemplateSupport.builder(this.name);
            for (var variableBuilder : this.variables) {
                Variable variable = variable(variableBuilder);
                builder.addVariable(variable);
                for (var tag : variableBuilder.targetTags) {
                    if (variableBuilder.expression != null) {
                        builder.addExpressionMapping(variableBuilder.expression, tag);
                    } else {
                        builder.addMapping(variable, tag);
                    }
                }
            }
            return builder
                    .build();
        }
    }

    private static Variable variable(VariableBuilder builder) {
        return Variable.withName(builder.name())
                .setLabel(builder.label())
                .setExpression(builder.expression());
    }
}
