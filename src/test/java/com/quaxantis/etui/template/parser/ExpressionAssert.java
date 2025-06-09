package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.MatchAssert.MatchWithExpressionAssert;
import com.quaxantis.support.ide.API;
import org.assertj.core.api.AbstractObjectAssert;

public class ExpressionAssert extends AbstractObjectAssert<ExpressionAssert, Expression> {

    private static final EELExpressionAnalyzer analyzer = new EELExpressionAnalyzer();

    @API
    public static ExpressionAssert assertThat(Expression expression) {
        return new ExpressionAssert(expression);
    }

    public ExpressionAssert(Expression expression) {
        super(expression, ExpressionAssert.class);
    }

    @API
    public ExpressionAssert doesNotMatch(String string) {
        matching(string).isNoMatch();
        return myself;
    }

    @API
    public ExpressionAssert matches(String string) {
        matching(string).isFullMatch();
        return myself;
    }

    @API
    public MatchWithExpressionAssert matching(String string) {
        return new MatchWithExpressionAssert(analyzer.match(actual, string), actual);
    }
}
