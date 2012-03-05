package se.grunka.jeeves.test;

public class TestServiceWithOverloadedMethodsAndDuplicateNamesImpl implements TestServiceWithOverloadedMethodsAndDuplicateNames {
    @Override
    public void method(String one) {
        System.out.println("hello");
    }

    @Override
    public void method(int one) {
        System.out.println("world");
    }
}
