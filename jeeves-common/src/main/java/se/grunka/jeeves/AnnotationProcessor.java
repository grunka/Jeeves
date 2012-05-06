package se.grunka.jeeves;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationProcessor {
    public void processService(Class<?> serviceType, Callback callback) {
        process(serviceType, callback);
        for (Class<?> interfaceType : serviceType.getInterfaces()) {
            process(interfaceType, callback);
        }
    }

    public void process(Class<?> type, Callback callback) {
        Service service = type.getAnnotation(Service.class);
        if (service == null) {
            return;
        }
        String serviceUrl = service.value();
        if (serviceUrl.contains("/")) {
            throw new IllegalArgumentException("Service name " + serviceUrl + " contains illegal character '/");
        }
        serviceUrl = "/" + serviceUrl;
        if (!serviceUrl.endsWith("/")) {
            serviceUrl = serviceUrl + "/";
        }
        processMethods(type, serviceUrl, callback);
    }

    private void processMethods(Class<?> type, String serviceUrl, Callback callback) {
        for (Method method : type.getDeclaredMethods()) {
            se.grunka.jeeves.Method methodAnnotation = method.getAnnotation(se.grunka.jeeves.Method.class);
            if (methodAnnotation == null) {
                continue;
            }
            String methodName = methodAnnotation.value();
            if (methodName.contains("/")) {
                throw new IllegalArgumentException("Method name " + methodName + " contains illegal character '/'");
            }
            String methodUrl = serviceUrl + methodName;
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            Class<?>[] parameterTypes = method.getParameterTypes();
            String[] parameterNames = new String[parameterTypes.length];
            for (int parameter = 0; parameter < parameterTypes.length; parameter++) {
                Param param = getParamAnnotation(parameterAnnotations[parameter]);
                if (param == null) {
                    throw new IllegalArgumentException("Annotated service method " + type.getName() + " " + method.getName() + " is missing @Param annotation");
                }
                String paramName = param.value();
                parameterNames[parameter] = paramName;
            }
            callback.method(methodUrl, method, parameterNames, parameterTypes);
        }
    }

    private Param getParamAnnotation(Annotation[] parameterAnnotation) {
        for (Annotation annotation : parameterAnnotation) {
            if (annotation instanceof Param) {
                return (Param) annotation;
            }
        }
        return null;
    }

    public interface Callback {
        void method(String path, Method method, String[] names, Class<?>[] types);
    }
}
