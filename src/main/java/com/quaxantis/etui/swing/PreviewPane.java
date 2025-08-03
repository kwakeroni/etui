package com.quaxantis.etui.swing;

import com.quaxantis.support.swing.Dimensions;
import com.quaxantis.support.swing.image.ImageZoomLabel;
import com.quaxantis.support.swing.layout.SpringLayoutOrganizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class PreviewPane extends JPanel implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PreviewPane.class);

    private final NavigationBar navigationBar;

    private PreviewPane(ImageZoomLabel imageLabel, NavigationBar navigationBar) {
        this.navigationBar = navigationBar;
        initializeUI(this, imageLabel, navigationBar);
    }

    public static Optional<PreviewPane> of(Path file) {
        try {
            var imageLabel = new ImageZoomLabel();
            return createNavigationBar(file, imageLabel)
                    .map(navigationBar -> new PreviewPane(imageLabel, navigationBar));
        } catch (Exception exc) {
            log.warn("Unable to preview image {}", file, exc);
            return Optional.empty();
        }
    }

    private static Optional<NavigationBar> createNavigationBar(Path file, ImageZoomLabel imageLabel) throws IOException {
        for (ImageReader reader : (Iterable<ImageReader>) () -> ImageIO.getImageReaders(file)) {
            reader.setInput(file);
            return Optional.of(new NavigationBar(null, reader, imageLabel::setImage));
        }

        var imageInputStream = ImageIO.createImageInputStream(file.toFile());
        if (imageInputStream != null) {
            for (ImageReader reader : (Iterable<ImageReader>) () -> ImageIO.getImageReaders(imageInputStream)) {
                reader.setInput(imageInputStream);
                return Optional.of(new NavigationBar(imageInputStream, reader, imageLabel::setImage));
            }
        }

        log.debug("Unable to preview image {}", file);
        return Optional.empty();
    }

    private static void initializeUI(Container container, ImageZoomLabel imageLabel, NavigationBar navigationBar) {
        var flowLayout = new FlowLayout();
        int padding = Math.max(flowLayout.getHgap(), flowLayout.getVgap());

        var previewContainer = new JPanel(flowLayout);
        var scrollPane = new JScrollPane();

        previewContainer.add(imageLabel);
        previewContainer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                imageLabel.setViewSize(Dimensions.minus(scrollPane.getViewport().getSize(), 2 * padding));
            }
        });

        scrollPane.setViewportView(previewContainer);
        int inc = 20;
        scrollPane.getVerticalScrollBar().setUnitIncrement(inc);
        scrollPane.getVerticalScrollBar().setBlockIncrement(inc);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(inc);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(inc);


        SpringLayoutOrganizer.organize(container)
                .add(navigationBar)
                .atLeftSide()
                .atBottom()
                .stretchToRightSide()
                .and()
                .add(scrollPane)
                .atTop()
                .atLeftSide()
                .stretchToRightSide()
                .above(navigationBar)
        ;
    }

    @Override
    public void removeNotify() {
        try {
            close();
        } catch (IOException ioe) {
            log.warn("Unable to discard PreviewPane resources", ioe);
        }

        super.removeNotify();
    }

    @Override
    public void close() throws IOException {
        if (this.navigationBar != null) {
            this.navigationBar.close();
        }
    }

    private static class NavigationBar extends JPanel implements Closeable {
        private static final int PAD = 5;
        @Nullable
        private ImageInputStream imageInputStream;
        private ImageReader imageReader;
        @Nonnull
        private final Consumer<BufferedImage> onSelectedImage;
        private int current;
        @Nullable
        private BufferedImage currentImage;
        private final int imageCount;
        private final JButton previous;
        private final JButton next;
        private final PageNumberField currentDisplay;

        public NavigationBar(@Nullable ImageInputStream imageInputStream, @Nonnull ImageReader imageReader, @Nonnull Consumer<BufferedImage> onSelectedImage) throws IOException {
            super(new FlowLayout(FlowLayout.CENTER, PAD, PAD));
            this.imageInputStream = imageInputStream;
            this.imageReader = Objects.requireNonNull(imageReader, "imageReader");
            this.onSelectedImage = Objects.requireNonNull(onSelectedImage, "onSelectedImage");
            this.current = 0;
            this.imageCount = imageReader.getNumImages(true);
            this.previous = new JButton("◀");
            this.next = new JButton("▶");
            this.currentDisplay = new PageNumberField(0, this.imageCount);
            this.initUI();
            this.showImage(0);
        }

        private void initUI() {
            this.previous.addActionListener(_ -> {
                if (current > 0) {
                    SwingUtilities.invokeLater(() -> showImage(current - 1));
                }
            });

            this.next.addActionListener(_ -> {
                if (current < imageCount) {
                    SwingUtilities.invokeLater(() -> showImage(current + 1));
                }
            });

            this.currentDisplay.addValueChangeListener(evt -> {
                if (!Objects.equals(evt.getOldValue(), evt.getNewValue())) {
                    if (evt.getNewValue() instanceof Integer i && i >= 0 && i < imageCount) {
                        showImage(i);
                    }
                }
            });

            this.add(currentDisplay);
            this.add(new JLabel(" / " + this.imageCount));
            this.add(previous);
            this.add(next);
        }

        private void showImage(int number) {
            current = Math.clamp(number, 0, imageCount - 1);
            previous.setEnabled(current > 0);
            next.setEnabled(current < imageCount - 1);
            this.currentDisplay.setValue(this.current);
            try {
                this.currentImage = this.imageReader.read(current);
            } catch (IOException ioe) {
                log.error("Unable to display image #{}", this.current + 1, ioe);
                this.currentImage = null;
            }
            onSelectedImage.accept(this.currentImage);
        }

        @Override
        public void close() throws IOException {
            var reader = this.imageReader;
            var stream = this.imageInputStream;

            this.currentImage = null;
            this.imageReader = null;
            this.imageInputStream = null;

            try (stream) {
                if (reader != null) {
                    reader.setInput(null);
                    reader.dispose();
                }
            }
        }

    }

    private static class PageNumberField extends JFormattedTextField {

        public PageNumberField(int minIncl, int maxExcl) {
            super(formatter(minIncl, maxExcl));


            setColumns(Math.max(2, 1 + (int) Math.log10(maxExcl)));
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    SwingUtilities.invokeLater(() -> selectAll());
                }
            });

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    char keyChar = e.getKeyChar();
                    if (keyChar == KeyEvent.VK_DELETE || keyChar < ' ') {
                        return;
                    }
                    if (keyChar < '0' || keyChar > '9') {
                        Toolkit.getDefaultToolkit().beep();
                        e.consume();
                    }
                }
            });
        }

        public void addValueChangeListener(PropertyChangeListener listener) {
            addPropertyChangeListener("value", listener);
        }

        private static AbstractFormatter formatter(int minIncl, int maxExcl) {
            return new JFormattedTextField.AbstractFormatter() {
                @Override
                public Object stringToValue(String text) throws ParseException {
                    int value;
                    try {
                        value = Integer.parseInt(text) - 1;
                    } catch (Exception exc) {
                        throw new ParseException("Not a number", 0);
                    }
                    if (value < minIncl || value >= maxExcl) {
                        throw new ParseException("Not within range", 0);
                    }
                    return value;
                }

                @Override
                public String valueToString(Object value) throws ParseException {
                    if (value instanceof Integer i) {
                        return Integer.toString(i + 1);
                    } else {
                        throw new ParseException("Not an integer: " + value, 0);
                    }
                }
            };
        }
    }
}
