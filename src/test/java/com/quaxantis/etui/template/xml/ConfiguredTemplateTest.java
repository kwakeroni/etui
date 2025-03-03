package com.quaxantis.etui.template.xml;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagDescriptor;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.TemplateValues;
import com.quaxantis.etui.tag.TagDescriptorBuilder;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.etui.template.AbstractTemplateTest;
import com.quaxantis.etui.template.ExpressionEvaluator;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.net.URL;
import java.util.Optional;

import static java.util.Objects.requireNonNullElse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("A ConfiguredTemplate")
class ConfiguredTemplateTest extends AbstractTemplateTest {

    private static final Logger log = LoggerFactory.getLogger(ConfiguredTemplateTest.class);
    private static final ObjectMapper objectMapper = XmlMapper.builder().build();
    private static final Validator validator = createValidator(ConfiguredTemplateTest.class.getResource("/com/quaxantis/etui/template/template-collection.xsd"));

    TagRepository tagRepository = Mockito.mock(TagRepository.class);

    @Test
    @DisplayName("Variables reflect the information of a reference tag")
    void testVariableReflectsTag() {
        Tag tag = TagDescriptorBuilder.of(Tag.of("GRP", "myTag"))
                .withLabel("myTagLabel")
                .withDescription("myDescription")
                .withFormatDescription("myFormat")
                .addExample("myExample", "myPattern");

        Mockito.when(tagRepository.findTagByQualifiedName("GRP:myTag"))
                .thenReturn(Optional.of(tag));

        Template template = getTemplate("""
                                        <template>
                                            <label>test</label>
                                            <variable name='myVar' tag='GRP:myTag' />
                                        </template>
                                        """);

        assertThat(template.variableByName("myVar")).hasValueSatisfying(variable -> {
            assertThat(variable)
                    .returns("myVar", Template.Variable::name)
                    .returns("myTagLabel", Template.Variable::label)
                    .isInstanceOf(TagDescriptor.class)
                    .extracting(TagDescriptor.class::cast)
                    .returns("myDescription", TagDescriptor::description)
                    .returns("myFormat", TagDescriptor::formatDescription)
                    .extracting(TagDescriptor::examples)
                    .satisfies(examples -> {
                        assertThat(examples).extracting(TagDescriptor.Example::text, TagDescriptor.Example::pattern)
                                .containsExactly(Tuple.tuple("myExample", "myPattern"));
                    });
        });
    }

    @Test
    @DisplayName("Provides a tag attribute as a tag target of the variable")
    void testVariableMapsToReferenceTag() {
        Template template = getTemplate("""
                                        <template>
                                            <label>test</label>
                                            <variable name='%s' tag='%s:%s' />
                                        </template>
                                        """.formatted(VARIABLE_NAME, GROUP_NAME, TAG_NAME));

        var variable = template.variableByName(VARIABLE_NAME).orElseThrow();

        assertThat(template.tags(variable)).usingElementComparator(Tag.COMPARATOR).containsExactly(tag);
    }

    @Test
    @DisplayName("Variables combine tags specified in attribute and elements")
    void testVariableCombinesAttributeTagAndElementTags() {
        Template template = getTemplate("""
                                        <template>
                                            <label>test</label>
                                            <variable name='var' tag='attr:t1'>
                                                <tag>el:t1</tag>
                                                <tag>el:t2</tag>
                                            </variable>
                                        </template>
                                        """);

        var var = template.variableByName("var").orElseThrow();
        assertThat(template.tags(var)).extracting(Tag::qualifiedName)
                .containsExactlyInAnyOrder("attr:t1", "el:t1", "el:t2");
    }

    @Test
    @DisplayName("Fails on unknown variable property")
    void testFailOnUnknownVariableProperty() {
        assertThatThrownBy(() -> getTemplate("""
                                             <template>
                                                 <label>test</label>
                                                 <variable name='var' tag='attr:t1'>
                                                     <unknown>X</unknown>
                                                 </variable>
                                             </template>
                                             """, false))
                .cause()
                .isInstanceOf(JsonMappingException.class)
                .hasMessageStartingWith("Unrecognized field \"unknown\"");
    }

    @Test
    @DisplayName("Fails on tag property of unknown type")
    void testFailOnTagPropertyWithUnknownType() {
        assertThatThrownBy(() -> getTemplate("""
                                             <template>
                                                 <label>test</label>
                                                 <variable name='var' tag='attr:t1'>
                                                     <tag>
                                                         <unknown>content</unknown>
                                                     </tag>
                                                 </variable>
                                             </template>
                                             """, false))
                .cause()
                .isInstanceOf(JsonMappingException.class)
                .message()
                .startsWith("Field \"tag\" expected to have a String value but was ");
    }


    @Test
    @DisplayName("Provides a expression evaluator using the evaluator attribute")
    void testUseConfiguredExpressionEvaluator() {
        Template template = getTemplate("""
                                        <template evaluator='com.quaxantis.etui.template.xml.ConfiguredTemplateTest$TestEvaluator'>
                                            <label>test</label>
                                            <variable name="var" />
                                        </template>
                                        """.formatted(GROUP_NAME, TAG_NAME));

        assertThat(template.expressionEvaluator()).isInstanceOf(TestEvaluator.class);

    }

    public static class TestEvaluator implements ExpressionEvaluator {

        public TestEvaluator(ExpressionEvaluator.Context ignored) {

        }

        @Override
        public String evaluate(String expression, TemplateValues values) {
            throw new UnsupportedOperationException();
        }
    }


    private static Validator createValidator(URL url) {
        try {
            return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(url).newValidator();
        } catch (SAXException exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    private Template getTemplate(String xml) {
        return getTemplate(xml, true);
    }

    private Template getTemplate(String xml, boolean validate) {
        try {
            log.debug("Template XML:{}{}", System.lineSeparator(), xml);
            if (validate) {
                validator.validate(new StreamSource(new StringReader("<template-collection xmlns=\"https://quaxantis.com/etui/templates\">" + xml + "</template-collection>")));
            }
            XMLTemplate xmlTemplate = objectMapper.readValue(xml, XMLTemplate.class);
            return ConfiguredTemplate.of(xmlTemplate, tagRepository);
        } catch (Exception e) {
            throw new RuntimeException("Error processing XML\n" + xml, e);
        }
    }

    @Override
    protected TemplateBuilder builder() {
        return new Builder();
    }

    private class Builder extends AbstractTemplateTest.TemplateBuilder {
        @Override
        public Template build() {
            StringBuilder xml = new StringBuilder();
            xml.append("<template>")
                    .append("<label>")
                    .append(requireNonNullElse(this.name, "dummy"))
                    .append("</label>");

            if (this.variables.isEmpty()) {
                appendVariable(xml, new VariableBuilder().setName("dummy"));
            } else {
                for (VariableBuilder variable : this.variables) {
                    appendVariable(xml, variable);
                }
            }
            xml.append("</template>");

            return getTemplate(xml.toString());
        }

        private void appendVariable(StringBuilder appendable, VariableBuilder variable) {
            appendable.append("<variable");
            appendable.append(" name=\"").append(requireNonNullElse(variable.name(), "dummy")).append("\"");
            if (variable.expression() != null) {
                appendable.append(" expression=\"").append(variable.expression()).append("\"");
            }
            appendable.append(">");

            if (variable.label() != null) {
                appendable.append("<label>").append(variable.label()).append("</label>");
            }
            for (Tag tag : variable.targetTags()) {
                appendable.append("<tag>").append(tag.qualifiedName()).append("</tag>");
            }
            appendable.append("</variable>");

        }
    }
}
