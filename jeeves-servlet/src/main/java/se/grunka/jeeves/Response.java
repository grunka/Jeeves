package se.grunka.jeeves;

class Response {
    public final int status;
    public final Object content;

    public Response(int status, Object content) {
        this.status = status;
        this.content = content;
    }
}
