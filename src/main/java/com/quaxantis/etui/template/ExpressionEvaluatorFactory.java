package com.quaxantis.etui.template;

public interface ExpressionEvaluatorFactory {

    ExpressionEvaluator create(ExpressionEvaluator.Context context);

}
