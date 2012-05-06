package se.grunka.jeeves;

public class Message {
    public final String type;
    public final String message;

    public Message(String message) {
        this(null, message);
    }

    public Message(String type, String message) {
        this.type = type;
        this.message = message;
    }
}
