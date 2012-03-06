package se.grunka.jeeves.servlet;

import com.google.gson.Gson;
import com.google.inject.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ServiceServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceServlet.class);
    private static final String SERVICES_PARAMETER = "services";
    private static final String MODULE_PARAMETER = "module";
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String POST = "POST";
    private static final String GET = "GET";
    private Injector injector = null;
    private final ServiceMethodLookup lookup = new ServiceMethodLookup();
    private final Gson outputEncoder = new Gson();
    private final ArgumentDeserializer deserializer = new ArgumentDeserializer();

    @Inject
    public void setInjector(Injector injector) {
        this.injector = injector;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (injector == null) {
            injector = Guice.createInjector(Stage.PRODUCTION, getModule(config));
        } else {
            injector = injector.createChildInjector(getModule(config));
        }
        String services = config.getInitParameter(SERVICES_PARAMETER);
        if (services == null || services.trim().isEmpty()) {
            throw new ServletException("No services defined");
        }
        for (String serviceType : services.split(",")) {
            lookup.addService(getType(serviceType));
        }
    }

    private Module[] getModule(ServletConfig config) throws ServletException {
        String moduleClassName = config.getInitParameter(MODULE_PARAMETER);
        Class<Module> moduleType = getType(moduleClassName);
        if (moduleType != null) {
            try {
                return new Module[]{moduleType.newInstance()};
            } catch (InstantiationException e) {
                throw new ServletException("Could not instantiate module " + moduleClassName, e);
            } catch (IllegalAccessException e) {
                throw new ServletException("Was not allowed to instantiate module " + moduleClassName, e);
            }
        } else {
            return new Module[0];
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> getType(String name) throws ServletException {
        if (name == null) {
            return null;
        }
        try {
            return (Class<T>) Class.forName(name.trim());
        } catch (ClassNotFoundException e) {
            throw new ServletException("Could not load service class " + name.trim(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] methodPath = getMethodPath(req);
        ServiceMethod serviceMethod = findServiceMethod(methodPath);
        if (serviceMethod == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            try {
                Map<String, Object> arguments = getArguments(req, serviceMethod);
                Object result = serviceMethod.invoke(injector, arguments);
                writeResponse(resp, result);
            } catch (IllegalArgumentException e) {
                LOG.error("Request was not allowed", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } catch (UnsupportedOperationException e) {
                LOG.error("Request was not allowed", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } catch (InvocationTargetException e) {
                LOG.error("Error while calling service", e);
                writeException(resp, e.getCause());
            }
        }
    }

    private void writeException(HttpServletResponse resp, Throwable exception) throws IOException {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        Map<String, Object> errorResponse = new HashMap<String, Object>();
        errorResponse.put("type", exception.getClass().getName());
        errorResponse.put("message", exception.getMessage());
        writeResponse(resp, errorResponse);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getArguments(HttpServletRequest req, ServiceMethod serviceMethod) throws IOException {
        String contentType = req.getContentType();
        String method = req.getMethod();
        if (POST.equals(method)) {
            if (contentType != null) {
                contentType = contentType.toLowerCase();
                if (contentType.startsWith(FORM_CONTENT_TYPE)) {
                    return deserializer.fromRequestParameters(req.getParameterMap(), serviceMethod.parameterTypes);
                } else if (contentType.startsWith(JSON_CONTENT_TYPE)) {
                    return deserializer.fromJsonInputStream(req.getInputStream(), serviceMethod.parameterTypes);
                }
            }
        } else if (GET.equals(method)) {
            if (contentType == null) {
                return deserializer.fromRequestParameters(req.getParameterMap(), serviceMethod.parameterTypes);
            }
        }

        throw new UnsupportedOperationException("Could not find an accepted method + content-type combination (" + method + ", " + contentType + ")");
    }

    private String[] getMethodPath(HttpServletRequest req) {
        String methodPath = req.getRequestURI().substring(req.getContextPath().length() + req.getServletPath().length());
        if (methodPath.startsWith("/")) {
            methodPath = methodPath.substring(1);
        }
        return methodPath.split("/");
    }

    private ServiceMethod findServiceMethod(String[] segments) {
        ServiceMethod serviceMethod;
        if (segments.length > 2) {
            return null;
        } else if (segments.length == 0) {
            serviceMethod = lookup.find("", "");
        } else if (segments.length == 1) {
            serviceMethod = lookup.find(segments[0], "");
            if (serviceMethod == null) {
                serviceMethod = lookup.find("", segments[0]);
            }
        } else {
            serviceMethod = lookup.find(segments[0], segments[1]);
        }
        return serviceMethod;
    }

    private void writeResponse(HttpServletResponse resp, Object response) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(resp.getOutputStream());
        resp.setContentType(JSON_CONTENT_TYPE);
        try {
            outputEncoder.toJson(response, writer);
        } finally {
            writer.close();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }
}
