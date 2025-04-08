package com.quaxantis.support.swing.form;

import com.quaxantis.support.swing.Dimensions;
import com.quaxantis.support.swing.layout.SpringLayoutOrganizer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AbstractForm<T, F extends AbstractForm<T, F>> extends JPanel {
    private static final int PAD = 10;
    private static final Object PROPERTY_INFO_COMPONENT = new Object();

    protected final F self;
    private final AtomicReference<T> dataBeingSet = new AtomicReference<>();
    private Dimension wrappedContentSize;

    @SafeVarargs
    protected AbstractForm(F... noForms) {
        this(true, noForms);
    }

    @SafeVarargs
    public AbstractForm(boolean isDoubleBuffered, F... noForms) {
        super(isDoubleBuffered);
        @SuppressWarnings("unchecked")
        Class<F> selfType = (Class<F>) noForms.getClass().getComponentType();
        this.self = selfType.cast(this);
    }

    // ABSTRACT

    protected abstract SequencedCollection<? extends FormField<T, ?>> fields();

    protected abstract T createBean();

    // FORM LISTENERS

    public void addFormListener(FormListener<? super F> listener) {
        this.listenerList.add(FormListener.class, listener);
    }

    protected void fireFormChange() {
        fireFormEvent(listener -> listener.onChanged(self));
    }

    protected void fireFormEvent(Consumer<FormListener<? super F>> consumer) {
        for (@SuppressWarnings("unchecked") FormListener<? super F> listener : this.listenerList.getListeners(FormListener.class)) {
            consumer.accept(listener);
        }
    }

    // DATA

    public T getData() {
        if (dataBeingSet.get() instanceof T t) {
            return t;
        }
        T bean = createBean();
        for (FormField<T, ?> field : fields()) {
            field.putValueIn(bean);
        }
        return bean;
    }

    public void setData(T bean) {
        T oldData = dataBeingSet.getAndSet(bean);
        try {
            for (FormField<T, ?> field : fields()) {
                field.setValueFrom(bean);
            }
            fireFormChange();
        } finally {
            dataBeingSet.set(oldData);
        }
    }

    public void modifyData(Consumer<T> modifier) {
        T bean = getData();
        modifier.accept(bean);
        setData(bean);
    }

    protected boolean isSettingData() {
        return dataBeingSet.get() != null;
    }

    public void clear() {
        for (FormField<?, ?> field : fields()) {
            field.clear();
        }
        fireFormChange();
    }

    public boolean isEmpty() {
        for (FormField<?, ?> field : fields()) {
            if (!field.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // LAYOUT

    public Dimension getWrappedContentSize() {
        return this.wrappedContentSize;
    }

    protected void initialize() {

        this.setOpaque(true);

        var organizer = SpringLayoutOrganizer.organize(this).setDefaultPadding(PAD);
        positionLabels(organizer);
        this.wrappedContentSize = positionFields(organizer);
    }

    private void positionLabels(SpringLayoutOrganizer organizer) {
        if (fields().size() == 0) {
            return;
        }

        var widestLabel = fields().getFirst().label();
        for (FormField<T, ?> field : fields()) {
            var label = field.label();
            if (label.getPreferredSize().width > widestLabel.getPreferredSize().width) {
                // Keep track of the widest label
                widestLabel = label;
            }
        }

        var refLabel = widestLabel;

        for (FormField<T, ?> field : fields()) {
            var label = field.label();

            organizer.position(label)
                    .when(label == refLabel, SpringLayoutOrganizer.Position::atLeftSide)
                    .orElse(position -> position.atSameRightEdgeAs(refLabel));
        }
    }

    private Dimension positionFields(SpringLayoutOrganizer organizer) {
        FormFieldListener formFieldListener = formFieldListener();
        boolean hasDescriptions = fields().stream().map(FormField::description).anyMatch(Objects::nonNull);
        int minWidth = 200;
        int totalHeight = PAD;

        int i = 0;
        FormField<T, ?> previousField = null;
        for (FormField<T, ?> field : fields()) {
            var isLastField = (++i == fields().size());

            field.addFormFieldListener(formFieldListener);

            var dimension = positionField(organizer, previousField, field, isLastField, hasDescriptions);

            minWidth = Math.max(minWidth, dimension.width);
            totalHeight += dimension.height;

            previousField = field;
        }
        Dimension dimension = new Dimension(minWidth, totalHeight);
        return dimension;
    }

    private Dimension positionField(SpringLayoutOrganizer organizer,
                                    @Nullable FormField<?, ?> previousField,
                                    FormField<?, ?> field,
                                    boolean isLastField,
                                    boolean hasDescriptions) {

        var label = field.label();
        var component = field.component();
        var previousComponent = (previousField == null) ? null : previousField.component();
        var previousButton = (previousComponent == null || previousComponent.getClientProperty(PROPERTY_INFO_COMPONENT) == null) ?
                null : (Component) previousComponent.getClientProperty(PROPERTY_INFO_COMPONENT);

        if (component instanceof JTextField) {
//                 Do not resize TextFields vertically
            Dimension dimension = component.getPreferredSize();
            component.setMaximumSize(Dimensions.withWidth(dimension, Integer.MAX_VALUE));
        }

        if (StringUtils.isEmpty(component.getName())) {
            component.setName("Field(" + label.getText() + ")");
        }

        organizer.add(label).atSameTopAs(component);
//        organizer.add(label).withSameMiddleAs(component);


        if (false && hasDescriptions) {
            var description = field.description();

            JComponent infoButton = new InfoMarker(description);

            component.putClientProperty(PROPERTY_INFO_COMPONENT, infoButton);
            infoButton.setName("Info(" + label.getText() + ")");

            if (description == null) {
                infoButton.setVisible(false);
            }

            component.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        organizer.dumpConstraints(component);
                    }
                }
            });

            organizer.add(infoButton)
                    .notStretching()
                    .atRightSide()
                    .when(previousComponent == null, SpringLayoutOrganizer.Position::atTop)
                    .orElse(position -> position.below(previousButton))
                    .when(isLastField, SpringLayoutOrganizer.Position::definingContainerHeight);

            organizer.add(component)
                    .rightOf(label)
                    .leftOf(infoButton)
                    .withSameMiddleAs(infoButton)
                    .when(isLastField, SpringLayoutOrganizer.Position::definingContainerHeight);

            int width = Dimensions.sumWidth(Component::getPreferredSize, label, component, infoButton) + 4 * PAD;
            int height = Dimensions.maxHeight(Component::getPreferredSize, label, component, infoButton) + PAD;
            return new Dimension(width, height);
        } else {
            organizer.add(component)
                    .rightOf(label)
                    .stretchToRightSide()
                    .when(previousComponent == null, SpringLayoutOrganizer.Position::atTop)
                    .orElse(position -> position.below(previousComponent))
                    .when(isLastField, SpringLayoutOrganizer.Position::definingContainerHeight)
            ;

            int width = Dimensions.sumWidth(Component::getPreferredSize, label, component) + 3 * PAD;
            int height = Dimensions.maxHeight(Component::getPreferredSize, label, component) + PAD;
            return new Dimension(width, height);
        }
    }

    private FormFieldListener formFieldListener() {
        return new FormFieldListener() {
            @Override
            public void onChanged(FormField<?, ?> field) {
                fireFormEvent(listener -> listener.onChanged(self));
            }

            @Override
            public void focusGained(FormField<?, ?> field, FocusEvent event) {
                fireFormEvent(listener -> listener.focusGained(self, field, event));
            }

            @Override
            public void focusLost(FormField<?, ?> field, FocusEvent event) {
                fireFormEvent(listener -> listener.focusLost(self, field, event));
            }
        };
    }
}
