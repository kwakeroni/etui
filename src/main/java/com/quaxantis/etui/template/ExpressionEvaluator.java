package com.quaxantis.etui.template;

import com.quaxantis.etui.Template;
import com.quaxantis.etui.TemplateValues;
import com.quaxantis.etui.template.parser.matching.Binding;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ExpressionEvaluator {
    String evaluate(String expression, TemplateValues values);

    default Collection<Binding> deinterpolate(String expression, Map<String, String> boundVariables, String evaluatedExpression) {
        // TODO make Binding EEL-independent
        return Set.of();
    }

    interface Context {
        Optional<Template.Variable> resolveVariable(String name);

        static Context of(Collection<? extends Template.Variable> vars) {
            return name -> vars.stream()
                    .filter(var -> var.name().equals(name))
                    .findAny()
                    .map(Function.identity());
        }
    }

    static ExpressionEvaluator of(Context context) {
        return new SimpleNameEvaluator(context);
    }

    class SimpleNameEvaluator implements ExpressionEvaluator {
        private final Context context;

        private SimpleNameEvaluator(Context context) {
            this.context = context;
        }

        @Override
        public String evaluate(String expression, TemplateValues values) {
            StringBuilder builder = new StringBuilder();
            Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)}");
            Matcher matcher = pattern.matcher(expression);
            while (matcher.find()) {
                String value = this.context.resolveVariable(matcher.group(1))
                        .map(var -> values.getValue(var).orElse(""))
                        .orElse(matcher.group(0));
                matcher.appendReplacement(builder, Matcher.quoteReplacement(value));
            }
            matcher.appendTail(builder);
            return builder.toString();
        }
    }
}
