package se.grunka.jeeves.client;

import com.google.gson.Gson;
import se.grunka.jeeves.Param;
import se.grunka.jeeves.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ServiceClient {
    private static final Gson GSON = new Gson();
    private static final String POST = "POST";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String JSON = "application/json; charset=UTF-8";

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> type, String uri) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, createInvocationHandler(type, uri));
    }

    private static <T> InvocationHandler createInvocationHandler(Class<T> type, String uri) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Only interfaces are allowed");
        }
        Service service = type.getAnnotation(Service.class);
        if (service == null) {
            throw new IllegalArgumentException("Not annotated with @Service");
        }
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
        final Map<Method, MethodEndpoint> methodEndpoints = new HashMap<Method, MethodEndpoint>();
        for (Method method : type.getDeclaredMethods()) {
            se.grunka.jeeves.Method methodAnnotation = method.getAnnotation(se.grunka.jeeves.Method.class);
            if (methodAnnotation == null) {
                continue;
            }
            String methodPath = servicePath + methodAnnotation.value();
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            String[] arguments = new String[parameterAnnotations.length];
            int argument = 0;
            for (Annotation[] annotations : parameterAnnotations) {
                Param param = null;
                for (Annotation annotation : annotations) {
                    if (annotation instanceof Param) {
                        param = (Param) annotation;
                        break;
                    }
                }
                if (param == null) {
                    throw new IllegalArgumentException("Missing @Param annotation on a parameter on method " + method.getName() + " in " + type.getName());
                }
                arguments[argument++] = param.value();
            }
            try {
                methodEndpoints.put(method, new MethodEndpoint(new URL(methodPath), arguments));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Generated url is invalid " + methodPath, e);
            }
        }
        return new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                MethodEndpoint methodEndpoint = methodEndpoints.get(method);
                if (methodEndpoint == null) {
                    throw new UnsupportedOperationException("Method not registered as a service method, probably missing the @Method annotation");
                }
                Map<String, Object> arguments = createArgumentList(args, methodEndpoint);
                try {
                    HttpURLConnection connection = (HttpURLConnection) methodEndpoint.url.openConnection();
                    connection.setRequestMethod(POST);
                    connection.setRequestProperty(CONTENT_TYPE, JSON);
                    connection.setDoOutput(true);
                    writeRequest(arguments, connection);
                    return readResponse(method, connection);
                } catch (ConnectException e) {
                    throw new ServiceClientException("Could not connect to " + methodEndpoint.url, e);
                } catch (FileNotFoundException e) {
                    throw new ServiceClientException("Got 404 for " + methodEndpoint.url, e);
                }
            }

            private Map<String, Object> createArgumentList(Object[] args, MethodEndpoint methodEndpoint) {
                Map<String, Object> arguments = new HashMap<String, Object>();
                for (int i = 0; i < methodEndpoint.arguments.length; i++) {
                    arguments.put(methodEndpoint.arguments[i], args[i]);
                }
                return arguments;
            }

            private void writeRequest(Map<String, Object> arguments, HttpURLConnection connection) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                try {
                    GSON.toJson(arguments, writer);
                    writer.flush();
                } finally {
                    writer.close();
                }
            }

            private Object readResponse(Method method, HttpURLConnection connection) throws IOException {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                try {
                    return GSON.fromJson(reader, method.getReturnType());
                } finally {
                    reader.close();
                }
            }
        };
    }

    private static class MethodEndpoint {
        public final URL url;
        public final String[] arguments;

        private MethodEndpoint(URL url, String[] arguments) {
            this.arguments = arguments;
            this.url = url;
        }
    }
}
