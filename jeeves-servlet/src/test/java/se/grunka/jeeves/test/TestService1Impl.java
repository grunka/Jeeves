package se.grunka.jeeves.test;

public class TestService1Impl implements TestService1 {
    @Override
    public String method1(String param) {
        return "method1(" + param + ")";
    }

    @Override
    public void notAServiceMethod() {
        System.out.println("not a service method");
    }
}
