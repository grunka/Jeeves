package se.grunka.jeeves;

import org.junit.Ignore;
import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.fail;

public class JeevesClientTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailBecauseItIsNotAnInterface() throws Exception {
        JeevesClient.create(String.class, "nada");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailBecauseItIsNotAService() throws Exception {
        JeevesClient.create(Serializable.class, "nada");
    }

    @Ignore
    @Test
    public void shouldTestMore() throws Exception {
        fail();
    }
}
