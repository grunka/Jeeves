package se.grunka.jeeves;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

class MethodEndpoint {
    public final URL url;
    public final Type returnType;
    private final String[] arguments;

    public MethodEndpoint(URL url, String[] arguments, Type returnType) {
        this.arguments = arguments;
        this.url = url;
        this.returnType = returnType;
    }

    public Map<String, Object> createArgumentMap(Object[] args) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (int i = 0; i < arguments.length; i++) {
            result.put(arguments[i], args[i]);
        }
        return result;
    }
}
