package com.quaxantis.etui.template;

import com.quaxantis.etui.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractTemplateTest {

    protected static final String VARIABLE_NAME = "TestVar";
    protected static final String VARIABLE_LABEL = "Test Variable";
    protected static final String VARIABLE2_NAME = "TestVar2";
    protected static final String VARIABLE2_LABEL = "Test Variable #2";
    protected static final String GROUP_NAME = "TestGroup";
    protected static final String TAG_NAME = "TestTag";

    protected static final Tag tag = Tag.of(GROUP_NAME, TAG_NAME);
    protected static final Tag tag2 = Tag.of("TestGroup-2", "TestTag-2");
    protected static final Tag tag3 = Tag.of("TestGroup-3", "TestTag-3");

    @Test
    @DisplayName("Has a name")
    void testHasName() {
        var template = builder().setName("myName").build();

        assertThat(template.name()).isEqualTo("myName");
    }

    @Test
    @DisplayName("Has variables")
    void testHasVariables() {
        var template = builder()
                .setName("myName")
                .addVariable(new VariableBuilder().setName(VARIABLE_NAME).setLabel(VARIABLE_LABEL))
                .addVariable(new VariableBuilder().setName(VARIABLE2_NAME).setLabel(VARIABLE2_LABEL))
                .build();

        assertThat(template.variables())
                .extracting(Template.Variable::name, Template.Variable::label)
                .containsExactly(tuple(VARIABLE_NAME, VARIABLE_LABEL),
                                 tuple(VARIABLE2_NAME, VARIABLE2_LABEL));
    }

    @Test
    @DisplayName("Supports template without mappings (not sure why)")
    void supportTemplateWithoutMappings() {
        var template = builder()
                .addVariable(new VariableBuilder().setName(VARIABLE_NAME))
                .build();
        var variable = template.variableByName(VARIABLE_NAME).orElseThrow();

        assertThat(template.tags(variable)).isEmpty();
    }

    @Test
    @DisplayName("Provides a tag as a target of the variable")
    void testTagsSingle() {
        var template = builder()
                .addVariable(new VariableBuilder().setName(VARIABLE_NAME).addTargetTag(tag))
                .build();
        var variable = template.variableByName(VARIABLE_NAME).orElseThrow();

        assertThat(template.tags(variable)).usingElementComparator(Tag.COMPARATOR).containsExactly(tag);
    }

    @Test
    @DisplayName("Provides multiple tags as targets of the variable")
    void testTagsMultiple() {
        var template = builder()
                .addVariable(new VariableBuilder().setName(VARIABLE_NAME).addTargetTag(tag).addTargetTag(tag3))
                .addVariable(new VariableBuilder().setName(VARIABLE2_NAME).addTargetTag(tag2))
                .build();
        var variable = template.variableByName(VARIABLE_NAME).orElseThrow();

        assertThat(template.tags(variable))
                .usingElementComparator(Tag.COMPARATOR)
                .containsExactlyInAnyOrder(tag, tag3);
    }

    @Test
    @DisplayName("Provides a default expression evaluator")
    void provideExpressionEvaluator() {
        var template = builder().build();
        assertThat(template.expressionEvaluator()).isNotNull();
    }

    @Test
    @DisplayName("Has variables with expressions")
    void testVariableWithExpression() {
        var template = builder()
                .addVariable(new VariableBuilder().setName(VARIABLE_NAME).setExpression("an-expression"))
                .build();

        var variable = template.variableByName(VARIABLE_NAME).orElseThrow();
        assertThat(variable.hasExpression()).describedAs("hasExpression").isTrue();
        assertThat(variable.expression()).isEqualTo("an-expression");
    }

    protected abstract TemplateBuilder builder();

    protected static abstract class TemplateBuilder {
        protected String name;
        protected List<VariableBuilder> variables = new ArrayList<>();


        protected TemplateBuilder setName(String name) {
            this.name = name;
            return this;
        }

        protected TemplateBuilder addVariable(VariableBuilder variableBuilder) {
            this.variables.add(variableBuilder);
            return this;
        }

        protected abstract Template build();
    }

    protected static class VariableBuilder {
        String name;
        String label;
        String expression;
        final List<Tag> targetTags = new ArrayList<>();

        public VariableBuilder() {

        }

        public String label() {
            return this.label;
        }

        public VariableBuilder setLabel(String label) {
            this.label = label;
            return this;
        }

        public String name() {
            return this.name;
        }

        public VariableBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public String expression() {
            return this.expression;
        }

        public VariableBuilder setExpression(String expression) {
            this.expression = expression;
            return this;
        }

        public VariableBuilder addTargetTag(Tag tag) {
            this.targetTags.add(tag);
            return this;
        }

        public List<Tag> targetTags() {
            return Collections.unmodifiableList(this.targetTags);
        }
    }

}
