package se.grunka.jeeves;

import com.google.inject.Injector;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class ServiceMethod {
    public final Map<String, Class<?>> parameterTypes = new HashMap<String, Class<?>>();
    public final Map<Set<String>, MethodDetails> methodDetails = new HashMap<Set<String>, MethodDetails>();

    public Object invoke(Injector injector, Map<String, Object> parameters) throws InvocationTargetException {
        MethodDetails invocationMethodDetails = methodDetails.get(parameters.keySet());
        if (invocationMethodDetails == null) {
            throw new UnsupportedOperationException("Method not found with these parameters " + parameters.keySet());
        }
        Object[] arguments = new Object[invocationMethodDetails.argumentOrder.length];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = parameters.get(invocationMethodDetails.argumentOrder[i]);
        }
        try {
            return invocationMethodDetails.method.invoke(injector.getInstance(invocationMethodDetails.service), arguments);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("Not allowed to call this method", e);
        }
    }
}
