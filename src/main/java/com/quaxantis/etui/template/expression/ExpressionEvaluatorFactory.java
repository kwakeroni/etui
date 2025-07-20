package com.quaxantis.etui.template.expression;

public interface ExpressionEvaluatorFactory {

    ExpressionEvaluator create(ExpressionEvaluator.Context context);

}
