package com.quaxantis.support.util;

import com.quaxantis.support.ide.API;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@API
public final class GlobMatcher {

    @API
    public static GlobMatcher of(String pathPattern, String defaultPattern) {
        Path root = Path.of(pathPattern.replaceAll("[?*{\\[]", "_"));
        while (root.getParent() != null) {
            root = root.getParent();
        }

        String[] chunks = pathPattern.split("[/\\\\]");
        boolean hasGlob = false;
        Path path = root;
        for (String chunk : chunks) {
            hasGlob = hasGlobPattern(chunk);
            if (hasGlob) {
                break;
            } else {
                path = path.resolve(chunk);
            }
        }

        if (!hasGlob) {
            pathPattern = path + "/" + defaultPattern;
        }
        PathMatcher matcher = root.getFileSystem().getPathMatcher("glob:" + pathPattern.replaceAll("\\\\", "/"));

        if (!hasGlob) {
            Path selfPath = path;
            return new GlobMatcher(path, p -> selfPath.equals(p) || matcher.matches(p));
        } else {
            return new GlobMatcher(path, matcher);
        }


    }

    private static final Predicate<String> HAS_GLOB = Pattern.compile("(?<!\\\\)[?*{\\[]").asPredicate();

    private final Path path;
    private final PathMatcher matcher;

    private GlobMatcher(Path path, PathMatcher matcher) {
        this.path = path;
        this.matcher = matcher;
    }

    static boolean hasGlobPattern(String filename) {
        return HAS_GLOB.test(filename);
    }

    @API
    public boolean matches(Path path) {
        return matcher.matches(path);
    }

    @API
    @SuppressWarnings("resource")
    public Stream<Path> walk() {
        try {
            return Files.walk(this.path).filter(this.matcher::matches);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @API
    public Path path() {
        return path;
    }

    @Override
    public String toString() {
        return "GlobMatcher[" +
               "path=" + path + ", " +
               "matcher=" + matcher + ']';
    }

}
