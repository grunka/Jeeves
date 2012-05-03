package se.grunka.jeeves;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import se.grunka.jeeves.io.HttpClient;

public class JeevesClient {
    private static final HttpClient CLIENT = new HttpClient();
    private static final MethodEndpointIndexer INDEXER = new MethodEndpointIndexer();

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> type, String uri) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, createInvocationHandler(type, uri));
    }

    private static <T> InvocationHandler createInvocationHandler(Class<T> type, String uri) {
        Service service = ensureService(type);
        String servicePath = createServicePath(uri, service);
        final Map<Method, MethodEndpoint> methodEndpoints = INDEXER.createIndex(type, servicePath);
        return new RemoteInvocationHandler(methodEndpoints, CLIENT);
    }

    private static <T> Service ensureService(Class<T> type) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Only interfaces are allowed");
        }
        Service service = type.getAnnotation(Service.class);
        if (service == null) {
            throw new IllegalArgumentException("Not annotated with @Service");
        }
        return service;
    }

    private static String createServicePath(String uri, Service service) {
        String servicePath = uri;
        if (!service.value().isEmpty()) {
            if (!servicePath.endsWith("/")) {
                servicePath += "/";
            }
            servicePath += service.value();
            if (!servicePath.endsWith("/")) {
                servicePath += "/";
            }
        }
        return servicePath;
    }
}
