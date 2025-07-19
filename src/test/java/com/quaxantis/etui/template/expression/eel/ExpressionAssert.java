package com.quaxantis.etui.template.expression.eel;

import com.quaxantis.etui.template.expression.Binding;
import com.quaxantis.etui.template.expression.matching.Match;
import com.quaxantis.etui.template.expression.matching.MatchingAssert;
import com.quaxantis.support.ide.API;
import org.assertj.core.api.AbstractObjectAssert;

import java.util.Map;
import java.util.function.BiFunction;

import static java.util.function.Predicate.not;

public class ExpressionAssert extends AbstractObjectAssert<ExpressionAssert, Expression> {

    private static final EELExpressionAnalyzer analyzer = new EELExpressionAnalyzer();
    private static final BiFunction<Expression, Binding, String> resolver = (expression, binding) ->
            new EELExpressionResolver(var -> binding.valueOf(var).orElse("")).resolve(expression);

    @API
    public static ExpressionAssert assertThat(Expression expression) {
        return new ExpressionAssert(expression);
    }

    public ExpressionAssert(Expression expression) {
        super(expression, ExpressionAssert.class);
    }

    @API
    public ExpressionAssert doesNotMatch(String string) {
        matching(string).allMatch(Match.NoMatch.class::isInstance);
        return myself;
    }

    @API
    public ExpressionAssert matches(String string) {
        matching(string).anyMatch(not(Match.NoMatch.class::isInstance));
        return myself;
    }

    @API
    public MatchingAssert matching(String string) {
        return matching(string, Map.of());
    }

    @API
    public MatchingAssert matching(String string, Map<String, String> bindings) {
        return new MatchingAssert(actual, resolver, string, analyzer.match(actual, string, bindings).toList());
    }
}
