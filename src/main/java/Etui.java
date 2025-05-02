//#!/c/Programs/jdk-22.0.1/bin/java --source 22 --class-path $CLASSPATH

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import com.quaxantis.etui.application.EtuiApplication;

import java.io.File;
import java.nio.file.Path;

// TODO: duplicate tag -> edit window
// TODO: XMP & XMP-MM tags |- metadatadate
// TODO: DC Format mimetypes
// TODO: "now pattern" for time fields
public final class Etui {

    @Parameter(description = "File to open", converter = FileConverter.class)
    private File file;

    public static void main(String[] args) {
        Etui etui = new Etui();
        JCommander.newBuilder()
                .addObject(etui)
                .build()
                .parse(args);

        System.out.println("Launching UI");
        EtuiApplication.main(etui.filePath());
    }

    private Etui() {
    }

    private Path filePath() {
        return (file == null) ? null : file.toPath();
    }
}
