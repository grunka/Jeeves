package se.grunka.jeeves;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArgumentDeserializerTest {
    private ArgumentDeserializer target;
    private Map<String, Class<?>> types;
    private InputStream inputStream;
    private String content;

    @Before
    public void before() throws Exception {
        target = new ArgumentDeserializer();
        types = new HashMap<String, Class<?>>();
        types.put("list", List.class);
        types.put("text", String.class);
        types.put("integer", Integer.class);
        content = "";
        inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                if (content.length() == 0) {
                    return -1;
                }
                try {
                    return content.charAt(0);
                } finally {
                    content = content.substring(1);
                }
            }
        };
    }

    @Test
    public void shouldHandleEmptyInputStream() throws Exception {
        Map<String, Object> result = target.fromJsonInputStream(inputStream, types);
        assertEquals(0, result.size());
    }

    @Test
    public void shouldHandleEmptyObjectInStream() throws Exception {
        content = "{}";
        Map<String, Object> result = target.fromJsonInputStream(inputStream, types);
        assertEquals(0, result.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailForMissingTypesForInputStream() throws Exception {
        content = "{missing:whatever}";
        Map<String, Object> result = target.fromJsonInputStream(inputStream, types);
        assertEquals(0, result.size());
    }

    //TODO
}
