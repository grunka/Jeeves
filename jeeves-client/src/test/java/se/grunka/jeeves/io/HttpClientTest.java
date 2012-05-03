package se.grunka.jeeves.io;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HttpClientTest {

    private HttpClient httpClient;

    @Before
    public void before() throws Exception {
        httpClient = new HttpClient();
    }

    @Test
    public void shouldDoIt() throws Exception {
        Response response = httpClient.post(new URL("http://localhost:8000"), "hello\n");
        assertEquals(-1, response.status);
    }

    //TODO
}
