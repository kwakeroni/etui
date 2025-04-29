package com.quaxantis.etui.swing.tagentry;

import com.quaxantis.etui.Tag;
import com.quaxantis.etui.TagValue;
import com.quaxantis.etui.application.config.Configuration;
import com.quaxantis.etui.application.config.ConfigurationListener;
import com.quaxantis.etui.application.config.ViewFilter;
import com.quaxantis.etui.swing.taginfo.TagInfoPanel;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.support.swing.Dimensions;
import com.quaxantis.support.swing.ToolTips;
import com.quaxantis.support.swing.form.FormField;
import com.quaxantis.support.swing.form.FormListener;
import com.quaxantis.support.swing.layout.SpringLayoutOrganizer;
import com.quaxantis.support.swing.form.SimpleForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

public class TagEntryUI {

    public static void main(String[] ignoredArgs) {
        Configuration config = new Configuration() {
            @Override
            public Optional<Path> getExiftoolExecutable() {
                return Optional.empty();
            }

            @Override
            public ViewFilter getViewFilter() {
                return null;
            }

            @Override
            public Optional<String[]> getLastSelectedTemplate() {
                return Optional.empty();
            }

            @Override
            public List<Path> getHistory() {
                return List.of();
            }

            @Override
            public List<Path> getTagDefinitions() {
                return List.of();
            }

            @Override
            public List<String> getTemplatePaths() {
                return List.of();
            }

            @Override
            public void addConfigurationListener(ConfigurationListener listener) {
            }
        };

        ToolTips.setToolTipBackground(new Color(255, 250, 227));

        TagValue tagValue = TagValue.of("XMP", "Date", "2024-09-06");

        JFrame jFrame = new JFrame("TagEntryUI");
        jFrame.setVisible(true);
        openDialog(jFrame, new TagRepository(config), "Test", "Test", true, tagValue)
                .ifPresent(System.out::println);
        jFrame.dispose();
    }

    public static Optional<TagValue> openDialog(Frame owner, TagRepository tagRepository, String title, String action, boolean modal) {
        return openDialog(owner, tagRepository, title, action, modal, null);
    }

    public static Optional<TagValue> openDialog(Frame owner, TagRepository tagRepository, String title, String action, boolean modal, TagValue tagValue) {
        JDialog dialog = new JDialog(owner, title, modal);
        AtomicBoolean actionPerformed = new AtomicBoolean(false);
        TagEntryUI ui = new TagEntryUI(action, tagRepository, tagValue, _ -> {
            actionPerformed.set(true);
            dialog.dispose();
        });

        dialog.setContentPane(ui.getUIContainer());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
        if (actionPerformed.get()) {
            return Optional.of(ui.getTag());
        } else {
            return Optional.empty();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TagEntryUI.class);
    private static final int PAD = 10;

    private final TagRepository tagRepository;
    private final SimpleForm<TagValueBean> form;
    private final Container uiContainer;

    public TagEntryUI(String action, TagRepository tagRepository, TagValue tagValue, ActionListener actionlistener) {
        this.tagRepository = tagRepository;
        var tagTree = createTagTree(tagRepository);
        this.form = createForm(tagTree);
        var actionBar = createActionBar(this.form, action, actionlistener);
        var tagEntryPanel = createTagEntryPanel(this.form, actionBar);
        this.uiContainer = composeUI(tagTree, tagEntryPanel, this.form);
        if (tagValue != null) {
            tagTree.selectTag(tagValue.tag());
            setTag(tagValue);
        }
    }

    public Container getUIContainer() {
        return this.uiContainer;
    }

    public TagValue getTag() {
        TagValueBean bean = this.form.getData();
        Tag tag = this.tagRepository.enrichTag(bean.tag());
        return TagValue.of(tag, bean.value());
    }

    private void setTag(TagValue tagValue) {
        this.form.setData(new TagValueBean(tagValue));
    }

    private static Container composeUI(TagTree tagTree, Container tagEntryPanel, SimpleForm<TagValueBean> form) {
        var listPanel = createTagListPanel(tagTree);
        var infoPanel = createTagInfoPanel(tagTree);
        infoPanel.setPreferredSize(new Dimension(200, 120));
        var tagSelectionPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                              listPanel,
                                              infoPanel);

        var formatPanel = createTagFormatPanel(tagTree, form);
        formatPanel.setPreferredSize(new Dimension(200, 120));
        var rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tagEntryPanel, formatPanel);

        var splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tagSelectionPane, rightPane);
        splitPane.setOneTouchExpandable(true);
        return splitPane;
    }

    private static Container createTagFormatPanel(TagTree tagTree, SimpleForm<TagValueBean> form) {
        TagInfoPanel pane = new TagInfoPanel(
                TagInfoPanel.StandardInfoItem.FORMAT_DESCRIPTION,
                TagInfoPanel.StandardInfoItem.FORMAT_EXAMPLES.wrapped("<p><strong>examples</strong>", "</p>")
        );
        pane.setExamplePatternAction(pattern -> form.modifyData(bean -> bean.setValue(bean.value() + pattern)));
        pane.setHyperlinkAction(TagInfoPanel::openWithDesktop);
        form.addFormListener(FormListener.onChanged(f -> tagTree.findTag(f.getData().tag()).ifPresent(pane::setTag)));
        return new JScrollPane(pane);
    }

    private static SimpleForm<TagValueBean> createForm(TagTree tagTree) {
        var form = new SimpleForm<>(TagValueBean::new,
                                    FormField.ofTextField("Group", TagValueBean::groupName, TagValueBean::setGroup),
                                    FormField.ofTextField("Tag", TagValueBean::tagName, TagValueBean::setTag),
                                    FormField.ofTextArea("Value", TagValueBean::value, TagValueBean::setValue)
        );
        form.setName("TagEntryUI Form");

        tagTree.addTagSelectionListener((status, node) -> {
            if (status.isActivated() && node instanceof Tag tag) {
                TagValueBean bean = form.getData();
                bean.setGroup(tag.groupName());
                bean.setTag(tag.tagName());
                form.setData(bean);
            }
        });

        return form;
    }

    private static Container createTagListPanel(TagTree tagTree) {
        JScrollPane scrollPane = new JScrollPane(tagTree);
        scrollPane.setPreferredSize(Dimensions.updateWidth(scrollPane.getPreferredSize(),
                                                           width -> width + tagTree.getPreferredExpansion()));
        return scrollPane;
    }

    private static Container createTagInfoPanel(TagTree tagTree) {
        TagInfoPanel pane = new TagInfoPanel(
                TagInfoPanel.StandardInfoItem.LABEL,
                TagInfoPanel.StandardInfoItem.DESCRIPTION
        );
        pane.setHyperlinkAction(TagInfoPanel::openWithDesktop);
        tagTree.addTagSelectionListener((_, tag) -> pane.setTag(tag));
        return new JScrollPane(pane);
    }

//    private static String getNodeInfoAsHtml(Object node) {
//        String header = (node instanceof Tag tag) ?
//                (node instanceof HasLabel hasLabel && !hasLabel.label().equalsIgnoreCase(tag.tagName())) ?
//                        "<b>%s:%s</b> (%s)".formatted(tag.groupName(), tag.tagName(), hasLabel.label()) :
//                        "<b>%s:%s</b>".formatted(tag.groupName(), tag.tagName()) :
//                (node instanceof HasLabel hasLabel) ?
//                        "<b>%s</b>".formatted(hasLabel.label()) :
//                        "";
//
//        String description = (node instanceof HasDescription hasDescription) ?
//                "<p>" + hasDescription.description().strip()
//                        .replaceAll("\\v+", "</p><p>") + "</p>" :
//                "";
//
//        return """
//               <html>
//                  %s
//                  %s
//               </html>
//               """.formatted(header, description);
//    }

    private static TagTree createTagTree(TagRepository tagRepository) {
        return new TagTree(tagRepository.getGroupedFamilies());
    }

    private static JPanel createTagEntryPanel(SimpleForm<?> form, Container actionBar) {
        var panel = new JPanel();
        var organizer = SpringLayoutOrganizer.organize(panel);

        organizer.add(form)
                .atTop()
                .atLeftSide()
                .definingContainerWidth()
                .above(actionBar);

        organizer.add(actionBar)
//                .below(form)
                .atRightSide()
                .atBottom()
//                .definingContainerHeight()
        ;

        var formSize = form.getWrappedContentSize();

        panel.setPreferredSize(new Dimension(formSize.width, formSize.height + actionBar.getPreferredSize().height));

        return panel;
    }

    private static Container createActionBar(SimpleForm<?> form, String action, ActionListener actionlistener) {

        JButton actionButton = new JButton(action);
        actionButton.setMaximumSize(actionButton.getPreferredSize());
        actionButton.addActionListener(actionlistener);
        actionButton.setEnabled(!form.isEmpty());
        form.addFormListener(FormListener.onChanged(f -> actionButton.setEnabled(!f.isEmpty())));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, PAD, PAD));
        panel.add(actionButton);
        panel.setMaximumSize(panel.getPreferredSize());

        return panel;
    }

    private static class TagValueBean implements TagValue {
        private String group;
        private String tag;
        private String value;

        public TagValueBean() {
        }

        public TagValueBean(TagValue tagValue) {
            this.group = tagValue.groupName();
            this.tag = tagValue.tagName();
            this.value = tagValue.value();
        }

        @Override
        public Tag tag() {
            return Tag.of(groupName(), tagName());
        }

        @Override
        public String groupName() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        @Override
        public String tagName() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        @Override
        public String value() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", TagValueBean.class.getSimpleName() + "[", "]")
                    .add("group='" + group + "'")
                    .add("tag='" + tag + "'")
                    .add("value='" + value + "'")
                    .toString();
        }
    }
}
