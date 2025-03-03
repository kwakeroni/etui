package com.quaxantis.support.swing.form;

import javax.swing.JComponent;
import java.util.function.BiFunction;
import java.util.function.Function;

public class WrappedFormFieldInput<C extends JComponent, V, DV> implements FormFieldInput<C, V> {
    protected final C wrappedComponent;
    protected final FormFieldInput<?, DV> delegate;
    private final Function<V, DV> inputFunction;
    private final BiFunction<V, DV, V> outputFunction;
    private V currentValue;

    public static <C extends JComponent, DC extends JComponent, V> WrappedFormFieldInput<C, V, V> of(Function<DC, C> wrapper, FormFieldInput<DC, V> delegate) {
        return new WrappedFormFieldInput<C, V, V>(wrapper.apply(delegate.component()), Function.identity(), delegate, Function.identity());
    }

    public static <C extends JComponent, V> WrappedFormFieldInput<C, V, V> of(C wrappedComponent, FormFieldInput<?, V> delegate) {
        return new WrappedFormFieldInput<C, V, V>(wrappedComponent, Function.identity(), delegate, Function.identity());
    }

    public WrappedFormFieldInput(C wrappedComponent, Function<V, DV> inputFunction, FormFieldInput<?, DV> delegate, Function<DV, V> outputFunction) {
        this(wrappedComponent, inputFunction, delegate, (old, $new) -> outputFunction.apply($new));
    }
    public WrappedFormFieldInput(C wrappedComponent, Function<V, DV> inputFunction, FormFieldInput<?, DV> delegate, BiFunction<V, DV, V> outputFunction) {
        this.wrappedComponent = wrappedComponent;
        this.inputFunction = inputFunction;
        this.outputFunction = outputFunction;
        this.delegate = delegate;
    }

    @Override
    public C component() {
        return wrappedComponent;
    }

    @Override
    public void setValue(V value) {
        this.currentValue = value;
        delegate.setValue(inputFunction.apply(value));
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public V getValue() {
        return outputFunction.apply(this.currentValue, delegate.getValue());
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean isDirty() {
        return delegate.isDirty();
    }

    @Override
    public void applyPattern(String pattern) {
        delegate.applyPattern(pattern);
    }

    @Override
    public void addFormFieldListener(FormField<?, ?> formField, FormFieldListener formFieldListener) {
        delegate.addFormFieldListener(formField, formFieldListener);
    }
}
