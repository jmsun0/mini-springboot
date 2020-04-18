package com.sjm.core.mini.springboot.ext;

public class SpringException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SpringException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpringException(Throwable cause) {
        super(cause);
    }

    public SpringException(String message) {
        super(message);
    }

    public SpringException() {
        super();
    }
}
