package se.grunka.jeeves;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JeevesServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(JeevesServlet.class);
    private static final String SERVICES_PARAMETER = "services";
    private static final String MODULE_PARAMETER = "module";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final int BUFFER_SIZE = 8192;
    private static final String UTF8 = "UTF-8";
    private Injector injector = null;
    private final Gson gson = new Gson();
    private final ArgumentDeserializer deserializer = new ArgumentDeserializer();

    private final ServiceMethodIndexer indexer = new ServiceMethodIndexer();
    private final Map<String, ServiceMethod> serviceMethodIndex = new HashMap<String, ServiceMethod>();

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
            try {
                indexer.updateIndex(serviceMethodIndex, Class.forName(serviceType));
            } catch (ClassNotFoundException e) {
                throw new ServletException("Could not find class " + serviceType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Module[] getModule(ServletConfig config) throws ServletException {
        String moduleClassName = config.getInitParameter(MODULE_PARAMETER);
        if (moduleClassName == null) {
            return new Module[0];
        }
        try {
            Class<Module> moduleType = (Class<Module>) Class.forName(moduleClassName);
            return new Module[]{moduleType.newInstance()};
        } catch (ClassNotFoundException e) {
            throw new ServletException("Could not load module class " + moduleClassName);
        } catch (InstantiationException e) {
            throw new ServletException("Could not instantiate module " + moduleClassName, e);
        } catch (IllegalAccessException e) {
            throw new ServletException("Was not allowed to instantiate module " + moduleClassName, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Response response = getResponse(req, resp);
        byte[] content = gson.toJson(response.content).getBytes(UTF8);
        resp.setStatus(response.status);
        resp.setContentType(JSON_CONTENT_TYPE);
        ServletOutputStream output = resp.getOutputStream();
        try {
            output.write(content);
            output.flush();
        } catch (IOException e) {
            LOG.error("Failed to write response", e);
        } finally {
            output.close();
        }
    }

    private Response getResponse(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String contentType = req.getContentType();
        if (contentType != null && !contentType.equals(JSON_CONTENT_TYPE)) {
            LOG.warn("Request content type not allowed " + contentType);
            return new Response(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, new Message("Unsupported content type"));
        }
        String methodPath = req.getRequestURI().substring(req.getContextPath().length() + req.getServletPath().length());
        ServiceMethod serviceMethod = serviceMethodIndex.get(methodPath);
        if (serviceMethod == null) {
            LOG.warn("Request for unknown path " + methodPath);
            return new Response(HttpServletResponse.SC_NOT_FOUND, new Message("Method not found"));
        }
        String content = readFullyAndClose(req.getInputStream());
        Map<String, Object> arguments = deserializer.fromJson(content, serviceMethod.parameterTypes);
        if (arguments == null) {
            LOG.warn("Could not parse arguments for " + methodPath);
            LOG.debug(content);
            return new Response(HttpServletResponse.SC_BAD_REQUEST, new Message("Bad parameters"));
        }
        MethodDetails methodDetails = serviceMethod.methodDetails.get(arguments.keySet());
        if (methodDetails == null) {
            LOG.warn("Could not find matching method " + methodPath + " " + arguments.keySet());
            return new Response(HttpServletResponse.SC_NOT_FOUND, new Message("No matching method found"));
        }
        Object[] orderedArguments = new Object[methodDetails.argumentOrder.length];
        for (int argument = 0; argument < orderedArguments.length; argument++) {
            orderedArguments[argument] = arguments.get(methodDetails.argumentOrder[argument]);
        }
        Object serviceInstance = injector.getInstance(methodDetails.service);
        try {
            Object result = methodDetails.method.invoke(serviceInstance, orderedArguments);
            return new Response(HttpServletResponse.SC_OK, result);
        } catch (IllegalAccessException e) {
            LOG.error("Not allowed to call method", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new Response(HttpServletResponse.SC_BAD_REQUEST, new Message("Not allowed"));
        } catch (InvocationTargetException e) {
            LOG.error("Error while calling service", e);
            Throwable cause = e.getCause();
            return new Response(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, new Message(cause.getClass().getName(), cause.getMessage()));
        }
    }

    private String readFullyAndClose(InputStream input) throws IOException {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while ((bytes = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytes);
            }
            return output.toString(UTF8);
        } finally {
            input.close();
        }
    }
}
