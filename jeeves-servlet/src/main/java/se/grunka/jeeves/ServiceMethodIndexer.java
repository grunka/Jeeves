package se.grunka.jeeves;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ServiceMethodIndexer {
    public void updateIndex(Map<String, ServiceMethod> index, Class<?> serviceType) {
        if (serviceType.isInterface()) {
            throw new IllegalArgumentException("Service type is an interface, needs to be an implementation " + serviceType.getName());
        }
        handleType(index, serviceType, serviceType);
        for (Class<?> interfaceType : serviceType.getInterfaces()) {
            handleType(index, interfaceType, serviceType);
        }
        if (index.size() == 0) {
            throw new IllegalArgumentException("Type did not have a @Service annotation on itself or interfaces");
        }
    }

    private void handleType(Map<String, ServiceMethod> serviceMethodIndex, Class<?> type, Class<?> serviceType) {
        Service service = type.getAnnotation(Service.class);
        if (service == null) {
            return;
        }
        String url = service.value();
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        handleMethods(serviceMethodIndex, type, serviceType, url);
    }

    private void handleMethods(Map<String, ServiceMethod> serviceMethodIndex, Class<?> type, Class<?> serviceType, String url) {
        for (Method method : type.getDeclaredMethods()) {
            se.grunka.jeeves.Method methodAnnotation = method.getAnnotation(se.grunka.jeeves.Method.class);
            if (methodAnnotation == null) {
                continue;
            }
            String methodName = methodAnnotation.value();
            if (methodName.startsWith("/")) {
                methodName = methodName.substring(1);
            }
            String serviceUrl = url + methodName;
            ServiceMethod serviceMethod = serviceMethodIndex.get(serviceUrl);
            if (serviceMethod == null) {
                serviceMethod = new ServiceMethod();
                serviceMethodIndex.put(serviceUrl, serviceMethod);
            }
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            Class<?>[] parameterTypes = method.getParameterTypes();
            Set<String> paramNames = new HashSet<String>();
            String[] paramOrder = new String[parameterTypes.length];
            for (int parameter = 0; parameter < parameterTypes.length; parameter++) {
                Param param = getParamAnnotation(parameterAnnotations[parameter]);
                if (param == null) {
                    throw new IllegalArgumentException("Annotated service method " + type.getName() + "::" + method.getName() + " is missing @Param annotation");
                }
                String paramName = param.value();
                Class<?> existingParameterType = serviceMethod.parameterTypes.get(paramName);
                if (existingParameterType != null && !existingParameterType.equals(parameterTypes[parameter])) {
                    throw new IllegalArgumentException("Duplicate parameter name with differing type " + paramName);
                }
                serviceMethod.parameterTypes.put(paramName, parameterTypes[parameter]);
                paramNames.add(paramName);
                paramOrder[parameter] = paramName;
            }
            serviceMethod.methodDetails.put(paramNames, new MethodDetails(serviceType, method, paramOrder));
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
}
