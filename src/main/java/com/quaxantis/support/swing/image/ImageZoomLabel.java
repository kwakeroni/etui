package com.quaxantis.support.swing.image;

import com.quaxantis.support.swing.AspectRatio;
import com.quaxantis.support.swing.Dimensions;

import javax.annotation.Nullable;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

public class ImageZoomLabel extends JLabel {

    @Nullable
    private BufferedImage image;
    private AspectRatio aspectRatio;
    private Dimension viewSize;
    private double zoom = -1d;

    public ImageZoomLabel() {
        this(null);
    }

    public ImageZoomLabel(BufferedImage image) {
        setImage(image);
        addMouseWheelListener(zoomListener());
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        if (image != null) {
            this.aspectRatio = new AspectRatio(image.getWidth(), image.getHeight());
        }
        updateIcon();
    }

    private boolean isZoomed() {
        return zoom > 0;
    }

    private void updateIcon() {
        if (this.image == null) {
            setIcon(null);
            return;
        }
        // Adapt to the view size if the image was not previously zoomed
        if (!isZoomed()) {
            // Scale only if the image does not fit within the view size
            if (viewSize != null && (image.getWidth() > viewSize.width || image.getHeight() > viewSize.height)) {
//                 Change the image size to fit within the view size while keeping the aspect ratio
                Dimension newSize = aspectRatio.shrink(viewSize);
                setIcon(new ImageIcon(image.getScaledInstance(newSize.width, newSize.height, Image.SCALE_SMOOTH)));
            } else {
                setIcon(new ImageIcon(this.image));
            }
        } else {
            // Adapt the size based on the full image size and the zoom factor with a minimum of 10x10 px
            Dimension newSize = new Dimension((int) (image.getWidth() * zoom), (int) (image.getHeight() * zoom));
            newSize = Dimensions.withMinimumWidthAndHeight(newSize, 10, 10);
            newSize = aspectRatio.grow(newSize);
            // Resize the image

            setIcon(new ImageIcon(image.getScaledInstance(newSize.width, newSize.height, Image.SCALE_SMOOTH)));
        }
    }

    private void setZoom(double zoom) {
        if (zoom <= 0) {
            throw new IllegalArgumentException("Zoom must be greater than zero: " + zoom);
        }

//        System.out.println("Zoom " + zoom + " => " + newSize);

        this.zoom = zoom;
        updateIcon();
    }

    private void adjustZoom(double adjustment) {
        if (this.image == null) {
            return;
        }

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
            setZoom(Math.max(0.01, round(currentZoom + adjustment / 10.0d, 100.0d)));
        }
    }

    public void setViewSize(Dimension viewSize) {
        this.viewSize = viewSize;
        updateIcon();
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
