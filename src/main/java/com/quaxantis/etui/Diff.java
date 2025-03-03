package com.quaxantis.etui;

import com.quaxantis.etui.exiftool.Exiftool;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Diff {
    static {
        System.setProperty(Exiftool.PROPERTY_EXIFTOOL_EXECUTABLE, "C:/Programs/bin/exiftool");
    }

    public static void main(String[] args) throws IOException {
        Path left= Path.of("C:/Users/Eigenaar/AppData/Local/Temp/exiftool2997817169087028412.txt");
        Path right = Path.of("C:/Users/Eigenaar/AppData/Local/Temp/exiftool401630955629237774.txt");
        diff(left, right);
    }

    public static void diff(Path input, Path output) throws IOException {

        List<String> diff = List.of("C:/Program Files/Git/usr/bin/diff",
                                    "--unchanged-line-format=",
                                    "--old-line-format=- %L",
                                    "--new-line-format=+ %L",

                                    input.toString(), output.toString());
        System.out.println("Executing " + String.join(" ", diff).replace("C:", "/c").replace('\\', '/'));
        new ProcessBuilder()
                .inheritIO()
                .command(diff)
                .start();

//        Desktop.getDesktop().open(input.toFile());
//        Desktop.getDesktop().open(output.toFile());
    }
}
