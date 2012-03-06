package se.grunka.jeeves;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import se.grunka.jeeves.test.TestServiceWithMultipleParametersImpl;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceMethodTest {

    private ServiceMethod target;
    private Injector injector;
    private Map<String, Object> parameters;

    @Before
    public void before() throws Exception {
        target = new ServiceMethod();
        parameters = new HashMap<String, Object>();
        injector = mock(Injector.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldFailIfNoMethodWithMatchingParametersIsFound() throws Exception {
        target.invoke(injector, parameters);
    }

    @Test
    public void shouldOrderArgumentsCorrectly() throws Exception {
        parameters.put("a", "hello");
        parameters.put("b", "world");
        target.methodDetails.put(parameters.keySet(), new MethodDetails(TestServiceWithMultipleParametersImpl.class, TestServiceWithMultipleParametersImpl.class.getDeclaredMethod("method", String.class, String.class), new String[]{"a", "b"}));
        when(injector.getInstance(TestServiceWithMultipleParametersImpl.class)).thenReturn(new TestServiceWithMultipleParametersImpl());

        Object result = target.invoke(injector, parameters);
        assertEquals("a=hello, b=world", result);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldFailCorrectlyForIllegalAccess() throws Exception {
        target.methodDetails.put(parameters.keySet(), new MethodDetails(TestServiceWithMultipleParametersImpl.class, TestServiceWithMultipleParametersImpl.class.getDeclaredMethod("illegal"), new String[0]));
        when(injector.getInstance(TestServiceWithMultipleParametersImpl.class)).thenReturn(new TestServiceWithMultipleParametersImpl());

        target.invoke(injector, parameters);
    }
}
