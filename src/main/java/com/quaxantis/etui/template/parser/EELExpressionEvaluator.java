package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.TemplateValues;
import com.quaxantis.etui.template.ExpressionEvaluator;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class EELExpressionEvaluator implements ExpressionEvaluator {
    private final Context context;
    private final SimpleParser parser = new SimpleParser();
    private final EELExpressionAnalyzer analyzer = new EELExpressionAnalyzer();

    public EELExpressionEvaluator(Context context) {
        this.context = context;
    }

    @Override
    public String evaluate(String expression, TemplateValues values) {
        ExpressionResolver resolver = new ExpressionResolver(
                varName -> resolveVariable(values, varName).orElse(null));
        return resolver.resolve(parser.parse(expression));
    }

    @Override
    public Collection<Binding> deinterpolate(String expression, Map<String, String> boundVariables, String evaluatedExpression) {
        Expression expr = parser.parse(expression);
        return analyzer.findBindings(expr, evaluatedExpression, boundVariables).toList();
    }

    private Optional<String> resolveVariable(TemplateValues values, String varName) {
        var variable = context.resolveVariable(varName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown variable: " + varName));
        return values.getValue(variable);
    }
}
