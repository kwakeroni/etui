package com.quaxantis.support.swing.layout;

import javax.swing.Spring;
import javax.swing.SpringLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.function.Consumer;

import static javax.swing.SpringLayout.*;

public class SpringLayoutOrganizer {
    private final SpringLayout layout;
    private final Container container;
    private int defaultPadding = 0;

    public static SpringLayoutOrganizer organize(Container container) {
        SpringLayout springLayout = new SpringLayout();
        container.setLayout(springLayout);
        return new SpringLayoutOrganizer(springLayout, container);
    }

    private SpringLayoutOrganizer(SpringLayout layout, Container container) {
        this.layout = layout;
        this.container = container;
    }

    public SpringLayoutOrganizer setDefaultPadding(int padding) {
        this.defaultPadding = padding;
        return this;
    }

    public SpringLayout layout() {
        return this.layout;
    }

    public Container container() {
        return this.container;
    }

    public Position add(Component component) {
        this.container.add(component);
        return new Position(component);
    }

    public Position position(Component component) {
        return new Position(component);
    }

    public class Position {
        private final Component component;

        private Position(Component component) {
            this.component = component;
        }

        public SpringLayoutOrganizer and() {
            return SpringLayoutOrganizer.this;
        }

        public Position notStretching() {
            setNotStretching(this.component);
            return this;
        }

        public Position notStretchingHorizontally() {
            setNotStretchingHorizontally(this.component);
            return this;
        }

        public Position notStretchingVertically() {
            setNotStretchingVertically(this.component);
            return this;
        }


        public Position atLeftSide() {
            return atLeftSide(defaultPadding);
        }

        public Position atLeftSide(int padding) {
            layout.putConstraint(WEST, component, padding, WEST, container);
            return this;
        }

        public Position atTop() {
            return atTop(defaultPadding);
        }

        public Position atTop(int padding) {
            layout.putConstraint(NORTH, component, padding, NORTH, container);
            return this;
        }

        public Position atRightSide() {
            return atRightSide(defaultPadding);
        }

        public Position atRightSide(int padding) {
            layout.putConstraint(EAST, component, -padding, EAST, container);
            return this;
        }

        public Position stretchToRightSide() {
            return atRightSide();
        }

        public Position stretchToRightSide(int padding) {
            return atRightSide(padding);
        }

        public Position atBottom() {
            return atBottom(defaultPadding);
        }

        public Position atBottom(int padding) {
            layout.putConstraint(SOUTH, component, -padding, SOUTH, container);
            return this;
        }

        public Position stretchToBottom() {
            return atBottom();
        }

        public Position stretchToBottom(int padding) {
            return atBottom(padding);
        }

        public Position definingContainerWidth() {
            return definingContainerWidth(defaultPadding);
        }

        public Position definingContainerWidth(int padding) {
            layout.putConstraint(EAST, container, padding, EAST, component);
            return this;
        }

        public Position definingContainerHeight() {
            return definingContainerHeight(defaultPadding);
        }

        public Position definingContainerHeight(int padding) {
            layout.putConstraint(SOUTH, container, padding, SOUTH, component);
            return this;
        }

        public Position leftOf(Component other) {
            return leftOf(other, defaultPadding);
        }

        public Position leftOf(Component other, int padding) {
            layout.putConstraint(EAST, component, -padding, WEST, other);
            return this;
        }

        public Position rightOf(Component other) {
            return rightOf(other, defaultPadding);
        }

        public Position rightOf(Component other, int padding) {
            layout.putConstraint(WEST, component, padding, EAST, other);
            return this;
        }

        public Position above(Component other) {
            return above(other, defaultPadding);
        }

        public Position above(Component other, int padding) {
            layout.putConstraint(SOUTH, component, -padding, NORTH, other);
            return this;
        }

        public Position below(Component other) {
            return below(other, defaultPadding);
        }

        public Position below(Component other, int padding) {
            layout.putConstraint(NORTH, component, padding, SOUTH, other);
            return this;
        }

        public Position atSameTopAs(Component other) {
            layout.putConstraint(NORTH, component, 0, NORTH, other);
            return this;
        }

        public Position atBottomTopAs(Component other) {
            layout.putConstraint(SOUTH, component, 0, SOUTH, other);
            return this;
        }

        public Position withSameMiddleAs(Component other) {
            layout.putConstraint(VERTICAL_CENTER, component, 0, VERTICAL_CENTER, other);
            return this;
        }

        public Position atSameLeftEdgeAs(Component other) {
            layout.putConstraint(WEST, component, 0, WEST, other);
            return this;
        }

        public Position atSameRightEdgeAs(Component other) {
            layout.putConstraint(EAST, component, 0, EAST, other);
            return this;
        }

        public Position withSameCenterAs(Component other) {
            layout.putConstraint(HORIZONTAL_CENTER, component, 0, HORIZONTAL_CENTER, other);
            return this;
        }

        public When when(boolean condition, Consumer<Position> action) {
            return When.of(this, condition, action);
        }
    }

    public void dumpConstraints(Component component) {
        var constraints = layout.getConstraints(component);
        System.out.println("--- Constraints of " + component);
        System.out.printf("[%5s] %s%n", "size", component.getSize());
        System.out.printf("[%5s] %s%n", " -min", component.getMinimumSize());
        System.out.printf("[%5s] %s%n", " -prf", component.getPreferredSize());
        System.out.printf("[%5s] %s%n", " -max", component.getMaximumSize());
        System.out.printf("[%5s] %s%n", WEST, toString(constraints.getConstraint(WEST)));
        System.out.printf("[%5s] %s%n", EAST, toString(constraints.getConstraint(EAST)));
        System.out.printf("[%5s] %s%n", "x", toString(constraints.getX()));
        System.out.printf("[%5s] %s%n", "width", toString(constraints.getWidth()));
        System.out.printf("[%5s] %s%n", NORTH, toString(constraints.getConstraint(NORTH)));
        System.out.printf("[%5s] %s%n", SOUTH, toString(constraints.getConstraint(SOUTH)));
        System.out.printf("[%5s] %s%n", "y", toString(constraints.getY()));
        System.out.printf("[%5s] %s%n", "height", toString(constraints.getHeight()));
        System.out.println();
    }

    private String toString(Spring spring) {
        return switch (spring) {
            default -> spring.toString();
        };
    }

    public static abstract class When {
        private When() {

        }

        public abstract When orWhen(boolean condition, Consumer<Position> action);
        public abstract Position orElse(Consumer<Position> action);

        private static When of(Position position, boolean condition, Consumer<Position> action) {
            if (condition) {
                action.accept(position);
                return new When() {
                    @Override
                    public When orWhen(boolean condition, Consumer<Position> action) {
                        return this;
                    }

                    @Override
                    public Position orElse(Consumer<Position> action) {
                        return position;
                    }
                };
            } else {
                return new When() {
                    @Override
                    public When orWhen(boolean condition, Consumer<Position> action) {
                        return When.of(position, condition, action);
                    }

                    @Override
                    public Position orElse(Consumer<Position> action) {
                        action.accept(position);
                        return position;
                    }
                };
            }
        }
    }


    public static void setNotStretchingVertically(Component c) {
        var preferredSize = c.getPreferredSize();
        var maxSize = c.getMaximumSize();
        int maxWidth = (maxSize != null)? maxSize.width : Integer.MAX_VALUE;
        Dimension maximumSize = new Dimension(maxWidth, preferredSize.height);
        c.setMaximumSize(maximumSize);
    }

    public static void setNotStretchingHorizontally(Component c) {
        var preferredSize = c.getPreferredSize();
        var maxSize = c.getMaximumSize();
        int maxHeight = (maxSize != null)? maxSize.height : Integer.MAX_VALUE;
        c.setMaximumSize(new Dimension(preferredSize.width, maxHeight));
    }

    public static void setNotStretching(Component c) {
        c.setMaximumSize(c.getPreferredSize());
    }

}
