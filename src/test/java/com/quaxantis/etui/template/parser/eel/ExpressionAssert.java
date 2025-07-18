package com.quaxantis.etui.template.parser.eel;

import com.quaxantis.etui.template.parser.matching.Match;
import com.quaxantis.etui.template.parser.matching.MatchingAssert;
import com.quaxantis.support.ide.API;
import org.assertj.core.api.AbstractObjectAssert;

import java.util.Map;

import static java.util.function.Predicate.not;

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
        return new MatchingAssert(actual, string, analyzer.match(actual, string, bindings).toList());
    }
}
