package se.grunka.jeeves;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class ServiceMethod {
    public final Map<String, Class<?>> parameterTypes = new HashMap<String, Class<?>>();
    public final Map<Set<String>, MethodDetails> methodDetails = new HashMap<Set<String>, MethodDetails>();
}
