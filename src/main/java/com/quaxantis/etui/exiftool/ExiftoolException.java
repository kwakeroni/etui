package com.quaxantis.etui.exiftool;

import java.util.List;
import java.util.Optional;

public class ExiftoolException extends RuntimeException {
    private List<String> command;

    public ExiftoolException(String message) {
        super(message);
    }

    public ExiftoolException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExiftoolException(String message, List<String> command) {
        super(message);
        this.command = List.copyOf(command);
    }

    public ExiftoolException(String message, Throwable cause, List<String> command) {
        super(message, cause);
        this.command = List.copyOf(command);
    }

    public Optional<List<String>> command() {
        return Optional.ofNullable(this.command);
    }
}
