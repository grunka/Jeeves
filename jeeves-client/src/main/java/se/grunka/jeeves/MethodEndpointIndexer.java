package se.grunka.jeeves;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

class MethodEndpointIndexer {
    public <T> Map<java.lang.reflect.Method, MethodEndpoint> createIndex(Class<T> type, String servicePath) {
        final Map<java.lang.reflect.Method, MethodEndpoint> methodEndpoints = new HashMap<java.lang.reflect.Method, MethodEndpoint>();
        for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
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
        return methodEndpoints;
    }
}
