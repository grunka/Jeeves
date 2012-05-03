package se.grunka.jeeves;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class JeevesServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(JeevesServlet.class);
    private static final String SERVICES_PARAMETER = "services";
    private static final String MODULE_PARAMETER = "module";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String POST = "POST";
    private static final String GET = "GET";
    private Injector injector = null;
    private final Gson outputEncoder = new Gson();
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
        String contentType = req.getContentType();
        if (contentType != null && !contentType.equals(JSON_CONTENT_TYPE)) {
            LOG.warn("Request content type not allowed " + contentType);
            resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } else {
            String methodPath = req.getRequestURI().substring(req.getContextPath().length() + req.getServletPath().length());
            ServiceMethod serviceMethod = serviceMethodIndex.get(methodPath);
            if (serviceMethod == null) {
                LOG.warn("Request for unknown path " + methodPath);
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else {
                String content = getContent(req);
                Map<String, Object> arguments = deserializer.fromJson(content, serviceMethod.parameterTypes);
                if (arguments == null) {
                    LOG.warn("Could not parse arguments for " + methodPath);
                    LOG.debug(content);
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                } else {
                    MethodDetails methodDetails = serviceMethod.methodDetails.get(arguments.keySet());
                    if (methodDetails == null) {
                        LOG.warn("Could not find matching method " + methodPath + " " + arguments.keySet());
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    } else {
                        Object[] orderedArguments = new Object[methodDetails.argumentOrder.length];
                        for (int argument = 0; argument < orderedArguments.length; argument++) {
                            orderedArguments[argument] = arguments.get(methodDetails.argumentOrder[argument]);
                        }
                        Object serviceInstance = injector.getInstance(methodDetails.service);
                        try {
                            Object result = methodDetails.method.invoke(serviceInstance, orderedArguments);
                            writeResponse(resp, result);
                        } catch (IllegalAccessException e) {
                            LOG.error("Not allowed to call method" ,e);
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        } catch (InvocationTargetException e) {
                            LOG.error("Error while calling service", e);
                            writeException(resp, e.getCause());
                        }
                    }
                }
            }
        }
    }

    private String getContent(HttpServletRequest req) throws IOException {
        InputStream input = req.getInputStream();
        try {
            byte[] buffer = new byte[4096];
            int bytes;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            String content = "";
            while ((bytes = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytes);
                content += new String(buffer, 0, bytes);
            }
            return content;
        } finally {
            input.close();
        }
    }

    private void writeException(HttpServletResponse resp, Throwable exception) throws IOException {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        Map<String, String> errorResponse = new HashMap<String, String>();
        errorResponse.put("type", exception.getClass().getName());
        errorResponse.put("message", exception.getMessage());
        writeResponse(resp, errorResponse);
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
}
