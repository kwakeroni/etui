package com.quaxantis.etui.swing;

import com.quaxantis.etui.TagSet;
import com.quaxantis.etui.application.config.ConfigOperations;
import com.quaxantis.etui.application.config.Configuration;
import com.quaxantis.etui.application.file.FileStateMachine;
import com.quaxantis.etui.swing.table.TagTableUI;
import com.quaxantis.etui.swing.template.TemplateUI;
import com.quaxantis.etui.tag.TagRepository;
import com.quaxantis.etui.template.TemplateRepository;
import com.quaxantis.support.swing.Dimensions;
import com.quaxantis.support.swing.image.ImageZoomLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

class TagSetUI extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(TagSetUI.class);

    private final FileStateMachine fileStateMachine;
    private final TagRepository tagRepository;
    private final TemplateRepository templateRepository;
    private final ConfigOperations configOperations;
    private TagTableUI tagTableUI;

    TagSetUI(FileStateMachine fileStateMachine, TagRepository tagRepository, TemplateRepository templateRepository, ConfigOperations configOperations) {
        this.fileStateMachine = fileStateMachine;
        this.tagRepository = tagRepository;
        this.templateRepository = templateRepository;
        this.configOperations = configOperations;
        setLayout(new GridLayout(1, 1));
    }

    Optional<TagTableUI> getTagTableUI() {
        return Optional.ofNullable(this.tagTableUI);
    }

    void clear() {
        replaceUI(() -> null);
    }

    void setTagSet(@Nonnull TagSet tagSet, @Nullable Path file) {
        replaceUI(() -> {
            var tagTableUi = createTagTableUI(tagSet, fileStateMachine, tagRepository, configOperations.getConfiguration());
            var tabbedPane = createTabs(templateRepository, configOperations, tagTableUi, file);
            this.add(tabbedPane);
            return tagTableUi;
        });
    }

    private void replaceUI(Supplier<TagTableUI> installer) {
        this.removeAll();
        this.tagTableUI = installer.get();
        this.invalidate();
        this.validate();
        this.repaint();
    }

    private static TagTableUI createTagTableUI(TagSet tagSet, FileStateMachine fileStateMachine, TagRepository tagRepository, Configuration configuration) {
        var tagTableUi = new TagTableUI(tagSet, tagRepository, configuration);

        tagTableUi.addChangeListener(e -> {
            boolean hasChanges = tagTableUi.getChanges().stream().findAny().isPresent();
            if (hasChanges) {
                fileStateMachine.onChanged();
            } else {
                fileStateMachine.onRevert();
            }
        });

        return tagTableUi;
    }

    private static JTabbedPane createTabs(TemplateRepository templateRepository, ConfigOperations configOperations, TagTableUI tagTableUi, Path file) {
        var tabbedPane = new JTabbedPane() {
            @Override
            public void setSelectedIndex(int index) {
                Arrays.stream(listenerList.getListeners(TabListener.class))
                        .forEach(listener -> listener.beforeTabSelected(this, index));
                super.setSelectedIndex(index);
            }

            public void addTabListener(TabListener tabListener) {
                listenerList.add(TabListener.class, tabListener);
            }
        };

        var templateUi = createTemplateUI(templateRepository, configOperations, tagTableUi, tabbedPane, 0);
        Container templateContainer = templateUi.getUIContainer();
        tabbedPane.addTabListener((JTabbedPane pane, int index) -> {
            if (index == pane.indexOfComponent(templateContainer)) {
                templateUi.setTags(tagTableUi.getTags());
            }
        });


        tabbedPane.addTab("Tags", tagTableUi.getUIContainer());
        tabbedPane.addTab("Template", templateContainer);
        createPreviewPane(file).ifPresent(
                previewPane -> tabbedPane.addTab("Preview", previewPane));

        return tabbedPane;
    }

    private static TemplateUI createTemplateUI(TemplateRepository templateRepository, ConfigOperations configOperations, TagTableUI tagTable, JTabbedPane tabbedPane, int index) {
        return new TemplateUI(templateRepository, configOperations, "Apply", tagSetSupplier -> e -> {
            tagTable.mergeTags(tagSetSupplier.get());
            tabbedPane.setSelectedIndex(index);
        });
    }

    private static Optional<Container> createPreviewPane(Path file) {
        if (file != null) {
            try {
                return Optional.ofNullable(createPreviewPane0(file));
            } catch (Exception exc) {
                log.error("Unable to preview image {}", file, exc);
            }
        }
        return Optional.empty();
    }

    @Nullable
    private static Container createPreviewPane0(Path file) throws IOException {
        var flowLayout = new FlowLayout();
        int padding = Math.max(flowLayout.getHgap(), flowLayout.getVgap());
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            log.info("Unable to preview image {}", file);
            return null;
        }
        var imageLabel = new ImageZoomLabel(image);
        var previewContainer = new JPanel(flowLayout);
        var scrollPane = new JScrollPane(previewContainer);
        int inc = 20;
        scrollPane.getVerticalScrollBar().setUnitIncrement(inc);
        scrollPane.getVerticalScrollBar().setBlockIncrement(inc);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(inc);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(inc);

        previewContainer.add(imageLabel);

        previewContainer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                imageLabel.setViewSize(Dimensions.minus(scrollPane.getViewport().getSize(), 2 * padding));
            }
        });

        return scrollPane;
    }

}
