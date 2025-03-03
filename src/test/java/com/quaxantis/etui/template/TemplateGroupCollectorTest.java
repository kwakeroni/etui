package com.quaxantis.etui.template;

import com.quaxantis.etui.Template;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@MockitoSettings
class TemplateGroupCollectorTest {


    @Test
    @DisplayName("Collects a single TemplateGroup with individual templates")
    void testOneGroupIndividualTemplates(@Mock Template template1, @Mock Template template2) {
        var groups = Stream.of(template1, template2)
                .map(template -> Map.entry("test-group", template))
                .collect(TemplateGroupCollector.collector());

        assertThat(groups).hasSize(1)
                .first().satisfies(group -> {
            assertThat(group.name()).isEqualTo("test-group");
            assertThat(group.subGroups()).isEmpty();
            assertThat(group.templates()).containsExactly(template1, template2);
        });
    }

    @Test
    @DisplayName("Collects multiple TemplateGroups with individual templates")
    void testMultipleGroupsIndividualTemplates(@Mock Template template11, @Mock Template template12, @Mock Template template21, @Mock Template template22) {
        var groups = Stream.of(
                Map.entry("group1", template11),
                Map.entry("group2", template21),
                Map.entry("group2", template22),
                Map.entry("group1", template12)
        ).collect(TemplateGroupCollector.collector());

        assertThat(groups).hasSize(2)
                .satisfiesExactly(group -> {
                    assertThat(group.name()).isEqualTo("group1");
                    assertThat(group.subGroups()).isEmpty();
                    assertThat(group.templates()).containsExactly(template11, template12);
                }, group -> {
                    assertThat(group.name()).isEqualTo("group2");
                    assertThat(group.subGroups()).isEmpty();
                    assertThat(group.templates()).containsExactly(template21, template22);
                });
    }

    @Test
    @DisplayName("Collects TemplateGroups with nested sub groups based on naming convention using '/'")
    void testNestedGroups(@Mock Template rootTemplate, @Mock Template templateA1, @Mock Template templateA2, @Mock Template templateB) {
        var groups = Stream.of(
                Map.entry("Test / Group A", templateA1),
                Map.entry("Test / Group B", templateB),
                Map.entry("Test", rootTemplate),
                Map.entry("Test / Group A", templateA2)
        ).collect(TemplateGroupCollector.collector());

        assertThat(groups).hasSize(1)
                .first().satisfies(root -> {
                    assertThat(root.name()).isEqualTo("Test");
                    assertThat(root.templates()).containsExactly(rootTemplate);
                    assertThat(root.subGroups()).hasSize(2)
                            .satisfiesExactly(group -> {
                                assertThat(group.name()).isEqualTo("Group A");
                                assertThat(group.subGroups()).isEmpty();
                                assertThat(group.templates()).containsExactly(templateA1, templateA2);
                            }, group -> {
                                assertThat(group.name()).isEqualTo("Group B");
                                assertThat(group.subGroups()).isEmpty();
                                assertThat(group.templates()).containsExactly(templateB);
                            });
                });
    }

}
