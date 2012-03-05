package se.grunka.jeeves.test;

public class TestServiceWithDuplicateParamsImpl implements TestServiceWithDuplicateParams {
    @Override
    public void method(String one, String two) {
        System.out.println("hello");
    }
}
