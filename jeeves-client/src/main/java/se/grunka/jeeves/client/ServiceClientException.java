package se.grunka.jeeves.client;

public class ServiceClientException extends RuntimeException {
    public ServiceClientException(String message) {
        super(message);
    }

    public ServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
