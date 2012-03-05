package se.grunka.jeeves.test;

public class TestServiceWithMissingParamImpl implements TestServiceWithMissingParam {
    @Override
    public void method(String one, String two) {
        System.out.println("hello");
    }
}
