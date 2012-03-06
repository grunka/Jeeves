package se.grunka.jeeves.sample.impl;

import com.google.gson.Gson;
import se.grunka.jeeves.sample.api.*;

public class MyService implements HelloWorld, MathOperations, BaseService, Complex {
    @Override
    public String helloWorld() {
        return helloWho("World");
    }

    @Override
    public String helloWho(String who) {
        return "Hello " + who + "!";
    }

    @Override
    public int add(int x, int y) {
        return x + y;
    }

    @Override
    public int nothing() {
        return 0;
    }

    @Override
    public String complex(ComplexValue value) {
        return new Gson().toJson(value);
    }
}
