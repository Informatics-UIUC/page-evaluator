package edu.illinois.i3.emop.apps.pageevaluator.exceptions;

public class UnsupportedLanguageException extends Exception {

    public UnsupportedLanguageException(String message) {
        super(message);
    }

    public UnsupportedLanguageException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedLanguageException(Throwable cause) {
        super(cause);
    }

}
