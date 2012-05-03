package se.grunka.jeeves;

import java.io.Serializable;

import org.junit.Test;

public class JeevesClientTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailBecauseItIsNotAnInterface() throws Exception {
        JeevesClient.create(String.class, "nada");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailBecauseItIsNotAService() throws Exception {
        JeevesClient.create(Serializable.class, "nada");
    }

    //TODO
}
