package com.quaxantis.etui.template.parser.eel;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ExpressionResolver")
@ExtendWith(MockitoExtension.class)
class EELExpressionResolverTest {
    @Mock
    private Function<String, String> identifierResolver;
    @InjectMocks
    private EELExpressionResolver resolver;

    @Test
    @DisplayName("resolves an Identifier expression by looking up the identifier")
    void testResolveIdentifier() {
        when(identifierResolver.apply(any())).thenReturn("myValue");

        assertThat(resolver.resolve(
                new Expression.Identifier("myVariable")))
                .isEqualTo("myValue");

        verify(identifierResolver).apply("myVariable");
    }

    @Test
    @DisplayName("resolves a Text expression")
    void testResolve() {
        assertThat(resolver.resolve(new Expression.Text("my literal String")))
                .isEqualTo("my literal String");
    }

    @Test
    @DisplayName("resolves a Concat expression")
    void testResolveConcat() {
        when(identifierResolver.apply(any())).thenReturn("myValue", "anotherValue");

        assertThat(resolver.resolve(
                new Expression.Concat(
                        new Expression.Identifier("myVariable"),
                        new Expression.Text(" - "),
                        new Expression.Identifier("anotherVariable")
                ))).isEqualTo("myValue - anotherValue");

        verify(identifierResolver).apply("myVariable");
        verify(identifierResolver).apply("anotherVariable");
    }

    @Test
    @DisplayName("resolves a Concat expression when one of the parts is null")
    void testResolveConcatWithNull() {
        when(identifierResolver.apply(any())).thenReturn(null, "anotherValue");

        assertThat(resolver.resolve(
                new Expression.Concat(
                        new Expression.Identifier("myVariable"),
                        new Expression.Text(" - "),
                        new Expression.Identifier("anotherVariable")
                ))).isEqualTo(" - anotherValue");

        verify(identifierResolver).apply("myVariable");
        verify(identifierResolver).apply("anotherVariable");
    }

    @Test
    @DisplayName("resolves an Elvis expression to the main expression")
    void testResolveElvisWhenDefined() {
        when(identifierResolver.apply(any())).thenReturn("myValue");

        assertThatResolvingElvis("myVariable", "anotherVariable")
                .isEqualTo("myValue");

        verifyResolvedIdentifiers("myVariable");
    }

    @Test
    @DisplayName("resolves an Elvis expression to the fallback when the main expression is null")
    void testResolveElvisWhenNull() {
        when(identifierResolver.apply("myVariable")).thenReturn(null);
        when(identifierResolver.apply("anotherVariable")).thenReturn("anotherValue");

        assertThatResolvingElvis("myVariable", "anotherVariable")
                .isEqualTo("anotherValue");

        verifyResolvedIdentifiers("myVariable", "anotherVariable");
    }

    @Test
    @DisplayName("resolves an Elvis expression to the fallback when the main expression is empty")
    void testResolveElvisWhenEmpty() {
        when(identifierResolver.apply("myVariable")).thenReturn("");
        when(identifierResolver.apply("anotherVariable")).thenReturn("anotherValue");

        assertThatResolvingElvis("myVariable", "anotherVariable")
                .isEqualTo("anotherValue");

        verifyResolvedIdentifiers("myVariable", "anotherVariable");
    }

    private AbstractStringAssert<?> assertThatResolvingElvis(String mainVar, String orElseVar) {
        return assertThatResolving(new Expression.Elvis(
                new Expression.Identifier(mainVar),
                new Expression.Identifier(orElseVar)
        ));
    }

    @Test
    @DisplayName("resolves an OptSuffix expression to the main expression concatenated to the suffix expression")
    void testResolveOptSuffixWhenDefined() {
        when(identifierResolver.apply("myVariable")).thenReturn("myValue");
        when(identifierResolver.apply("anotherVariable")).thenReturn("WithSuffix");

        assertThatResolvingOptSuffix("myVariable", "anotherVariable")
                .isEqualTo("myValueWithSuffix");

        verifyResolvedIdentifiers("myVariable", "anotherVariable");
    }

    @Test
    @DisplayName("resolves an OptSuffix expression when the main expression is null")
    void testResolveOptSuffixWhenNull() {
        when(identifierResolver.apply(any())).thenReturn(null, "WithSuffix");

        assertThatResolvingOptSuffix("myVariable", "anotherVariable")
                .isNull();

        verifyResolvedIdentifiers("myVariable");
    }

    @Test
    @DisplayName("resolves an OptSuffix expression when the main expression is empty")
    void testResolveOptSuffixWhenEmpty() {
        when(identifierResolver.apply(any())).thenReturn("", "WithSuffix");

        assertThatResolvingOptSuffix("myVariable", "anotherVariable")
                .isEqualTo("");

        verifyResolvedIdentifiers("myVariable");
    }

    @Test
    @DisplayName("resolves an OptSuffix expression when the suffix is null")
    void testResolveOptSuffixWhenSuffixIsNull() {
        when(identifierResolver.apply("myVariable")).thenReturn("myValue");
        when(identifierResolver.apply("anotherVariable")).thenReturn(null);

        assertThatResolvingOptSuffix("myVariable", "anotherVariable")
                .isEqualTo("myValue");

        verifyResolvedIdentifiers("myVariable", "anotherVariable");
    }

    private AbstractStringAssert<?> assertThatResolvingOptSuffix(String mainVar, String suffixVar) {
        return assertThatResolving(new Expression.OptSuffix(
                new Expression.Identifier(mainVar),
                new Expression.Identifier(suffixVar)
        ));
    }

    @Test
    @DisplayName("resolves an OptPrefix expression to the prefix expression concatenated to the main expression")
    void testResolveOptPrefixWhenDefined() {
        when(identifierResolver.apply("myVariable")).thenReturn("myValue");
        when(identifierResolver.apply("anotherVariable")).thenReturn("PrefixWith");

        assertThatResolvingOptPrefix("anotherVariable", "myVariable")
                .isEqualTo("PrefixWithmyValue");

        verifyResolvedIdentifiers("myVariable", "anotherVariable");
    }


    @Test
    @DisplayName("resolves an OptPrefix expression when the main expression is null")
    void testResolveOptPrefixWhenNull() {
        when(identifierResolver.apply(any())).thenReturn(null, "PrefixWith");

        assertThatResolvingOptPrefix("anotherVariable", "myVariable")
                .isNull();

        verifyResolvedIdentifiers("myVariable");
    }

    @Test
    @DisplayName("resolves an OptPrefix expression when the main expression is empty")
    void testResolveOptPrefixWhenEmpty() {
        when(identifierResolver.apply(any())).thenReturn("", "PrefixWith");

        assertThatResolvingOptPrefix("anotherVariable", "myVariable")
                .isEqualTo("");

        verifyResolvedIdentifiers("myVariable");
    }

    @Test
    @DisplayName("resolves an OptPrefix expression when the prefix is null")
    void testResolveOptPrefixWhenPrefixIsNull() {
        when(identifierResolver.apply("myVariable")).thenReturn("myValue");
        when(identifierResolver.apply("anotherVariable")).thenReturn(null);

        assertThatResolvingOptPrefix("anotherVariable", "myVariable")
                .isEqualTo("myValue");

        verifyResolvedIdentifiers("myVariable", "anotherVariable");
    }

    private AbstractStringAssert<?> assertThatResolvingOptPrefix(String prefixVar, String mainVar) {
        return assertThatResolving(new Expression.OptPrefix(
                new Expression.Identifier(prefixVar),
                new Expression.Identifier(mainVar)
        ));
    }

    private AbstractStringAssert<?> assertThatResolving(Expression expression) {
        return assertThat(resolver.resolve(expression));
    }

    private void verifyResolvedIdentifiers(String... vars) {
        for (String var : vars) {
            verify(identifierResolver).apply(var);
        }
        verify(identifierResolver, times(vars.length)).apply(any());
    }
}
