package se.grunka.jeeves;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class ArgumentDeserializerTest {
    private ArgumentDeserializer target;
    private Map<String, Class<?>> types;
    private String content;

    @Before
    public void before() throws Exception {
        target = new ArgumentDeserializer();
        types = new HashMap<String, Class<?>>();
        types.put("list", List.class);
        types.put("text", String.class);
        types.put("integer", Integer.class);
        content = "";
    }

    @Test
    public void shouldHandleEmptyInputStream() throws Exception {
        //Map<String, Object> result = target.fromJson(content, types);
        //assertEquals(0, result.size());
    }

    @Test
    public void shouldHandleEmptyObjectInStream() throws Exception {
        content = "{}";
        //Map<String, Object> result = target.fromJson(content, types);
        //assertEquals(0, result.size());
    }

    @Test
    public void shouldFailForMissingTypesForInputStream() throws Exception {
        content = "{missing:whatever}";
        //Map<String, Object> result = target.fromJson(content, types);
        //assertNull(result);
    }

    //TODO
}
