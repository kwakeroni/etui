package com.quaxantis.etui.swing.template;

import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.TemplateValues;
import com.quaxantis.support.swing.form.FormListener;
import com.quaxantis.support.swing.layout.SpringLayoutOrganizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JPanel;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

class TemplatePanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(TemplatePanel.class);

    private final Map<Template, TrackedValues> trackedValuesMap = new HashMap<>();
    private final Component actionButton;
    private Template currentTemplate;
    private TemplateForm form;
    private TagSet originalTags;

    TemplatePanel(Component actionButton) {
        this.actionButton = Objects.requireNonNull(actionButton, "actionButton");
    }

    void addFormListener(FormListener<TemplateForm> listener) {
        this.listenerList.add(FormListener.class, listener);
        if (this.form != null) {
            this.form.addFormListener(listener);
        }
    }

    void setTags(TagSet originalTags) {
        this.originalTags = originalTags;
        // Recreate template to avoid onChange getting called
        setTemplate(this.currentTemplate);
    }

    void setTemplate(Template template) {
        this.currentTemplate = template;
        if (template != null) {
            this.form = new TemplateForm(template);
            initData();
            this.form.addFormListener(FormListener.onChanged(form -> setChanges(template, form.getData())));
            for (@SuppressWarnings("unchecked") FormListener<TemplateForm> listener : this.listenerList.getListeners(FormListener.class)) {
                this.form.addFormListener(listener);
            }
        } else {
            this.actionButton.setEnabled(false);
        }
        resetLayout();
    }

    @Nullable
    Template getCurrentTemplate() {
        return this.currentTemplate;
    }

    private String format(Template template, TemplateValues values) {
        return "%s = %s".formatted(template, template.variables().stream()
                .filter(var -> values.getValue(var).filter(Predicate.not(String::isBlank)).isPresent())
                .map(Template.Variable::name)
                .collect(Collectors.joining(",")));
    }

    void setChanges(Template template, TemplateValues changes) {

        updateTrackedChanges(template,
                             tv -> tv.withCurrentValues(changes),
                             () -> new TrackedValues(template, changes));

        updateActionButton();
    }

    void resetChanges() {
        System.out.println("Reset changes " + this.currentTemplate);
        updateTrackedChanges(this.currentTemplate,
                             TrackedValues::reset,
                             () -> null);
    }

    void clearChangeCache() {
        System.out.println("Clear changes " + this.currentTemplate);
        this.trackedValuesMap.remove(this.currentTemplate);
    }

    private void updateTrackedChanges(Template template, UnaryOperator<TrackedValues> operator, Supplier<TrackedValues> supplier) {
        this.trackedValuesMap.compute(template, (_, trackedValues) -> {
            if (trackedValues == null) {
                log.warn("No tracked values found for template {}", template);
                return supplier.get();
            } else {
                return operator.apply(trackedValues);
            }
        });
        System.out.println("Updated tracked values " + format(template, getTrackedValues(template).map(TrackedValues::current).orElseThrow()));

    }

    private Optional<TrackedValues> getTrackedValues(Template template) {
        return Optional.ofNullable(this.trackedValuesMap.get(template));
    }

    TemplateForm form() {
        return this.form;
    }

    void clearForm() {
        if (this.form != null) {
            resetChanges();
            this.form.clear();
        }
    }

    void resetForm() {
        if (this.form != null) {
            resetChanges();
            initData();
        }
    }

    void initData() {
        if (this.trackedValuesMap.get(currentTemplate) instanceof TrackedValues trackedValues
            && trackedValues.hasChanges()) {
            var values = trackedValues.current();
            System.out.println("Setting cached values " + format(currentTemplate, values));
            this.form.setData(values);
        } else if (originalTags != null) {
            System.out.println("Setting tags " + currentTemplate);
            this.form.setTags(originalTags);
            TemplateValues values = this.form.getData();
            System.out.println("Setting values from tags " + format(currentTemplate, values));
            this.trackedValuesMap.put(currentTemplate, new TrackedValues(currentTemplate, values));
        }
        updateActionButton();
    }

    void updateActionButton() {
        boolean hasChanges = getTrackedValues(currentTemplate).map(TrackedValues::hasChanges).orElse(false);
        this.actionButton.setEnabled(hasChanges && !this.form.isEmpty());
    }

    private void resetLayout() {
        this.removeAll();

        if (this.form != null) {
            SpringLayoutOrganizer.organize(this)
                    .add(this.form)
                    .atTop()
                    .definingContainerHeight()
                    .atLeftSide()
                    .atRightSide();
        }

        if (this.getParent() != null) {
            this.invalidate();
            this.getParent().validate();
            this.repaint();
        }
    }

    private record TrackedValues(@Nonnull Template template, TemplateValues original, TemplateValues current) {
        public TrackedValues(@Nonnull Template template, TemplateValues original) {
            this(template, original, original);
        }

        public TrackedValues withCurrentValues(TemplateValues current) {
            return new TrackedValues(this.template, this.original, current);
        }

        public TrackedValues reset() {
            return new TrackedValues(this.template, this.original, this.original);
        }

        public boolean hasChanges() {
            if (this.original == this.current) {
                return false;
            } else {
                return this.template.variables()
                        .stream()
                        .anyMatch(var -> !Objects.equals(this.original.getValue(var), this.current.getValue(var)));
            }
        }
    }
}
