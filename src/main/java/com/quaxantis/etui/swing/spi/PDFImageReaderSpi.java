package com.quaxantis.etui.swing.spi;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class PDFImageReaderSpi extends ImageReaderSpi {

    private static final boolean IS_PDFBOX_AVAILABLE;

    public PDFImageReaderSpi() {
        super(
                "Quaxantis",
                "0.1",
                new String[]{"pdf", "PDF"},
                new String[]{"pdf", "PDF"},
                new String[]{"application/pdf"},
                "com.quaxantis.etui.swing.spi.PDFImageReader",
                new Class<?>[]{File.class, Path.class},
                new String[0],
                false,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null
        );
    }

    public String getDescription(Locale locale) {
        return "PDF Preview Image Reader based on Apache PDFBox";
    }

    public ImageReader createReaderInstance(Object extension) {
        return new PDFBoxImageReader(this);
    }

    public boolean canDecodeInput(Object source) throws IOException {
        if (!IS_PDFBOX_AVAILABLE) {
            return false;
        }

        if (!(PDFBoxImageReader.normalizeInput(source).orElse(null) instanceof File sourceFile)) {
            return false;
        }

        // Acrobat viewers require only that the header appear somewhere within
        // the first 1024 bytes of the file. [PDF Reference 1.3, implementation note 13]

        try (InputStream stream = Files.newInputStream(sourceFile.toPath())) {
            byte[] header = readBytes(stream, new byte[1024]);
            return canDecodeInput(header);
        }

    }

    private static boolean canDecodeInput(byte[] header) {
        byte[] magic = "%PDF-".getBytes();
        for (int i = 0; i < header.length; i++) {
            while ((i < header.length) && header[i] != magic[0]) {
                i++;
            }
            int j = 1;
            while ((j < magic.length) && (i + j < header.length) && header[i + j] == magic[j]) {
                j++;
            }
            if (j == magic.length) {
                return true;
            }
        }
        return false;
    }

    private static byte[] readBytes(InputStream stream, byte[] bytes) throws IOException {
        int offset = 0;
        while (offset < bytes.length) {
            int count = stream.read(bytes, offset, bytes.length - offset);
            if (count < 0) {
                break;
            }
            offset += count;

        }
        return bytes;
    }

    static {
        boolean hasLoadPDFMethod;
        try {
            Method loadPDF = Class.forName("org.apache.pdfbox.Loader").getMethod("loadPDF", File.class);
            hasLoadPDFMethod = Modifier.isStatic(loadPDF.getModifiers());
        } catch (Exception e) {
            hasLoadPDFMethod = false;
        }
        IS_PDFBOX_AVAILABLE = hasLoadPDFMethod;
    }

}
