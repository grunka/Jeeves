package se.grunka.jeeves;

import java.net.URL;

class MethodEndpoint {
    public final URL url;
    public final String[] arguments;

    public MethodEndpoint(URL url, String[] arguments) {
        this.arguments = arguments;
        this.url = url;
    }
}
