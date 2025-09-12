package com.quaxantis.etui.swing.tagentry;

import com.quaxantis.etui.*;
import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.application.config.Configuration;
import com.quaxantis.etui.swing.tagentry.TagTree.TagSelectionListener.Status;
import com.quaxantis.etui.tag.TagGroup;
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
import java.util.EventListener;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

class TagTree extends JTree {


    public TagTree(SequencedCollection<TagGroup> tagGroups) {
        this(tagGroups, true);
    }

    private TagTree(SequencedCollection<TagGroup> tagGroups, boolean removeReadOnlyTags) {
        super(new Model(tagGroups, removeReadOnlyTags));
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
        } else if (value instanceof TagFamily tagFamily) {
            return tagFamily.label();
        } else if (value instanceof TagGroup tagGroup) {
            return tagGroup.name();
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
        private final TagGroup root;

        private Model(SequencedCollection<TagGroup> tagGroups, boolean removeReadOnlyTags) {
            this.root = new TagGroup("", tagGroups.stream().toList(), List.of());
        }

        private List<TagFamily> filterFamilies(List<TagFamily> families, boolean removeReadOnlyTags) {
            if (!removeReadOnlyTags) {
                return families;
            }

            return families.stream()
                    .map(this::removeReadOnlyTags)
                    .filter(not(family -> family.tags().isEmpty()))
                    .toList();
        }

        private TagFamily removeReadOnlyTags(TagFamily tagFamily) {
            return tagFamily.filter(not(TagDescriptor::isReadOnly));
        }

        @Override
        public Object getRoot() {
            return root;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent instanceof TagGroup group) {
                return group.subGroups().size() + group.families().size();
            } else if (parent instanceof TagFamily family) {
                return family.tags().size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent instanceof TagGroup group) {
                if (index < group.subGroups().size()) {
                    return group.subGroups().get(index);
                } else {
                    return group.families().get(index - group.subGroups().size());
                }
            } else if (parent instanceof TagFamily family) {
                return family.tags().get(index);
            } else {
                return null;
            }
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof TagGroup group && child instanceof TagGroup subGroup) {
                return group.subGroups().indexOf(subGroup);
            } else if (parent instanceof TagGroup group && child instanceof TagFamily family) {
                return group.subGroups().size() + group.families().indexOf(family);
            } else if (parent instanceof TagFamily family) {
                return family.tags().indexOf(child);
            } else {
                return -1;
            }
        }

        public Optional<Tag> findTag(Tag tag) {
            return tags(this.root)
                    .filter(tag::isSameTagAs)
                    .findFirst();
        }

        private Stream<Tag> tags(TagGroup group) {
            return Stream.concat(group.families().stream().map(TagFamily::tags).flatMap(List::stream),
                                 group.subGroups().stream().flatMap(this::tags));
        }

        public Optional<TreePath> findSelectionPath(Tag tag) {
            return findSelectionPath(tag, this.root).map(NodePath::toTreePath);
        }

        public Optional<NodePath> findSelectionPath(Tag tag, TagGroup group) {
            for (TagFamily family : group.families()) {
                Optional<NodePath> path = findSelectionPath(tag, family);
                if (path.isPresent()) {
                    return path.map(p -> p.withParent(group));
                }
            }
            for (TagGroup subGroup : group.subGroups()) {
                Optional<NodePath> path = findSelectionPath(tag, subGroup);
                if (path.isPresent()) {
                    return path.map(p -> p.withParent(group));
                }
            }
            return Optional.empty();
        }

        public Optional<NodePath> findSelectionPath(Tag tag, TagFamily family) {
            for (Tag modelTag : family.tags()) {
                if (tag.isSameTagAs(modelTag)) {
                    return Optional.of(new NodePath(modelTag, null).withParent(family));
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

        private record NodePath(Object node, NodePath childPath) {

            NodePath withParent(Object parent) {
                return new NodePath(parent, this);
            }

            TreePath toTreePath() {
                TreePath thisPath = new TreePath(this.node);
                return (this.childPath == null) ? thisPath : this.childPath.addTo(thisPath);
            }

            private TreePath addTo(TreePath path) {
                TreePath thisPath = path.pathByAddingChild(this.node);
                return (this.childPath == null) ? thisPath : this.childPath.addTo(thisPath);
            }
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

    public static void main(String[] args) {
        Configuration configuration = new ConfigOperations().getConfiguration();
        var repo = new TagRepository(configuration);
        System.out.println(repo.getFamilies().stream().map(TagFamily::label).toList());

        QuickJFrame.of(new TagTree(repo.tagGroups(), false))
                .withTitle("Tag Tree")
                .show();
    }
}
