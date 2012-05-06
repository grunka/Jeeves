package se.grunka.jeeves;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import se.grunka.jeeves.io.HttpClient;
import se.grunka.jeeves.io.Response;

class RemoteInvocationHandler implements InvocationHandler {
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
        Map<String, Object> arguments = methodEndpoint.createArgumentMap(args);
        Response response = client.request(methodEndpoint.url, arguments, methodEndpoint.returnType);
        //TODO add own exception for connection errors for easier handling
        if (response.status == 200) {
            return response.content;
        } else if (response.status == 400) {
            throw new IllegalArgumentException("Request was not allowed");
        } else if (response.status == 404) {
            throw new IllegalArgumentException("Remote method not found");
        } else if (response.status == 503) {
            Message message = (Message) response.content;
            @SuppressWarnings("unchecked")
            Class<? extends Exception> exceptionClass = (Class<? extends Exception>) Class.forName(message.type);
            Constructor<? extends Exception> messageConstructor = exceptionClass.getConstructor(String.class);
            throw messageConstructor.newInstance(message.message);
        } else if (response.status < 0) {
            //TODO handle connection errors, check type and things...
            throw new IllegalStateException(((Message) response.content).message);
        } else {
            //TODO handle other error, probably not a correct service on the other end
            throw new Error(response.status + ": " + response.content);
        }
    }
}
