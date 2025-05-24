package com.quaxantis.etui.template.parser;

import com.quaxantis.etui.template.parser.EELExpressionAnalyzer.Match.FlexMatch;
import com.quaxantis.etui.template.parser.EELExpressionAnalyzer.Match.NoMatch;
import com.quaxantis.support.util.Result;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class EELExpressionAnalyzer {

    public Bindings detectBindings(Expression expression, String resolvedExpression) {
        return switch (expression) {
            case Expression.Identifier identifier -> bindIdentifier(identifier, resolvedExpression);
            case Expression.Elvis elvis -> bindElvis(elvis, resolvedExpression);
            default -> Bindings.of();
        };
    }

    private Bindings bindIdentifier(Expression.Identifier identifier, String resolvedExpression) {
        return Bindings.of(new Binding(identifier.name(), resolvedExpression));
    }

    private Bindings bindElvis(Expression.Elvis elvis, String resolvedExpression) {
        Bindings bindLeft = detectBindings(elvis.expression(), resolvedExpression);
        Bindings bindEmptyLeft = detectBindings(elvis.expression(), "");
        Bindings bindOrElse = detectBindings(elvis.orElse(), resolvedExpression);
        Bindings bindRight = bindEmptyLeft.addSubBindings(bindOrElse);


        return bindLeft.concat(bindRight);
    }

    public Match match(Expression expression, String string) {
        return switch (expression) {
            case Expression.Text text -> matchText(text, string);
            case Expression.Identifier identifier -> matchIdentifier(identifier, string);
//            case Expression.Elvis elvis -> bindElvis(elvis, resolvedExpression);
            case Expression.OptSuffix optSuffix -> matchOptSuffix(optSuffix, string);
            default -> new NoMatch();
        };
    }

    private Match matchIdentifier(Expression.Identifier identifier, String resolvedExpression) {
        IntRange fullRange = IntRange.of(0, resolvedExpression.length());
        RangeFlex flex = RangeFlex.of(fullRange, fullRange).withMinLength(0);
        return new FlexMatch(flex);
    }

    private Match matchText(Expression.Text textExpr, String string) {
        Objects.requireNonNull(textExpr);
        Objects.requireNonNull(string);
        String text = textExpr.literal();
        if (string.equals(text)) {
            int end = string.length() - 1;
            return new FlexMatch(RangeFlex.ofFixed(0, end).withMinLength(end - 0 + 1));
        } else {
            int index = string.indexOf(text);
            if (index < 0) {
                return new NoMatch();
            } else {
                int end = index + text.length() - 1;
                return new FlexMatch(RangeFlex.ofFixed(index, end).withMinLength(end - index + 1));
            }
        }
    }

    private Match matchOptSuffix(Expression.OptSuffix optSuffix, String string) {
        Match leftMatch = match(optSuffix.expression(), string);
        Match rightMatch = match(optSuffix.suffix(), string);

        if (leftMatch instanceof FlexMatch(var leftFlex)
            && rightMatch instanceof FlexMatch(var rightFlex)
            && RangeFlex.tryConcat(leftFlex.withMinLength(1), rightFlex) instanceof Result.Success(
                var concatRangeFlex
        )) {
            return new FlexMatch(concatRangeFlex);
        } else {
            return Match.noMatch();
        }
    }

    private static final Match NO_MATCH = new NoMatch();

    public sealed interface Match {

        static Match noMatch() {
            return NO_MATCH;
        }


        record NoMatch() implements Match {
        }

        record FlexMatch(RangeFlex flex) implements Match {

        }
    }

    public record Binding(
            String variable,
            String value,
            Bindings subBindings
    ) {

        public Binding {
            Objects.requireNonNull(variable, "variable");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(subBindings, "subBindings");
        }

        public Binding(String variable, String value) {
            this(variable, value, Bindings.of());
        }

        public Binding addSubBindings(Bindings bindings) {
            return new Binding(variable, value, subBindings.concat(bindings));
        }
    }

    public static class Bindings implements Iterable<Binding> {
        private static final Bindings EMPTY = new Bindings(List.of());
        private final List<Binding> bindings;

        public static Bindings of() {
            return EMPTY;
        }

        public static Bindings of(Binding binding) {
            Objects.requireNonNull(binding, "binding");
            return new Bindings(List.of(binding));
        }

        private Bindings(List<Binding> bindings) {
            this.bindings = bindings;
        }

        public boolean isEmpty() {
            return this.bindings.isEmpty();
        }

        @Override
        public Iterator<Binding> iterator() {
            return this.bindings.iterator();
        }

        public Stream<Binding> stream() {
            return this.bindings.stream();
        }

        public Bindings addSubBindings(Bindings subBindings) {
            return new Bindings(
                    this.bindings.stream()
                            .map(binding -> binding.addSubBindings(subBindings))
                            .toList());
        }

        public Bindings concat(Bindings addedBindings) {
            return new Bindings(Stream.concat(this.stream(), addedBindings.stream()).toList());
        }

        @Override
        public String toString() {
            return "Bindings" + bindings.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Bindings bindings1 = (Bindings) o;
            return Objects.equals(bindings, bindings1.bindings);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(bindings);
        }
    }

}
