package se.grunka.jeeves;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import se.grunka.jeeves.io.HttpClient;

public class JeevesClient {
    private static final HttpClient CLIENT = new HttpClient();
    private static final AnnotationProcessor ANNOTATION_PROCESSOR = new AnnotationProcessor();

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> type, String uri) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, createInvocationHandler(type, uri));
    }

    private static <T> InvocationHandler createInvocationHandler(Class<T> type, String uri) {
        final String serviceUri;
        if (uri.endsWith("/")) {
            serviceUri = uri.substring(0, uri.length() - 2);
        } else {
            serviceUri = uri;
        }
        final Map<Method, MethodEndpoint> methodEndpoints = new HashMap<Method, MethodEndpoint>();
        ANNOTATION_PROCESSOR.process(type, new AnnotationProcessor.Callback() {
            @Override
            public void method(String path, Method method, String[] names, Class<?>[] types) {
                try {
                    methodEndpoints.put(method, new MethodEndpoint(new URL(serviceUri + path), names, method.getReturnType()));
                } catch (MalformedURLException e) {
                    throw new Error("The generated url is malformed", e);
                }
            }
        });
        if (methodEndpoints.isEmpty()) {
            throw new Error("No properly configured method endpoints were found for " + type.getName());
        }
        return new RemoteInvocationHandler(methodEndpoints, CLIENT);
    }
}
