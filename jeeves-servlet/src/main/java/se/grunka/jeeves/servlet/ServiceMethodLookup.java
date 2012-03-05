package se.grunka.jeeves.servlet;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import se.grunka.jeeves.Method;
import se.grunka.jeeves.Param;
import se.grunka.jeeves.Service;

class ServiceMethodLookup {
    private final Map<ServiceMethodKey, ServiceMethod> serviceMethods = new HashMap<ServiceMethodKey, ServiceMethod>();

    public void addService(Class<?> serviceType) {
        if (serviceType == null) {
            throw new NullPointerException("Service type is null");
        }
        if (serviceType.isInterface()) {
            throw new IllegalArgumentException("Cannot register an interface, must be an implementation");
        }
        boolean hasServiceInterface = false;
        for (Class<?> serviceInterface : serviceType.getInterfaces()) {
            Service serviceAnnotation = serviceInterface.getAnnotation(Service.class);
            if (serviceAnnotation == null) {
                continue;
            }
            String serviceName = serviceAnnotation.value();
            for (java.lang.reflect.Method method : serviceInterface.getDeclaredMethods()) {
                Method methodAnnotation = method.getAnnotation(Method.class);
                if (methodAnnotation == null) {
                    continue;
                }
                String methodName = methodAnnotation.value();
                hasServiceInterface = true;
                ServiceMethodKey key = new ServiceMethodKey(serviceName, methodName);
                ServiceMethod serviceMethod = serviceMethods.get(key);
                if (serviceMethod == null) {
                    serviceMethod = new ServiceMethod();
                    serviceMethods.put(key, serviceMethod);
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                String[] argumentOrder = new String[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> parameterType = parameterTypes[i];
                    Param paramAnnotation = null;
                    for (Annotation annotation : parameterAnnotations[i]) {
                        if (annotation instanceof Param) {
                            paramAnnotation = (Param) annotation;
                            break;
                        }
                    }
                    if (paramAnnotation == null) {
                        throw new IllegalArgumentException(serviceInterface.getName() + " - " + method.getName() + " has parameters not annotated with @Param");
                    }
                    Map<String, Class<?>> parameterTypesMap = serviceMethod.parameterTypes;
                    if (parameterTypesMap.containsKey(paramAnnotation.value()) && !parameterTypesMap.get(paramAnnotation.value()).equals(parameterType)) {
                        throw new IllegalArgumentException("/" + serviceName + "/" + methodName + " has duplicate parameter names with different types");
                    }
                    parameterTypesMap.put(paramAnnotation.value(), parameterType);
                    argumentOrder[i] = paramAnnotation.value();
                }
                Set<String> argumentKey = new HashSet<String>(Arrays.asList(argumentOrder));
                if (argumentKey.size() != argumentOrder.length) {
                    throw new IllegalArgumentException("/" + serviceName + "/" + methodName + " has duplicate names in @Param annotations");
                }
                if (serviceMethod.methodDetails.containsKey(argumentKey)) {
                    throw new IllegalArgumentException("/" + serviceName + "/" + methodName + " already have a method with the parameters " + Arrays.toString(argumentOrder));
                }
                serviceMethod.methodDetails.put(argumentKey, new MethodDetails(serviceType, method, argumentOrder));
            }
        }
        if (!hasServiceInterface) {
            throw new IllegalArgumentException("Service did not have an interface annotated with @Service with at least one method annotated with @Method");
        }
    }

    public ServiceMethod find(String service, String method) {
        return serviceMethods.get(new ServiceMethodKey(service, method));
    }

    private static class ServiceMethodKey {
        private final String service;
        private final String method;

        public ServiceMethodKey(String service, String method) {
            this.service = service;
            this.method = method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ServiceMethodKey that = (ServiceMethodKey) o;

            return service.equals(that.service) && method.equals(that.method);
        }

        @Override
        public int hashCode() {
            int result = service.hashCode();
            result = 31 * result + method.hashCode();
            return result;
        }
    }

}
