package com.quaxantis.support.swing.form;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SimpleForm<T> extends AbstractForm<T, SimpleForm<T>> {

    private final Supplier<? extends T> beanSupplier;
    private final List<? extends FormField<T, ?>> fields;

    @SafeVarargs
    public SimpleForm(Supplier<? extends T> beanSupplier, FormField<T, ?>... fields) {
        this(beanSupplier, List.of(fields));
    }

    @SafeVarargs
    public SimpleForm(Supplier<? extends T> beanSupplier, FormField.Builder<T, ?>... fields) {
        this(beanSupplier, Arrays.stream(fields).map(FormField.Builder::build).collect(Collectors.toUnmodifiableList()));
    }

    public SimpleForm(Supplier<? extends T> beanSupplier, List<? extends FormField<T, ?>> fields) {
        this(beanSupplier, fields, true);
    }

    public SimpleForm(Supplier<? extends T> beanSupplier, List<? extends FormField<T, ?>> fields, boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        this.fields = fields;
        this.beanSupplier = beanSupplier;
        initialize();
    }

    @Override
    protected List<? extends FormField<T, ?>> fields() {
        return this.fields;
    }

    @Override
    protected T createBean() {
        return beanSupplier.get();
    }
}
