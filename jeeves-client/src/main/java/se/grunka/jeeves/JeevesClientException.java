package se.grunka.jeeves;

public class JeevesClientException extends RuntimeException {
    public JeevesClientException(String message) {
        super(message);
    }

    public JeevesClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
