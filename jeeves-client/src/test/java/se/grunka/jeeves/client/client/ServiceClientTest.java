package se.grunka.jeeves.client.client;

import java.io.Serializable;

import org.junit.Ignore;
import se.grunka.jeeves.client.ServiceClient;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ServiceClientTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailBecauseItIsNotAnInterface() throws Exception {
        ServiceClient.create(String.class, "nada");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailBecauseItIsNotAService() throws Exception {
        ServiceClient.create(Serializable.class, "nada");
    }

    @Ignore
    @Test
    public void shouldTestMore() throws Exception {
        fail();
    }
}
