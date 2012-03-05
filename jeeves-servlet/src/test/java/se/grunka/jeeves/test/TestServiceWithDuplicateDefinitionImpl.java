package se.grunka.jeeves.test;

public class TestServiceWithDuplicateDefinitionImpl implements TestServiceWithDuplicateDefinition {
    @Override
    public void method1(int one) {
        System.out.println("hello");
    }

    @Override
    public void method2(int one) {
        System.out.println("world");
    }
}
