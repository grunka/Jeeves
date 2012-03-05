package se.grunka.jeeves.servlet;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ArgumentDeserializerTest {
    private ArgumentDeserializer target;
    private Map<String, Class<?>> types;
    private Map<String, String[]> parameters;
    private InputStream inputStream;
    private String content;

    @Before
    public void before() throws Exception {
        target = new ArgumentDeserializer();
        types = new HashMap<String, Class<?>>();
        types.put("list", List.class);
        types.put("text", String.class);
        types.put("integer", Integer.class);
        types.put("complex", Thingy.class);
        parameters = new HashMap<String, String[]>();
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
    public void shouldHandleSimpleValues() throws Exception {
        addParameter("text", "value");
        addParameter("integer", 42);
        Map<String, Object> result = target.fromRequestParameters(parameters, types);
        assertEquals(2, result.size());
        assertEquals("value", result.get("text"));
        assertEquals(42, result.get("integer"));
    }

    private void addParameter(String name, Object... values) {
        String[] strings = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strings[i] = String.valueOf(values[i]);
        }
        parameters.put(name, strings);
    }

    @Test
    public void shouldHandleLists() throws Exception {
        addParameter("list[]", "a", "b", "c");
        Map<String, Object> result = target.fromRequestParameters(parameters, types);
        assertEquals(1, result.size());
        assertEquals(Arrays.asList("a", "b", "c"), result.get("list"));
    }

    private static class Thingy {
        public String a = null;
        public List<String> list = null;
        public List<Thingy> things = null;
        public Thingy thing = null;
    }

    @Test
    public void shouldHandleComplexTypes() throws Exception {
        addParameter("complex[a]", "value");
        Map<String, Object> result = target.fromRequestParameters(parameters, types);
        assertEquals(1, result.size());
        Thingy thingy = (Thingy) result.get("complex");
        assertEquals("value", thingy.a);
    }

    @Test
    public void shouldHandleComplexTypesWithList() throws Exception {
        addParameter("complex[list][]", "a", "b", "c");
        Map<String, Object> result = target.fromRequestParameters(parameters, types);
        assertEquals(1, result.size());
        Thingy thingy = (Thingy) result.get("complex");
        assertEquals(Arrays.asList("a", "b", "c"), thingy.list);
    }

    @Test
    public void shouldHandleComplexTypesWithComplexType() throws Exception {
        addParameter("complex[thing][list][]", "a", "b", "c");
        Map<String, Object> result = target.fromRequestParameters(parameters, types);
        assertEquals(1, result.size());
        Thingy thingy = (Thingy) result.get("complex");
        assertEquals(Arrays.asList("a", "b", "c"), thingy.thing.list);
    }

    @Test
    public void shouldHandleComplexListWithOrder() throws Exception {
        addParameter("complex[list][1]", "b");
        addParameter("complex[list][2]", "c");
        addParameter("complex[list][0]", "a");
        Map<String, Object> result = target.fromRequestParameters(parameters, types);
        assertEquals(1, result.size());
        Thingy thingy = (Thingy) result.get("complex");
        assertEquals(Arrays.asList("a", "b", "c"), thingy.list);
    }

    @Test
    public void shouldHandleComplexTypesWithListOfComplexThings() throws Exception {
        addParameter("complex[things][0][a]", "1");
        addParameter("complex[things][1][a]", "2");
        addParameter("complex[things][2][a]", "3");
        Map<String, Object> result = target.fromRequestParameters(parameters, types);
        assertEquals(1, result.size());
        Thingy thingy = (Thingy) result.get("complex");
        assertEquals(3, thingy.things.size());
        assertEquals("1", thingy.things.get(0).a);
        assertEquals("2", thingy.things.get(1).a);
        assertEquals("3", thingy.things.get(2).a);
    }

    @Test
    public void shouldHandleNoParameters() throws Exception {
        Map<String, Object> result = target.fromRequestParameters(parameters, types);
        assertEquals(0, result.size());
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
    public void shouldFailForMissingTypesForParameters() throws Exception {
        addParameter("missing", "whatever");
        target.fromRequestParameters(parameters, types);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailForMissingTypesForInputStream() throws Exception {
        content = "{missing:whatever}";
        Map<String, Object> result = target.fromJsonInputStream(inputStream, types);
        assertEquals(0, result.size());
    }
}
