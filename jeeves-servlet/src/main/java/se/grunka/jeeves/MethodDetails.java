package se.grunka.jeeves;

class MethodDetails {
    public final String[] argumentOrder;
    public final Class<?> service;
    public final java.lang.reflect.Method method;

    MethodDetails(Class<?> service, java.lang.reflect.Method method, String[] argumentOrder) {
        this.argumentOrder = argumentOrder;
        this.method = method;
        this.service = service;
    }
}
