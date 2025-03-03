package com.quaxantis.etui.template;

import com.quaxantis.etui.*;
import com.quaxantis.etui.Template.Variable;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static com.quaxantis.etui.template.test.MockTemplate.mockTemplate;
import static com.quaxantis.etui.template.test.MockTemplateValues.mockValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("TemplateMapper")
@ExtendWith(MockitoExtension.class)
class TemplateMapperTest {

    private static final String TAG1_QUALIFIED_NAME = "test-group:test-tag";
    private static final String TAG1B_QUALIFIED_NAME = "test-group:test-tag-1b";
    private static final String TAG2_QUALIFIED_NAME = "test-group:test-tag-2";
    private static final Tag TAG1 = Tag.ofQualifiedName(TAG1_QUALIFIED_NAME);
    private static final Tag TAG2 = Tag.ofQualifiedName(TAG2_QUALIFIED_NAME);
    private static final Tag TAG1B = Tag.ofQualifiedName(TAG1B_QUALIFIED_NAME);
    private static final Variable.Builder VARIABLE = Variable.withName("test-variable");
    private static final Variable.Builder VARIABLE2 = Variable.withName("test-variable-2");

    @Test
    @DisplayName("Provides a new TemplateValues instance")
    void newValues() {
        TemplateMapper mapper = new TemplateMapper(mockTemplate().mock());
        assertThat(mapper.newValues()).isNotNull();
    }

    @Nested
    @DisplayName("maps a variable to a single tag")
    class SingleTag {
        private final Template template = mockTemplate()
                .withVariables(VARIABLE, VARIABLE2)
                .withMapping(VARIABLE, TAG1)
                .withMapping(VARIABLE2, TAG2)
                .mock();
        private final TemplateMapper mapper = new TemplateMapper(template);

        @Test
        @DisplayName("maps a variable to a tag")
        void toTags() {
            var values = mockValues().withEntry(VARIABLE, "test-value").mock();

            assertThat(mapper.toTags(values))
                    .containsExactly(TagValue.of(TAG1, "test-value"));

            verify(values).get(VARIABLE);
            verify(template).tags(VARIABLE);
        }

        @Test
        @DisplayName("does not map an empty value to a tag")
        void toTagsWithEmptyValue() {
            var values = mockValues().withEntry(VARIABLE, "").mock();

            assertThat(mapper.toTags(values)).isEmpty();

            verify(values).get(VARIABLE);
            verify(template, never()).tags(VARIABLE);
        }

        @Test
        @DisplayName("maps a tag to a variable")
        void fromTags() {
            var tagSet = TagSet.of(TagValue.of(TAG1, "test-value"));

            var values = mapper.fromTags(tagSet);

            assertThat(values.get(VARIABLE)).hasValueSatisfying(
                    entryOf("test-value", tuple(TAG1_QUALIFIED_NAME, "test-value"))
            );

        }
    }

    @Nested
    @DisplayName("maps a variable to multiple tags")
    class MultipleTags {
        private final Template template = mockTemplate()
                .withVariables(VARIABLE, VARIABLE2)
                .withMapping(VARIABLE, TAG1, TAG1B)
                .withMapping(VARIABLE2, TAG2)
                .mock();
        private final TemplateMapper mapper = new TemplateMapper(template);

        @Test
        @DisplayName("maps a variable to multiple tags")
        void toTags() {
            var values = mockValues()
                    .withEntry(VARIABLE, "test-value-1")
                    .withEntry(VARIABLE2, "test-value-2")
                    .mock();

            assertThat(mapper.toTags(values))
                    .containsExactlyInAnyOrder(
                            TagValue.of(TAG1, "test-value-1"),
                            TagValue.of(TAG2, "test-value-2"),
                            TagValue.of(TAG1B, "test-value-1"));

            verify(values).get(VARIABLE);
            verify(template).tags(VARIABLE);
            verify(template).tags(VARIABLE2);
        }


        @Test
        @DisplayName("does not map empty values to multiple tags")
        void toTagsWithEmptyValue() {
            var values = mockValues()
                    .withEntry(VARIABLE, "")
                    .withEntry(VARIABLE2, "test-value-2")
                    .mock();

            assertThat(mapper.toTags(values))
                    .containsExactly(TagValue.of(TAG2, "test-value-2"));

            verify(values).get(VARIABLE);
            verify(template, never()).tags(VARIABLE);
            verify(template).tags(VARIABLE2);
        }

        @Test
        @DisplayName("maps multiple tags with the same value to a variable")
        void fromTagsWithSameValue() {
            var tagSet = TagSet.of(
                    TagValue.of(TAG1, "test-value-1"),
                    TagValue.of(TAG2, "test-value-2"),
                    TagValue.of(TAG1B, "test-value-1"));

            var values = mapper.fromTags(tagSet);

            assertThat(values.get(VARIABLE)).hasValueSatisfying(
                    entryOf("test-value-1",
                            tuple(TAG1_QUALIFIED_NAME, "test-value-1"),
                            tuple(TAG1B_QUALIFIED_NAME, "test-value-1")));

            assertThat(values.get(VARIABLE2)).hasValueSatisfying(
                    entryOf("test-value-2", tuple(TAG2_QUALIFIED_NAME, "test-value-2")));
        }

        @Test
        @DisplayName("does not map multiple tags with different-values to a variable")
        void fromTagsWithDifferentValues() {
            var tagSet = TagSet.of(
                    TagValue.of(TAG1, "test-value-1"),
                    TagValue.of(TAG2, "test-value-2"),
                    TagValue.of(TAG1B, "test-value-3"));

            var values = mapper.fromTags(tagSet);

            assertThat(values.get(VARIABLE)).hasValueSatisfying(
                    emptyEntryOf(tuple(TAG1_QUALIFIED_NAME, "test-value-1"),
                                 tuple(TAG1B_QUALIFIED_NAME, "test-value-3")));

            assertThat(values.get(VARIABLE2)).hasValueSatisfying(
                    entryOf("test-value-2", tuple(TAG2_QUALIFIED_NAME, "test-value-2")));
        }

        @Test
        @DisplayName("maps a tag with a value to a variable if other mapped tags are null")
        void fromTagsWithOneNonNullValue() {
            var tagSet = TagSet.of(
                    TagValue.of(TAG1, null),
                    TagValue.of(TAG2, "test-value-2"),
                    TagValue.of(TAG1B, "test-value-3"));

            var values = mapper.fromTags(tagSet);

            assertThat(values.get(VARIABLE)).hasValueSatisfying(
                    entryOf("test-value-3",
                            tuple(TAG1_QUALIFIED_NAME, null),
                            tuple(TAG1B_QUALIFIED_NAME, "test-value-3")));

            assertThat(values.get(VARIABLE2)).hasValueSatisfying(
                    entryOf("test-value-2", tuple(TAG2_QUALIFIED_NAME, "test-value-2")));
        }

        @Test
        @DisplayName("maps a tag with a value to a variable if other mapped tags are empty")
        void fromTagsWithOneNonEmptyValue() {
            var tagSet = TagSet.of(
                    TagValue.of(TAG1, ""),
                    TagValue.of(TAG2, "test-value-2"),
                    TagValue.of(TAG1B, "test-value-3"));

            var values = mapper.fromTags(tagSet);

            assertThat(values.get(VARIABLE)).hasValueSatisfying(
                    entryOf("test-value-3",
                            tuple(TAG1_QUALIFIED_NAME, ""),
                            tuple(TAG1B_QUALIFIED_NAME, "test-value-3")));

            assertThat(values.get(VARIABLE2)).hasValueSatisfying(
                    entryOf("test-value-2", tuple(TAG2_QUALIFIED_NAME, "test-value-2")));
        }

    }

    private static Consumer<TemplateValues.Entry> entryOf(String expectedValue, Tuple... expectedSources) {
        return entry -> {
            assertThat(entry.value()).hasValue(expectedValue);
            assertThat(entry.sources())
                    .extracting(TemplateValues.Source::name, TemplateValues.Source::value)
                    .containsExactly(expectedSources);
        };
    }

    private static Consumer<TemplateValues.Entry> emptyEntryOf(Tuple... expectedSources) {
        return entry -> {
            assertThat(entry.value()).isEmpty();
            assertThat(entry.sources())
                    .extracting(TemplateValues.Source::name, TemplateValues.Source::value)
                    .containsExactly(expectedSources);
        };
    }


}
