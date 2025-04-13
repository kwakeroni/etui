package com.quaxantis.etui.swing.template;

import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.swing.taginfo.TagInfoPanel;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.etui.template.TemplateRepository;
import com.quaxantis.support.swing.form.FormField;
import com.quaxantis.support.swing.form.FormListener;
import com.quaxantis.support.swing.layout.SpringLayoutOrganizer;
import com.quaxantis.support.swing.util.QuickJFrame;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.util.function.Function;
import java.util.function.Supplier;

public class TemplateUI {

    private static final int PAD = 5;

    public static void main(String[] ignored) {
        var configOperations = new ConfigOperations();
        var configuration = configOperations.getConfiguration();
        var repo = new TemplateRepository(configuration, new TagRepository(configuration));

        TemplateUI templateUI = new TemplateUI(repo, configOperations, "Dump", tagSetSupplier -> e -> {
            System.out.println(tagSetSupplier.get());
        });
        QuickJFrame.of(templateUI.getUIContainer())
                .withTitle("Template UI")
                .show();
    }

    private final Container uiContainer;
    private final TemplatePanel templatePanel;

    public TemplateUI(TemplateRepository repository, ConfigOperations configOperations, String action, Function<Supplier<TagSet>, ActionListener> actionListener) {
        var actionButton = new JButton(action);
        this.templatePanel = new TemplatePanel(actionButton);
        this.uiContainer = composeUI(repository, configOperations, templatePanel, actionButton, actionListener.apply(this::getTags));
    }

    public Container getUIContainer() {
        return this.uiContainer;
    }

    public TagSet getTags() {
        return templatePanel.form().getTags();
    }

    public void setTags(TagSet tags) {
        templatePanel.setTags(tags);
    }

    private static Container composeUI(TemplateRepository templateRepository, ConfigOperations configOperations, TemplatePanel templatePanel, JButton actionButton, ActionListener actionlistener) {
        var tagInfoPanel = createTagInfoPanel(templatePanel);
        var templateSelector = createTemplateTree(templateRepository, templatePanel, tagInfoPanel, configOperations);
        var actionBar = createActionBar(templatePanel, actionButton, actionlistener);
        var templateScrollPane = new JScrollPane(templatePanel);

        var panel = new JPanel();
        var organizer = SpringLayoutOrganizer.organize(panel);

        organizer.add(templateScrollPane)
                .atTop()
                .above(actionBar)
                .atLeftSide()
                .stretchToRightSide()
                ;

        organizer.add(actionBar)
                .atBottom()
                .atLeftSide()
                .stretchToRightSide()
                ;

        panel.setPreferredSize(new Dimension(350, 200));
        tagInfoPanel.setPreferredSize(new Dimension(200, 200));
        var templateSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel, tagInfoPanel);
        templateSplitPane.setOneTouchExpandable(true);
        templateSplitPane.setResizeWeight(0.9);

        var mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, templateSelector, templateSplitPane);
        mainSplitPane.setOneTouchExpandable(true);

        return mainSplitPane;
    }

    private static TagInfoPanel createTagInfoPanel(TemplatePanel templatePanel) {
        var tagInfoPanel = new TagInfoPanel(
                TagInfoPanel.StandardInfoItem.LABEL,
                TagInfoPanel.StandardInfoItem.DESCRIPTION,
                TagInfoPanel.StandardInfoItem.FORMAT_DESCRIPTION.wrapped("<p><strong>format</strong>", "</p>"),
                TagInfoPanel.StandardInfoItem.FORMAT_EXAMPLES.wrapped("<p><strong>examples</strong>", "</p>")
        );
        tagInfoPanel.setHyperlinkAction(TagInfoPanel::openWithDesktop);
        templatePanel.addFormListener(new FormListener.Adapter<>() {
            @Override
            public void focusGained(TemplateForm form, FormField<?, ?> field, FocusEvent event) {
//                System.out.println("focus gained " + field.source());
                // TODO avoid cast-at-a-distance of source
                // TODO add variable description (before tag description)
                tagInfoPanel.setTag(field.source());
                tagInfoPanel.setExamplePatternAction(field::applyPattern);
            }
        });
        return tagInfoPanel;
    }

    private static Container createTemplateTree(TemplateRepository repository, TemplatePanel templatePanel, TagInfoPanel tagInfoPanel, ConfigOperations configOperations) {
        var templateGroups = repository.templateGroups();
        var templateTree = new TemplateTree(templateGroups);
        var panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.add(templateTree, BorderLayout.WEST);
        panel.setBackground(templateTree.getBackground());
        // Todo: init template area when reopening with a selected template
        templateTree.setExternalizedPath(configOperations.getConfiguration().getLastSelectedTemplate().orElse(null));
        templateTree.addTemplateSelectionListener((_, template) -> {
            tagInfoPanel.setTag(null);
            templatePanel.setTemplate(template);
        });
        templateTree.addTreeSelectionListener(e -> configOperations.setLastSelectedTemplate(templateTree.getExternalizedPath()));
        panel.setMaximumSize(templateTree.getPreferredSize());
        return new JScrollPane(panel);
    }

    private static Container createActionBar(TemplatePanel templatePanel, JButton actionButton, ActionListener actionlistener) {

        actionButton.setMaximumSize(actionButton.getPreferredSize());
        actionButton.addActionListener(e -> {
            actionlistener.actionPerformed(e);
            templatePanel.clearChangeCache();
        });

        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> templatePanel.resetForm());
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> templatePanel.clearForm());

        JPanel panel = new JPanel();
        var organizer = SpringLayoutOrganizer.organize(panel).setDefaultPadding(PAD);
        organizer.add(actionButton)
                .atTop()
                .atRightSide()
                .definingContainerHeight();

        organizer.add(reset).atSameTopAs(actionButton).atLeftSide();
        organizer.add(clear).atSameTopAs(actionButton).rightOf(reset);

        panel.setMaximumSize(panel.getPreferredSize());
        return panel;
    }


}
