package se.grunka.jeeves;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.grunka.jeeves.io.HttpClient;
import se.grunka.jeeves.io.Response;

class RemoteInvocationHandler implements InvocationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteInvocationHandler.class);
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private final Map<java.lang.reflect.Method, MethodEndpoint> methodEndpoints;
    private final HttpClient client;

    public RemoteInvocationHandler(Map<Method, MethodEndpoint> methodEndpoints, HttpClient client) {
        this.methodEndpoints = methodEndpoints;
        this.client = client;
    }

    @Override
    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
        MethodEndpoint methodEndpoint = methodEndpoints.get(method);
        if (methodEndpoint == null) {
            throw new UnsupportedOperationException("Method not registered as a service method, probably missing the @Method annotation");
        }
        Map<String, Object> arguments = createArgumentList(args, methodEndpoint);
        Response response = client.post(methodEndpoint.url, GSON.toJson(arguments));
        if (response.status == 200) {
            return GSON.fromJson(response.content, method.getReturnType());
        } else if (response.status == 400) {
            LOGGER.debug(response.content);
            throw new IllegalArgumentException("Request was not allowed");
        } else if (response.status == 404) {
            LOGGER.debug(response.content);
            throw new IllegalArgumentException("Remote method not found");
        } else if (response.status == 503) {
            Map<String, String> errorResponse = GSON.fromJson(response.content, MAP_TYPE);
            Class<? extends Exception> exceptionClass = (Class<? extends Exception>) Class.forName(errorResponse.get("type"));
            Constructor<? extends Exception> messageConstructor = exceptionClass.getConstructor(String.class);
            throw messageConstructor.newInstance(errorResponse.get("message"));
        } else if (response.status < 0) {
            //TODO handle connection errors
            throw new IllegalStateException(response.content);
        } else {
            //TODO handle other error, probably not a correct service on the other end
            throw new Error(response.status + ": " + response.content);
        }
        //TODO handle json parse exceptions
    }

    private Map<String, Object> createArgumentList(Object[] args, MethodEndpoint methodEndpoint) {
        Map<String, Object> arguments = new HashMap<String, Object>();
        for (int i = 0; i < methodEndpoint.arguments.length; i++) {
            arguments.put(methodEndpoint.arguments[i], args[i]);
        }
        return arguments;
    }
}
