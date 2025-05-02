package com.quaxantis.support.swing.util;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.util.Objects;

public abstract class TransferHandlerAdapter extends TransferHandler {

    private final TransferHandler delegate;

    protected TransferHandlerAdapter(TransferHandler delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return delegate.canImport(comp, transferFlavors);
    }

    public boolean canImport(TransferHandler.TransferSupport support) {
        return delegate.canImport(support);
    }

    public boolean importData(TransferHandler.TransferSupport support) {
        return delegate.importData(support);
    }

    public boolean importData(JComponent comp, Transferable t) {
        return delegate.importData(comp, t);
    }

    public Point getDragImageOffset() {
        return delegate.getDragImageOffset();
    }

    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        delegate.exportToClipboard(comp, clip, action);
    }

    public Icon getVisualRepresentation(Transferable t) {
        return delegate.getVisualRepresentation(t);
    }

    public Image getDragImage() {
        return delegate.getDragImage();
    }

    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        delegate.exportAsDrag(comp, e, action);
    }

    public void setDragImageOffset(Point p) {
        delegate.setDragImageOffset(p);
    }

    public int getSourceActions(JComponent c) {
        return delegate.getSourceActions(c);
    }

    public void setDragImage(Image img) {
        delegate.setDragImage(img);
    }
}
