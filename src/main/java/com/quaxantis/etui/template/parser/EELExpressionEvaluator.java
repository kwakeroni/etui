package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.TemplateValues;
import com.quaxantis.etui.template.ExpressionEvaluator;

import java.util.Optional;

public class EELExpressionEvaluator implements ExpressionEvaluator {
    private final Context context;
    private final SimpleParser parser = new SimpleParser();

    public EELExpressionEvaluator(Context context) {
        this.context = context;
    }

    @Override
    public String evaluate(String expression, TemplateValues values) {
        ExpressionResolver resolver = new ExpressionResolver(
                varName -> resolveVariable(values, varName).orElse(null));
        return resolver.resolve(parser.parse(expression));
    }

    private Optional<String> resolveVariable(TemplateValues values, String varName) {
        var variable = context.resolveVariable(varName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown variable: " + varName));
        return values.getValue(variable);
    }
}
