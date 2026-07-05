package io.github.connellite.jcl.cloner;

public final class CloningException extends RuntimeException {

    public CloningException(String message) {
        super(message);
    }

    public CloningException(String message, Throwable cause) {
        super(message, cause);
    }
}
