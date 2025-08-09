package com.quaxantis.etui.swing.template;

import com.quaxantis.etui.Template;
import com.quaxantis.etui.TemplateValues;
import com.quaxantis.support.swing.Dimensions;
import com.quaxantis.support.swing.form.FormFieldInput;
import com.quaxantis.support.swing.form.InfoMarker;
import com.quaxantis.support.swing.form.WrappedFormFieldInput;
import com.quaxantis.support.swing.layout.SpringLayoutOrganizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.quaxantis.support.swing.form.InfoMarker.Type.INFO;
import static com.quaxantis.support.swing.form.InfoMarker.Type.WARNING;

class TemplateFormFieldInput extends WrappedFormFieldInput<JComponent, com.quaxantis.etui.TemplateValues.Entry, String> {
    private final Logger log = LoggerFactory.getLogger(TemplateFormFieldInput.class);

    private final InfoMarker marker;
    private final Template template;
    private final Template.Variable variable;
    private final Supplier<Optional<String>> expression;
    private boolean isDirty;

    public TemplateFormFieldInput(Template template, Template.Variable variable, Supplier<Optional<String>> expression, FormFieldInput<?, String> inputDelegate) {
        this(template,
             variable,
             expression,
             inputDelegate,
             new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)),
             new InfoMarker('i').setType(INFO)
        );
    }

    private TemplateFormFieldInput(Template template, Template.Variable variable, Supplier<Optional<String>> expression, FormFieldInput<?, String> inputDelegate, JPanel panel, InfoMarker marker) {
        super(panel, entry -> entry.value().orElse(""), inputDelegate, TemplateFormFieldInput::entryWithValue);
        this.template = template;
        this.variable = variable;
        this.marker = marker;
        this.expression = expression;

        hideMarker();

        JComponent component = inputDelegate.component();
        SpringLayoutOrganizer.organize(panel)
                .add(marker)
                .atRightSide()
                .withSameMiddleAs(component)
                .and()
                .add(component)
                .atTop()
                .atLeftSide()
                .leftOf(marker, 5);
        panel.setPreferredSize(new Dimension(Dimensions.sumWidth(marker.getPreferredSize(), component.getPreferredSize()),
                                             Dimensions.maxHeight(marker.getPreferredSize(), component.getPreferredSize())));
        SpringLayoutOrganizer.setNotStretchingVertically(panel);
    }

    private static TemplateValues.Entry entryWithValue(TemplateValues.Entry entry1, String newValue) {
        return Objects.requireNonNullElse(entry1, TemplateValues.Entry.EMPTY).withValue(newValue);
    }

    @Override
    public void setValue(TemplateValues.Entry entry) {
        super.setValue(entry);

        long sourceValues = entry.sources()
                .stream()
                .map(com.quaxantis.etui.TemplateValues.Source::value)
                .distinct()
                .count();

        String entryValue = entry.value().orElse(null);

        if (sourceValues > 1) {
            showMarkerForMultipleSourceValues(variable.name(), entry);
        } else {
            if (expression.get().orElse(null) instanceof String expressionValue
                && (!expressionValue.equals(entryValue))) {
                showMarkerForInconsistentExpressionValue(expressionValue);
            } else if (entry.info().orElse(null) instanceof String entryInfo) {
                showMarker(entryInfo, INFO);
            } else if (StringUtils.isEmpty(entryValue) && template.tags(variable).isEmpty()) {
                showMarker("Variable has no source tags. Analyze to find possible values", WARNING);
            } else {
                hideMarker();
            }
        }
    }

    private void showMarkerForMultipleSourceValues(String variableName, TemplateValues.Entry entry) {
        String message = entry.sources().stream()
                .map(v -> "%s : %s".formatted(v.name(), v.value()))
                .collect(Collectors.joining(
                        "</li><li>",
                        "Source is inconsistent with template. Multiple values for variable '%s':<ul><li>".formatted(variableName),
                        "</li></ul>"));
        showMarker(message, WARNING);
        isDirty = true;
    }

    private void showMarkerForInconsistentExpressionValue(String expressionValue) {
        String message = "Source is inconsistent with expression for variable '%s'. Expected: '%s'"
                .formatted(variable.name(), expressionValue);
        showMarker(message, WARNING);
        isDirty = true;
    }

    private void showMarker(String message, InfoMarker.Type type) {
        switch (type) {
            case INFO -> marker.setInfoCharacter('i');
            case WARNING -> marker.setInfoCharacter('!');
        }
        marker.setType(type);
        marker.setPreferredSize(null);
        marker.setToolTipAsHtml(message);
        marker.repaint();
        marker.setVisible(true);
        log.warn(message);
    }

    private void hideMarker() {
        marker.setMaximumSize(new Dimension(0, 0));
        marker.setPreferredSize(new Dimension(0, 0));
        marker.setToolTipAsHtml(null);
        marker.setVisible(false);
    }

    @Override
    public boolean isDirty() {
        return isDirty || super.isDirty();
    }
}
