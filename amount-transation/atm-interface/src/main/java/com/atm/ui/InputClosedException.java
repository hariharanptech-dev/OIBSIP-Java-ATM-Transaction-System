package com.atm.ui;

/** Raised when standard input reaches end-of-file (Ctrl+D / Ctrl+Z / closed pipe). */
public class InputClosedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InputClosedException() {
        super("Input stream closed");
    }
}
