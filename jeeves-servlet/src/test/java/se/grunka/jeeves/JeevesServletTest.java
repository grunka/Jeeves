package se.grunka.jeeves;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JeevesServletTest {

    private JeevesServlet servlet;
    private ServletConfig config;

    @Before
    public void before() throws Exception {
        servlet = new JeevesServlet();
        config = mock(ServletConfig.class);
    }

    @Test(expected = ServletException.class)
    public void shouldRequireServicesParameter() throws Exception {
        when(config.getInitParameter("services")).thenReturn(null);
        servlet.init(config);
    }

    @Test(expected = ServletException.class)
    public void shouldRequireNonEmptyServicesParameter() throws Exception {
        when(config.getInitParameter("services")).thenReturn("   \t     \r\n   ");
        servlet.init(config);
    }

    @Test(expected = ServletException.class)
    public void shouldFailNicelyForUnknownTypes() throws Exception {
        when(config.getInitParameter("services")).thenReturn("com.example.NotARealType");
        servlet.init(config);
    }

    @Ignore
    @Test
    public void shouldTest() throws Exception {
        fail();
    }
}
