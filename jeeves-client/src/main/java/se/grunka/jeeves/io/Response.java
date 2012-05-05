package se.grunka.jeeves.io;

public class Response {
    public final int status;
    public final String content;

    public Response(int status, String content) {
        this.status = status;
        this.content = content;
    }
}
