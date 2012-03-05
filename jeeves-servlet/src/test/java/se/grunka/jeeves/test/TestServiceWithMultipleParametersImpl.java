package se.grunka.jeeves.test;

public class TestServiceWithMultipleParametersImpl implements TestServiceWithMultipleParameters {
    @Override
    public String method(String a, String b) {
        return "a=" + a + ", b=" + b;
    }

    protected String illegal() {
        return null;
    }
}
