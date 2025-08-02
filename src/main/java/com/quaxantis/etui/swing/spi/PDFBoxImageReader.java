package com.quaxantis.etui.swing.spi;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.RenderDestination;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

public class PDFBoxImageReader extends ImageReader {

    private PDDocument document;

    public PDFBoxImageReader(ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        if (seekForwardOnly) {
            throw new UnsupportedOperationException("seekForwardOnly not supported");
        }
        super.setInput(input, false, ignoreMetadata);
        if (input == null) {
            this.document = null;
        }
    }

    private PDDocument getDocument() throws IOException {
        if (this.document == null) {
            if (this.input == null) {
                throw new IOException("Input not set");
            } else {
                this.document = Loader.loadPDF(normalizeInput(this.input).orElseThrow(
                        () -> new IllegalArgumentException("Incorrect input type: " + this.input.getClass())));
            }
        }
        return this.document;
    }

    @Override
    public void dispose() {
        super.dispose();
        this.document = null;
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        return getDocument().getNumberOfPages();
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        var page = getDocument().getPage(imageIndex);
        var cropBox = page.getCropBox();
        return (int) Math.ceil(page.getUserUnit() * cropBox.getWidth());
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        var page = getDocument().getPage(imageIndex);
        var cropBox = page.getCropBox();
        return (int) Math.ceil(page.getUserUnit() * cropBox.getHeight());
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) {
        return Collections.emptyIterator();
    }

    @Override
    public IIOMetadata getStreamMetadata() {
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) {
        return null;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        PDFRenderer renderer = new PDFRenderer(getDocument());
        renderer.setDefaultDestination(RenderDestination.VIEW);
        if (param != null && param.getDestination() instanceof BufferedImage destination) {
            renderer.renderPageToGraphics(imageIndex, destination.createGraphics());
            return destination;
        } else {
            return renderer.renderImage(imageIndex, 4.0f);
        }
    }

    static Optional<File> normalizeInput(Object source) {
        File sourceFile = (source instanceof File file) ? file
                : (source instanceof Path path) ? path.toFile()
                : null;

        return Optional.ofNullable(sourceFile);
    }
}
