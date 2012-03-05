package se.grunka.jeeves.test;

import se.grunka.jeeves.Param;

public class TestServiceCombined implements TestServiceA, TestServiceB, TestServiceC {
    @Override
    public int methodOne(@Param("one") String one) {
        return 42;
    }

    @Override
    public void methodTwo(@Param("two") int two) {
        System.out.println("hello");
    }

    @Override
    public String methodThree(@Param("one") String one, @Param("three") double three) {
        return "methodThree";
    }
}
