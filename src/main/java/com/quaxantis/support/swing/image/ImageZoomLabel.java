package com.quaxantis.support.swing.image;

import com.quaxantis.support.swing.AspectRatio;
import com.quaxantis.support.swing.Dimensions;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

public class ImageZoomLabel extends JLabel {

    private final BufferedImage image;
    private final AspectRatio aspectRatio;
    private double zoom = -1d;

    public ImageZoomLabel(BufferedImage image) {
        this.image = image;
        this.aspectRatio = new AspectRatio(image.getWidth(), image.getHeight());
        addMouseWheelListener(zoomListener());
    }

    private boolean isZoomed() {
        return zoom > 0;
    }

    private void setZoom(double zoom) {
        if (zoom <= 0) {
            throw new IllegalArgumentException("Zoom must be greater than zero: " + zoom);
        }
        // Adapt the size based on the full image size and the zoom factor with a minimum of 10x10 px
        Dimension newSize = new Dimension((int) (image.getWidth() * zoom), (int) (image.getHeight() * zoom));
        newSize = Dimensions.withMinimumWidthAndHeight(newSize, 10, 10);
        newSize = aspectRatio.grow(newSize);

        System.out.println("Zoom " + zoom + " => " + newSize);

        // Resize the image
        setIcon(new ImageIcon(image.getScaledInstance(newSize.width, newSize.height, Image.SCALE_SMOOTH)));
        this.zoom = zoom;
    }

    private void adjustZoom(double adjustment) {
        double currentZoom;

        if (isZoomed()) {
            currentZoom = zoom;
        } else {
            // If we have not zoomed before, calculate the initial zoom factor from the current size and the full image size
            Icon scaledImage = getIcon();
            currentZoom = Math.max(divideToDouble(scaledImage.getIconWidth(), image.getWidth()),
                                   divideToDouble(scaledImage.getIconHeight(), image.getHeight()));
        }

        if (currentZoom > 0.1 || (currentZoom == 0.1 && adjustment > 0)) {
            // Adjust the zoom factor and round to steps of 0.1
            setZoom(Math.max(0.01, round(currentZoom + adjustment, 10.0d)));
        } else {
            // Adjust the zoom factor and round to steps of 0.01
            setZoom(Math.max(0.01, round(currentZoom + adjustment/10.0d, 100.0d)));
        }
    }

    public void setViewSize(Dimension viewSize) {
        // Adapt to the view size if the image was not previously zoomed
        if (!isZoomed()) {
            // Change the image size to fit within the view size while keeping the aspect ratio
            Dimension newSize = aspectRatio.shrink(viewSize);
            setIcon(new ImageIcon(image.getScaledInstance(newSize.width, newSize.height, Image.SCALE_SMOOTH)));
        }
    }

    private MouseWheelListener zoomListener() {
        return event -> {
            // Zoom on wheel only when CTRL is pressed
            if (event.isControlDown()) {
                // Adjust the zoom factor based on the wheel rotation
                adjustZoom(-0.1d * (double) event.getWheelRotation());
            } else {
                // Let other wheel events bubble up
                getParent().dispatchEvent(event);
            }
        };
    }

    private static double divideToDouble(int x, int y) {
        return ((double) x) / ((double) y);
    }

    private static double round(double d, double inversePrecision) {
        return ((double) Math.round(inversePrecision * d)) / inversePrecision;
    }
}
