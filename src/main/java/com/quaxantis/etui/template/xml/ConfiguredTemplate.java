package com.quaxantis.etui.template.xml;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagDescriptor;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.etui.template.TagMapping;
import com.quaxantis.etui.template.VariableSupport;
import com.quaxantis.etui.template.expression.ExpressionEvaluator;
import com.quaxantis.etui.template.expression.ExpressionEvaluatorFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfiguredTemplate implements Template {

    private final String name;
    private final String source;
    private final List<Variable> variables;
    private final List<TagMapping> tagMappings;
    private final ExpressionEvaluator expressionEvaluator;


    private ConfiguredTemplate(String name, String source, List<Variable> variables, List<TagMapping> tagMappings, ExpressionEvaluator expressionEvaluator) {
        this.name = name;
        this.source = source;
        this.variables = variables;
        this.tagMappings = tagMappings;
        this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name=" + this.name + ", source=" + this.source + "}";
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public List<Variable> variables() {
        return this.variables;
    }

    @Override
    public Collection<Tag> tags(Variable variable) {
        // Variables are matched by identity - not by name only -
        // because a variable with a name does not necessarily represent the same thing
        // as a variable with the same name in another template
        return tagMappings.stream()
                .filter(mapping -> mapping.variable().map(variable::equals).orElse(false))
                .map(TagMapping::tags)
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public ExpressionEvaluator expressionEvaluator() {
        return this.expressionEvaluator;
    }

    public static ConfiguredTemplate of(String source, XMLTemplate template, TagRepository tagRepository) {
        var variables = template.variables().stream().map(var -> enrich(var, tagRepository)).toList();
        var variablesByName = Stream.ofNullable(variables).flatMap(Collection::stream).collect(Collectors.toMap(Variable::name, Function.identity()));
        var tagMappings = tagMappings(template, variablesByName);
        var expressionEvaluator = createEvaluator(template, ExpressionEvaluator::of, ExpressionEvaluator.Context.of(variables));
        return new ConfiguredTemplate(template.name(), source, variables, tagMappings, expressionEvaluator);
    }

    private static Variable enrich(XMLTemplateVariable xmlVariable, TagRepository tagRepository) {
        Tag[] tags = xmlVariable.tags()
                .stream()
                .flatMap(qualifiedName -> tagRepository.findTagByQualifiedName(qualifiedName).stream())
                .filter(TagDescriptor.class::isInstance)
                .toArray(Tag[]::new);

        return (tags.length == 1)? new VariableSupport(xmlVariable, tags[0]) : xmlVariable;
    }

    private static ExpressionEvaluator createEvaluator(XMLTemplate xmlTemplate, ExpressionEvaluatorFactory expressionEvaluatorFactory, ExpressionEvaluator.Context context) {
        return xmlTemplate.evaluator()
                .map(type -> createEvaluatorOfType(type, context))
                .orElseGet(() -> expressionEvaluatorFactory.create(context));
    }

    private static List<TagMapping> tagMappings(XMLTemplate xmlTemplate, Map<String, Variable> variablesByName) {
        return Stream.concat(implicitMappings(xmlTemplate, variablesByName), explicitMappings(xmlTemplate, variablesByName)).toList();
    }

    private static ExpressionEvaluator createEvaluatorOfType(String typeName, ExpressionEvaluator.Context context) {
        try {
            Class<? extends ExpressionEvaluator> type = Class.forName(typeName).asSubclass(ExpressionEvaluator.class);
            return type.getConstructor(ExpressionEvaluator.Context.class).newInstance(context);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException("Unable to create ExpressionEvaluator of type " + typeName, e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to create ExpressionEvaluator of type " + typeName, e.getCause());
        }
    }

    private static Stream<TagMapping> implicitMappings(XMLTemplate xmlTemplate, Map<String, Variable> variablesByName) {
        return xmlTemplate.variables().stream()
                .flatMap(variable -> implicitMappings(variable, variablesByName));
    }

    private static Stream<TagMapping> implicitMappings(XMLTemplateVariable xmlVariable, Map<String, Variable> variablesByName) {
        Variable variable = variablesByName.get(xmlVariable.name());
        return xmlVariable.tags().stream()
                .map(qualifiedTagName -> TagMapping.between(variable, Tag.ofQualifiedName(qualifiedTagName)));
    }

    private static Stream<TagMapping> explicitMappings(XMLTemplate xmlTemplate, Map<String, Variable> variablesByName) {
        return Stream.ofNullable(xmlTemplate.mappings())
                .flatMap(Collection::stream)
                .map(xmltm -> toTagMapping(variablesByName, xmltm));
    }

    private static TagMapping toTagMapping(Map<String, ? extends Variable> variables, XMLTagMapping xmltm) {
        Tag tag = Tag.ofQualifiedName(xmltm.tag());
        return xmltm.variable().map(varName -> {
                    Variable var = variables.get(varName);
                    if (var == null) {
                        throw new IllegalStateException("Unknown variable " + varName);
                    }
                    return TagMapping.between(var, tag);
                })
                .or(() -> xmltm.expression().map(expr -> TagMapping.evaluating(expr, tag)))
                .orElseThrow(() -> new IllegalArgumentException("Unable to handle mapping " + xmltm));
    }

}
