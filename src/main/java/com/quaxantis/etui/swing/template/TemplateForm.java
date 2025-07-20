package com.quaxantis.etui.swing.template;

import com.quaxantis.etui.HasLabel;
import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.Template.Variable;
import com.quaxantis.etui.TemplateValues;
import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.application.config.Configuration;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.etui.template.TemplateMapper;
import com.quaxantis.etui.template.TemplateRepository;
import com.quaxantis.etui.template.expression.ExpressionEvaluator;
import com.quaxantis.support.swing.form.AbstractForm;
import com.quaxantis.support.swing.form.FormField;
import com.quaxantis.support.swing.form.FormFieldInput;
import com.quaxantis.support.swing.form.FormListener;
import com.quaxantis.support.swing.util.QuickJFrame;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.quaxantis.support.util.MoreCollectors.uniqueKeysMerger;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

public class TemplateForm extends AbstractForm<TemplateValues, TemplateForm> {
    private final Logger log = LoggerFactory.getLogger(TemplateForm.class);

    private final Template template;
    private final ExpressionEvaluator expressionEvaluator;
    private final TemplateMapper mapper;
    private final SequencedMap<Variable, FormField<TemplateValues, TemplateValues.Entry>> fields;

    // CONSTRUCTOR

    public TemplateForm(Template template) {
        this(template, true);
    }

    public TemplateForm(Template template, boolean isDoubleBuffered) {
        super(isDoubleBuffered);

        this.expressionEvaluator = template.expressionEvaluator();
        this.mapper = new TemplateMapper(template);

        // this::evaluateExpression requires this.expressionEvaluator to be set
        this.fields = initFields(template, this::evaluateExpression);
        this.template = template;

        addFormListener(new FormListener.Adapter<>() {
            @Override
            public void onChanged(TemplateForm form) {
                updateExpressions();
            }
        });

        initialize();
    }


    @Override
    protected TemplateValues createBean() {
        return this.mapper.newValues();
    }

    @Override
    protected SequencedCollection<? extends FormField<TemplateValues, ?>> fields() {
        return this.fields.sequencedValues();
    }

    // UPDATE EXPRESSIONS

    private void updateExpressions() {
        if (isSettingData()) {
            return;
        }
        TemplateValues values = this.getData();

        this.fields.entrySet()
                .stream()
                .filter(entry -> entry.getKey().hasExpression())
                .filter(entry -> entry.getValue().isEmpty() || !entry.getValue().isDirty())
                .forEach(entry -> {
                    var var = entry.getKey();
                    try {
                        String newValue = expressionEvaluator.evaluate(var.expression(), values);
                        values.set(var, newValue);
                    } catch (Exception exc) {
                        log.error("Error while evaluating variable {} in template {}", var.name(), this.template, exc);
                    }
                });

        setData(values);
    }

    private Optional<String> evaluateExpression(Variable var) {
        try {
            return Optional.of(var)
                    .map(Variable::expression)
                    .map(expression -> expressionEvaluator.evaluate(expression, getData()));
        } catch (Exception exc) {
            log.error("Error while evaluating variable {} in template {}", var.name(), this.template, exc);
            return Optional.empty();
        }
    }

    //  TAGSET

    public TagSet getTags() {
        return mapper.toTags(getData());
    }

    public void setTags(TagSet tagSet) {
        TemplateValues values = mapper.fromTags(tagSet);
        setData(values);
    }

    private static SequencedMap<Variable, FormField<TemplateValues, TemplateValues.Entry>> initFields(Template template, Function<Variable, Optional<String>> expressionEvaluator) {
        return template.variables().stream().collect(
                collectingAndThen(
                        toMap(identity(),
                              var -> field(var, expressionEvaluator),
                              uniqueKeysMerger(),
                              LinkedHashMap::new),
                        Collections::unmodifiableSequencedMap
                ));

    }

    private static FormField<TemplateValues, TemplateValues.Entry> field(Variable variable, Function<Variable, Optional<String>> expressionEvaluator) {
        Function<TemplateValues, TemplateValues.Entry> getter = values -> values.get(variable).orElse(null);
        BiConsumer<TemplateValues, TemplateValues.Entry> setter = (values, entry) -> values.set(variable, entry.value().orElse(null));
        Supplier<Optional<String>> expressionValue = () -> expressionEvaluator.apply(variable);

        String label = HasLabel.labelOf(variable, () -> StringUtils.capitalize(variable.name())) + (variable.hasExpression()? "°" : "");
        return field(variable.name(), getter, setter, expressionValue)
                .withSource(variable)
                .withLabel(label)
                .build();
    }

    private static <T> FormField.Builder<T, TemplateValues.Entry> field(
            String variableName,
            Function<T, TemplateValues.Entry> getter,
            BiConsumer<T, TemplateValues.Entry> setter,
            Supplier<Optional<String>> expressionValue) {
        return FormField.builder()
                .withComponentBinding(fieldInput(variableName, expressionValue), getter, setter);
    }

    private static FormFieldInput<?, TemplateValues.Entry> fieldInput(String variableName, Supplier<Optional<String>> expressionValue) {
        return new TemplateFormFieldInput(variableName, expressionValue, FormFieldInput.ofTextField());
    }


    public static void main() {
        QuickJFrame.of(repositoryForm())
                .withTitle("Template Form")
                .onDispose(form -> {
                    TagSet tagSet = form.getTags();
                    tagSet.forEach(System.out::println);
                })
                .show();

    }

    private static TemplateForm repositoryForm() {
        Configuration configuration = new ConfigOperations().getConfiguration();
        TemplateRepository repo = new TemplateRepository(
                configuration,
                new TagRepository(configuration)
        );

        repo.templates().stream().map(Template::name).forEach(System.out::println);
        Template template = repo.templates().stream()
                .filter(tpl -> tpl.name().contains("Test Template 3"))
                .findAny().orElseThrow();

        return new TemplateForm(template);
    }
}
