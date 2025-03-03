package com.quaxantis.etui.swing.tagentry;

import com.quaxantis.etui.*;
import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.application.config.Configuration;
import com.quaxantis.etui.swing.tagentry.TagTree.TagSelectionListener.Status;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.support.swing.ToolTips;
import com.quaxantis.support.swing.util.QuickJFrame;

import javax.swing.*;
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
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

class TagTree extends JTree {


    public TagTree(Map<String, List<TagFamily>> groups) {
        this(groups, true);
    }

    private TagTree(Map<String, List<TagFamily>> groups, boolean removeReadOnlyTags) {
        super(new Model(groups, removeReadOnlyTags));
        setRootVisible(false);
        setShowsRootHandles(true);
        setCellRenderer(new Renderer());
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        ToolTipManager.sharedInstance().registerComponent(this);

        addTreeSelectionListener(e -> {
            fireTagSelection(Status.SELECTED, e.getPath());
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    fireTagSelection(Status.ACTIVATED, getPathForLocation(e.getX(), e.getY()));
                }
            }
        });

        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "activate");
        getActionMap().put("activate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireTagSelection(Status.ACTIVATED, getSelectionPath());
            }
        });
    }

    void selectTag(Tag tag) {
        getModel().findSelectionPath(tag)
                .ifPresent(this::setSelectionPath);
    }

    void addTagSelectionListener(TagSelectionListener listener) {
        this.listenerList.add(TagSelectionListener.class, listener);
    }

    private void fireTagSelection(Status status, TreePath path) {
        if (path != null) {
            fireTagSelection(status, path.getLastPathComponent());
        }
    }

    void fireTagSelection(Status status, Object node) {
        for (TagSelectionListener listener : this.listenerList.getListeners(TagSelectionListener.class)) {
            listener.onTagSelected(status, node);
        }
    }

    int getPreferredExpansion() {
        return calculateIndent();
    }

    private int calculateIndent() {
        TreeModel model = getModel();
        if (model.getChildCount(model.getRoot()) > 0) {
            Object first = model.getChild(model.getRoot(), 0);
            Component component = getCellRenderer().getTreeCellRendererComponent(this, first, false, false, false, 0, false);
            if (component instanceof JLabel label) {
                return label.getIconTextGap()
                       + ((label.getIcon() == null) ? 0 : label.getIcon().getIconWidth());
            }
        }
        return 16;
    }

    @Override
    public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof HasLabel hasLabel && hasLabel.label() != null) {
            return hasLabel.label();
        } else if (value instanceof Tag tag) {
            return tag.tagName();
        } else {
            return super.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
        }
    }

    @Override
    public Model getModel() {
        return (Model) super.getModel();
    }

    public Optional<Tag> findTag(Tag tag) {
        return this.getModel().findTag(tag);
    }

    private static class Renderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            var component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (component instanceof JComponent label) {
                if (value instanceof HasDescription hasDescription && hasDescription.description() != null) {
                    ToolTips.setToolTipAsHtml(label, hasDescription.description());
                }
            }

            return component;
        }
    }

    private static class Model implements TreeModel {
        private final List<String> groupNames;
        private final SequencedMap<String, List<TagFamily>> groups;

        private Model(Map<String, List<TagFamily>> groups, boolean removeReadOnlyTags) {
            this.groups = groups.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> {
                                List<TagFamily> families = entry.getValue();
                                return filterFamilies(families, removeReadOnlyTags);
                            },
                            (l, r) -> {
                                l.addAll(r);
                                return l;
                            },
                            LinkedHashMap::new));
            this.groupNames = this.groups.sequencedKeySet().stream().toList();
        }

        private List<TagFamily> filterFamilies(List<TagFamily> families, boolean removeReadOnlyTags) {
            if (! removeReadOnlyTags) {
                return families;
            }

            return families.stream()
                    .map(this::removeReadOnlyTags)
                    .filter(not(family -> family.tags().isEmpty()))
                    .toList();
        }

        private TagFamily removeReadOnlyTags(TagFamily tagFamily) {
            var writableTags = tagFamily.tags().stream().filter(not(TagDescriptor::isReadOnly)).toList();
            return TagFamily.of(tagFamily.label(), writableTags);
        }

        private List<?> getChildren(Object parent) {
            if (parent == this) {
                return this.groupNames;
            } else if (parent instanceof String group) {
                return groups.get(group);
            } else if (parent instanceof TagFamily collection) {
                return collection.tags();
            } else {
                return null;
            }
        }

        @Override
        public Object getRoot() {
            return this;
        }

        @Override
        public int getChildCount(Object parent) {
            if (getChildren(parent) instanceof List<?> list) {
                return list.size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (getChildren(parent) instanceof List<?> list) {
                return list.get(index);
            } else {
                return null;
            }
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (getChildren(parent) instanceof List<?> list) {
                return list.indexOf(child);
            } else {
                return -1;
            }
        }

        public Optional<Tag> findTag(Tag tag) {
            return this.groups.values().stream()
                    .flatMap(List::stream)
                    .<Tag>flatMap(family -> family.tags().stream())
                    .filter(tag::isSameTagAs)
                    .findFirst();
        }

        public Optional<TreePath> findSelectionPath(Tag tag) {
            for (String group : this.groupNames) {
                for (TagFamily family : this.groups.get(group)) {
                    for (Tag modelTag : family.tags()) {
                        if (tag.isSameTagAs(modelTag)) {
                            return Optional.of(new TreePath(new Object[]{this, group, family, modelTag}));
                        }
                    }
                }
            }
            return Optional.empty();
        }

        @Override
        public boolean isLeaf(Object node) {
            return node instanceof Tag;
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

    public interface TagSelectionListener extends EventListener {
        void onTagSelected(Status status, Object node);

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
        var repo = new TagRepository(configuration);
        System.out.println(repo.getFamilies().stream().map(TagFamily::label).toList());

        QuickJFrame.of(new TagTree(repo.getGroupedFamilies(), false))
                .withTitle("Tag Tree")
                .show();
    }
}
