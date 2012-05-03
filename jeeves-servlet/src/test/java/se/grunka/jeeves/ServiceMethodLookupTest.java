package se.grunka.jeeves;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import se.grunka.jeeves.test.NoService;
import se.grunka.jeeves.test.TestService1;
import se.grunka.jeeves.test.TestService1Impl;
import se.grunka.jeeves.test.TestServiceCombined;
import se.grunka.jeeves.test.TestServiceWithDuplicateDefinitionImpl;
import se.grunka.jeeves.test.TestServiceWithDuplicateParamsImpl;
import se.grunka.jeeves.test.TestServiceWithMissingParamImpl;
import se.grunka.jeeves.test.TestServiceWithOverloadedMethodsAndDuplicateNamesImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceMethodLookupTest {

    private ServiceMethodLookup lookup;
    private Injector injector;

    @Before
    public void setUp() throws Exception {
        lookup = new ServiceMethodLookup();
        createInjector();
    }

    @SuppressWarnings("unchecked")
    private void createInjector() {
        injector = mock(Injector.class);
        when(injector.getInstance(any(Class.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return ((Class<?>) invocation.getArguments()[0]).newInstance();
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfNoServiceIsDefined() throws Exception {
        lookup.addService(NoService.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfInterfaceIsAdded() throws Exception {
        lookup.addService(TestService1.class);
    }

    @Test(expected = NullPointerException.class)
    public void shouldFailIfNullIsAdded() throws Exception {
        lookup.addService(null);
    }

    @Test
    public void shouldNotAddNonAnnotatedMethods() throws Exception {
        lookup.addService(TestService1Impl.class);
        assertNull(lookup.find("service-one", "notAServiceMethod"));
    }

    @Test
    public void shouldAddTypesDefinedInService() throws Exception {
        lookup.addService(TestService1Impl.class);
        ServiceMethod serviceMethod = lookup.find("service-one", "method-one");
        assertEquals(1, serviceMethod.parameterTypes.size());
        assertEquals(String.class, serviceMethod.parameterTypes.get("parameter-one"));
    }

    @Test
    public void shouldInvokeMethodOne() throws Exception {
        lookup.addService(TestService1Impl.class);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("parameter-one", "value-one");
        Object result = lookup.find("service-one", "method-one").invoke(injector, parameters);
        assertEquals("method1(value-one)", result);
    }

    @Test
    public void shouldFailInvokeForUnknownMethod() throws Exception {
        assertNull(lookup.find("service-unknown", "method-unknown"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailWithMissingParameterAnnotation() throws Exception {
        lookup.addService(TestServiceWithMissingParamImpl.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailWithDuplicateParameterNames() throws Exception {
        lookup.addService(TestServiceWithDuplicateParamsImpl.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailOverloadedMethodsWithDuplicateParameterNames() throws Exception {
        lookup.addService(TestServiceWithOverloadedMethodsAndDuplicateNamesImpl.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailForDuplicateMethodDefinitions() throws Exception {
        lookup.addService(TestServiceWithDuplicateDefinitionImpl.class);
    }

    @Test
    public void shouldHandleOverloadedServices() throws Exception {
        lookup.addService(TestServiceCombined.class);
        ServiceMethod serviceMethod = lookup.find("service", "method");
        Map<String, Class<?>> types = serviceMethod.parameterTypes;
        assertEquals(String.class, types.get("one"));
        assertEquals(int.class, types.get("two"));
        assertEquals(double.class, types.get("three"));

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("one", "Hello");
        Object firstResult = serviceMethod.invoke(injector, parameters);
        parameters.put("three", 4.0);
        Object secondResult = serviceMethod.invoke(injector, parameters);

        assertEquals(42, firstResult);
        assertEquals("methodThree", secondResult);
    }

    //TODO
}
