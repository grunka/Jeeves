package se.grunka.jeeves.sample;

import java.util.List;

public class ComplexValue {
    public final int i;
    public final String s;
    public final double d;
    public final float f;
    public final boolean b;
    public final List<String> list;

    public ComplexValue(boolean b, float f, double d, String s, int i, List<String> list) {
        this.b = b;
        this.f = f;
        this.d = d;
        this.s = s;
        this.i = i;
        this.list = list;
    }
}
