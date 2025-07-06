package com.quaxantis.etui.template.parser;

import com.quaxantis.support.swing.Dimensions;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Dimension;
import java.util.*;
import java.util.function.Function;

public class GenericTreeModel implements TreeModel {
    private final Config config;
    private final Object root;

    public static Builder build() {
        return new Builder()
                .with(List.class, list -> list)
                .with(Object[].class, Arrays::deepToString, Arrays::asList)
                .with(Map.class, map -> new ArrayList<>(map.entrySet()))
                .withDelegate(Map.Entry.class, entry -> String.valueOf(entry.getKey()), Map.Entry::getValue)
                ;
    }

    private GenericTreeModel(Config config, Object root) {
        this.config = config;
        this.root = root;
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return parent instanceof Node node ? node.getChild(index) : null;
    }

    @Override
    public int getChildCount(Object parent) {
        return parent instanceof Node node ? node.getChildCount() : 0;
    }

    @Override
    public boolean isLeaf(Object object) {
        return object instanceof Node node ? node.isLeaf() : true;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return parent instanceof Node node ? node.getIndexOfChild(child) : -1;
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {

    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {

    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {

    }

    private JComponent createUI() {
        JTree tree = new JTree(this) {
            @Override
            public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof String string) {
                    return string;
                } else {
                    return config.toNodeString(value);
                }
            }
        };
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tree.setBackground(Color.DARK_GRAY);
        tree.setCellRenderer(new DefaultTreeCellRenderer() {{
            setBackgroundNonSelectionColor(Color.DARK_GRAY);
            setTextNonSelectionColor(new Color(192, 192, 192));
            setBackgroundSelectionColor(new Color(32, 32, 64));
            setTextSelectionColor(new Color(192, 192, 192));
            setBorderSelectionColor(new Color(64, 64, 128));
        }});
        return new JScrollPane(tree);
    }

    private static void showWindow(JComponent ui) {

        JFrame frame = new JFrame("Tree model");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(ui);
        var screen = frame.getGraphicsConfiguration().getDevice().getDisplayMode();
        Dimension preferredSize = Dimensions.withMaximumWidthAndHeight(Dimensions.withMinimumWidthAndHeight(ui.getPreferredSize(), 800, 250), screen.getWidth(), screen.getHeight());
        frame.getContentPane().setPreferredSize(preferredSize);

        frame.pack();
        if (preferredSize.width == screen.getWidth() || preferredSize.height == screen.getHeight()) {
            frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }
        frame.setVisible(true);


        try {
            while (frame.isVisible()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }


    interface Node {

        boolean isLeaf();

        int getChildCount();

        Object getChild(int index);

        int getIndexOfChild(Object child);

    }

    private static class ListNode implements Node {
        private final List<?> children;
        private final String toString;
        private final Object source;

        ListNode(Object source, String toString, List<?> children) {
            this.source = source;
            this.children = children;
            this.toString = toString;
        }

        @Override
        public Object getChild(int index) {
            return children.get(index);
        }

        @Override
        public boolean isLeaf() {
            return getChildCount() == 0;
        }

        @Override
        public int getChildCount() {
            return children.size();
        }

        @Override
        public int getIndexOfChild(Object child) {
            return children.indexOf(child);
        }

        @Override
        public String toString() {
            return toString;
        }
    }

    private static class Config {
        private final ClassMap<Function<?, Node>> typeWrappers = new ClassMap<>();
        private final ClassMap<Function<?, String>> toStringFunctions = new ClassMap<>();
        private final ClassMap<Function<?, List<?>>> toListFunctions = new ClassMap<>();

        <T> Object wrap(T t) {
            if (t instanceof Node) {
                return t;
            }
            Function<T, Node> wrapper = typeWrapperOf(t);
            return (wrapper == null) ? t : wrapper.apply(t);
        }

        @SuppressWarnings("unchecked")
        <T> Function<T, Node> typeWrapperOf(T t) {
            return (Function<T, Node>) typeWrappers.getForInstance(t).orElse(null);
        }

        <T> List<?> toList(T t) {
            if (t == null) {
                return Collections.singletonList(null);
            }
            var toListFunction = toListFunctionOf(t);
            return toListFunction == null ? List.of(t) : toListFunction.apply(t);
        }


        private <T> List<?> toWrappedList(List<T> children) {
            if ((!children.isEmpty()) && typeWrapperOf(children.getFirst()) instanceof Function<T, Node> typeWrapper) {
                return children.stream().map(typeWrapper).toList();
            } else {
                return children;
            }
        }

        @SuppressWarnings("unchecked")
        <T> Function<T, List<?>> toListFunctionOf(T t) {
            return (Function<T, List<?>>) toListFunctions.getForInstance(t).orElse(null);
        }

        @SuppressWarnings("unchecked")
        <T> String toNodeString(T t) {
            return toStringFunctions.getForInstance(t).map(toString -> ((Function<T, String>) toString).apply(t)).orElseGet(() -> String.valueOf(t));
        }
    }

    private static class ClassMap<V> {
        private final Map<Class<?>, V> map = new HashMap<>();

        private Optional<V> getForInstance(Object o) {
            return Optional.ofNullable(o).map(Object::getClass).flatMap(this::get);
        }

        private Optional<V> get(Class<?> clazz) {
            return Optional.ofNullable(map.get(clazz))
                    .or(() -> map.keySet().stream().filter(superClass -> superClass.isAssignableFrom(clazz)).findAny().map(map::get));
        }

        private void put(Class<?> clazz, V value) {
            map.put(clazz, value);
        }
    }

    public static final class Builder {
        private final Config config = new Config();

        public <P> Builder withToString(Class<P> type, Function<P, String> toString) {
            config.toStringFunctions.put(type, toString);
            return this;
        }

        public <P, C> Builder with(Class<P> type, Function<P, List<C>> toListFunction) {
            config.toListFunctions.put(type, named("toList(" + type + ")", (P parent) ->
                    config.toWrappedList(toListFunction.apply(parent))));
            config.typeWrappers.put(type, named("typeWrapper(" + type + ")", (P parent) ->
                    new ListNode(parent, config.toNodeString(parent), config.toWrappedList(toListFunction.apply(parent)))));
            return this;
        }

        public <P, C> Builder with(Class<P> type, Function<P, String> toString, Function<P, List<C>> toListFunction) {
            return withToString(type, toString)
                    .with(type, toListFunction);
        }

        public <P, C> Builder withDelegate(Class<P> type, Function<P, C> toChildFunction) {
            config.typeWrappers.put(type, (P parent) ->
                    new ListNode(parent, config.toNodeString(parent), config.toWrappedList(config.toList(toChildFunction.apply(parent)))));
            return this;
        }

        public <P, C> Builder withDelegate(Class<P> type, Function<P, String> toString, Function<P, C> toChildFunction) {
            return withToString(type, toString)
                    .withDelegate(type, toChildFunction);
        }

        public void show(Object root) {
            GenericTreeModel model = new GenericTreeModel(this.config, this.config.wrap(root));
            showWindow(model.createUI());
        }

        public void show(Object rootLeft, Object rootRight) {
            GenericTreeModel modelLeft = new GenericTreeModel(this.config, this.config.wrap(rootLeft));
            GenericTreeModel modelRight = new GenericTreeModel(this.config, this.config.wrap(rootRight));

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, modelLeft.createUI(), modelRight.createUI());
            splitPane.setOneTouchExpandable(true);
            splitPane.setResizeWeight(0.5);
            showWindow(splitPane);
        }
    }


    private static <T, R> Function<T, R> named(String name, Function<T, R> function) {
        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                return function.apply(t);
            }

            @Override
            public String toString() {
                return super.toString() + " [" + name + "]";
            }
        };
    }

    public static void main(String[] args) {
//        new GenericTreeModel().build().show(null);
//        new GenericTreeModel().build().show("abc");
//        GenericTreeModel.build()
//                .show(List.of("One", "Two", "Three" ));
//        GenericTreeModel.build()
//                .show(new String[]{"One", "Two", "Three"});
        GenericTreeModel.build()
                .show(Map.of("One", List.of(1), "<b><i>Two</i></b>", List.of(1, 2), "Three", List.of(1, 2, 3)));

    }
}
