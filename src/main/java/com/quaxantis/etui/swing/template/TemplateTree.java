package com.quaxantis.etui.swing.template;

import com.quaxantis.etui.HasLabel;
import com.quaxantis.etui.Template;
import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.application.config.Configuration;
import com.quaxantis.etui.swing.template.TemplateTree.TemplateSelectionListener.Status;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.etui.template.TemplateRepository;
import com.quaxantis.support.swing.util.QuickJFrame;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

public class TemplateTree extends JTree {

    public TemplateTree(SequencedCollection<TemplateGroup> templateGroups) {
        super(new Model(templateGroups));
        setRootVisible(false);
        setShowsRootHandles(true);
        setCellRenderer(new Renderer());
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        addTreeSelectionListener(e -> {
            fireTemplateSelection(Status.SELECTED, e.getPath());
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    fireTemplateSelection(Status.ACTIVATED, getPathForLocation(e.getX(), e.getY()));
                }
            }
        });

        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "activate");
        getActionMap().put("activate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireTemplateSelection(Status.ACTIVATED, getSelectionPath());
            }
        });
    }

    private Model getModelInternal() {
        return (Model) getModel();
    }

    String[] getExternalizedPath() {
        return getModelInternal().toExternalizedPath(getSelectionPath());
    }

    void setExternalizedPath(String[] path) {
        setSelectionPath(getModelInternal().fromExternalizedPath(path));
    }

    void addTemplateSelectionListener(TemplateSelectionListener listener) {
        this.listenerList.add(TemplateSelectionListener.class, listener);
    }

    private void fireTemplateSelection(Status status, TreePath path) {
        if (path != null && path.getLastPathComponent() instanceof Template template) {
            fireTemplateSelection(status, template);
        }
    }

    void fireTemplateSelection(Status status, Template template) {
        for (TemplateSelectionListener listener : this.listenerList.getListeners(TemplateSelectionListener.class)) {
            listener.onTemplateSelected(status, template);
        }
    }

    private static class Renderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            var component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (component instanceof JLabel label)
                if (value instanceof Template template) {
                    label.setText(HasLabel.labelOf(template, template::name));
                } else if (value instanceof TemplateGroup group) {
                    label.setText(HasLabel.labelOf(group, group::name));
                }
            return component;
        }
    }

    private static class Model implements TreeModel {
        private final TemplateGroup root;

        private Model(SequencedCollection<TemplateGroup> templateGroups) {
            this.root = new TemplateGroup("", templateGroups.stream().toList(), List.of());
        }

        @Override
        public Object getRoot() {
            return root;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent instanceof TemplateGroup group) {
                return group.subGroups().size() + group.templates().size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent instanceof TemplateGroup group) {
                if (index < group.subGroups().size()) {
                    return group.subGroups().get(index);
                } else {
                    return group.templates().get(index - group.subGroups().size());
                }
            } else {
                return null;
            }
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof TemplateGroup group && child instanceof TemplateGroup subGroup) {
                return group.subGroups().indexOf(subGroup);
            } else if (parent instanceof TemplateGroup group && child instanceof Template template) {
                return group.subGroups().size() + group.templates().indexOf(template);
            } else {
                return -1;
            }
        }


        String[] toExternalizedPath(TreePath path) {
            return Arrays.stream(path.getPath())
                    .filter(node -> node != this.root)
                    .map(node -> {
                        if (node instanceof TemplateGroup group) {
                            return group.name();
                        } else if (node instanceof Template template) {
                            return template.name();
                        } else {
                            return "?";
                        }
                    }).toArray(String[]::new);
        }

        TreePath fromExternalizedPath(String[] externalizedPath) {
            TemplateGroup group = this.root;
            TreePath path = new TreePath(group);

            if (externalizedPath == null) {
                return path;
            }

            for (int i = 0; i < externalizedPath.length; i++) {
                String name = externalizedPath[i];
                boolean isLast = i == externalizedPath.length - 1;
                Optional<TemplateGroup> subGroup = group.subGroups().stream().filter(g -> name.equals(g.name())).findFirst();
                Optional<Template> template = group.templates().stream().filter(t -> name.equals(t.name())).findFirst();

                if (template.isPresent()) {
                    // Prefer subGroup if present, unless it's the last node
                    if (isLast || subGroup.isEmpty()) {
                        // Template leaf node found, returning
                        return path.pathByAddingChild(template.get());
                    }
                } else if (subGroup.isPresent()) {
                    // Group node found, continue with next node
                    group = subGroup.get();
                    path = path.pathByAddingChild(group);
                } else {
                    // Child not found, returning current path
                    return path;
                }
            }
            return path;
        }

        @Override
        public boolean isLeaf(Object node) {
            return node instanceof Template;
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
            // Tree is immutable
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
            // Tree is immutable
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            throw new UnsupportedOperationException();
        }
    }

    public interface TemplateSelectionListener extends EventListener {
        void onTemplateSelected(Status status, Template template);

        enum Status {
            SELECTED,
            ACTIVATED;

            boolean isActivated() {
                return this == ACTIVATED;
            }
        }
    }


    public static void main() {
        Configuration configuration = new ConfigOperations().getConfiguration();
        TemplateRepository repo = new TemplateRepository(
                configuration,
                new TagRepository(configuration)
        );


        QuickJFrame.of(new TemplateTree(repo.templateGroups()))
                .withTitle("Template Tree")
                .show();
    }
}
