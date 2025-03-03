package com.quaxantis.etui.swing;

import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.application.ExiftoolUI;
import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.application.file.FileStateMachine;
import com.quaxantis.etui.swing.actions.ViewActions;
import com.quaxantis.etui.swing.menu.ExiftoolMenu;
import com.quaxantis.etui.swing.actions.FileActions;
import com.quaxantis.etui.swing.table.TagTableUI;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.etui.template.TemplateRepository;
import com.quaxantis.support.swing.Dimensions;
import com.quaxantis.support.swing.ToolTips;
import com.quaxantis.support.swing.layout.SpringLayoutOrganizer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.*;
import java.nio.file.Path;
import java.util.Optional;

// TODO: lifecycle of multiple edit/delete/add operations
// TODO: Reset all
public class ExiftoolSwingUI implements ExiftoolUI {
    public static final int PAD = 5;
    public static final String FRAME_TITLE = "Exiftool Template UI";

    private final JFrame frame;
    private final TagSetUI tagSetUI;

    public ExiftoolSwingUI(ConfigOperations configOperations, FileStateMachine fileStateMachine, TagRepository tagRepository, TemplateRepository templateRepository) {
        ToolTips.setToolTipBackground(new Color(255, 250, 227));

        var fileActions = new FileActions(fileStateMachine, configOperations);
        var viewActions = new ViewActions(configOperations);
        this.frame = createFrame(fileActions, viewActions, configOperations);
        this.tagSetUI = createTagSetUI(fileStateMachine, tagRepository, templateRepository, configOperations);

        composeUI(frame, tagSetUI, fileActions);
    }

    @Override
    public void display() {
        frame.pack();
        Dimension maxSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();

        if (!Dimensions.fitsInto(frame.getSize(), maxSize)) {
            frame.setPreferredSize(
                    Dimensions.cropTo(
                            frame.getSize(),
            maxSize
            //                Dimensions.minus(maxSize, Dimensions.ofInsets(frame.getInsets()))
            ));
            frame.pack();
        }
        frame.setVisible(true);
    }

    @Override
    public void loadTagSet(Path path, TagSet tagSet) {
        setFrameTitleFile(path);
        this.tagSetUI.setTagSet(tagSet, path);
    }

    @Override
    public void closeCurrentTagSet() {
        setFrameTitleFile(null);
        this.tagSetUI.clear();
    }

    @Override
    public Optional<TagSet> getChanges() {
        return this.tagSetUI.getTagTableUI()
                .map(TagTableUI::getChanges);
    }

    private void setFrameTitleFile(Path file) {
        frame.setTitle(FRAME_TITLE + ((file == null) ? "" : " [" + file + "]"));
    }

    private static JFrame createFrame(FileActions fileActions, ViewActions viewActions, ConfigOperations configOperations) {
        JFrame frame = new JFrame(FRAME_TITLE);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setJMenuBar(new ExiftoolMenu(fileActions, viewActions, configOperations));
        frame.setMinimumSize(new Dimension(640, 480));
        return frame;
    }

    private static TagSetUI createTagSetUI(FileStateMachine fileStateMachine, TagRepository tagRepository, TemplateRepository templateRepository, ConfigOperations configOperations) {
        return new TagSetUI(fileStateMachine, tagRepository, templateRepository, configOperations);
    }

    private static void composeUI(JFrame frame, TagSetUI tagSetUI, FileActions fileActions) {
        Container actionBar = createActionBar(fileActions);
        Container container = frame.getContentPane();
        var organizer = SpringLayoutOrganizer.organize(container);
        organizer.add(actionBar)
                .notStretching()
                .atTop()
                .atRightSide();

        organizer.add(tagSetUI)
                .atLeftSide()
                .below(actionBar)
                .definingContainerWidth()
                .definingContainerHeight();

//        Dimension tableDimension = tagSetUI.getPreferredSize();
//        Dimension barDimension = actionBar.getPreferredSize();
//        Dimension preferred = new Dimension(
//                Math.max((int) tableDimension.getWidth(), (int) barDimension.getWidth()),
//                (int) tableDimension.getHeight() + (int) barDimension.getHeight());
//        container.setPreferredSize(preferred);
//        return preferred;
    }

    private static Container createActionBar(FileActions actions) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, PAD, PAD));
        panel.add(new JButton(actions.open()));
        panel.add(new JButton(actions.save()));
        panel.add(new JButton(actions.saveAs()));
        return panel;
    }
}
