package se.grunka.jeeves;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private static final ObjectStreamer STREAMER = new ObjectStreamer();
    private static final AnnotationProcessor ANNOTATION_PROCESSOR = new AnnotationProcessor();
    private Injector injector = null;
    private final ArgumentDeserializer deserializer = new ArgumentDeserializer();

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
        for (String serviceTypeName : services.split(",")) {
            final Class<?> serviceType;
            try {
                serviceType = Class.forName(serviceTypeName);
            } catch (ClassNotFoundException e) {
                throw new ServletException("Could not find class " + serviceTypeName);
            }
            ANNOTATION_PROCESSOR.processService(serviceType, new AnnotationProcessor.Callback() {
                @Override
                public void method(String path, Method method, String[] names, Class<?>[] types) {
                    ServiceMethod serviceMethod = serviceMethodIndex.get(path);
                    if (serviceMethod == null) {
                        serviceMethod = new ServiceMethod();
                        serviceMethodIndex.put(path, serviceMethod);
                    }
                    Set<String> namesSet = new HashSet<String>();
                    for (int i = 0; i < names.length; i++) {
                        namesSet.add(names[i]);
                        serviceMethod.parameterTypes.put(names[i], types[i]);
                    }
                    serviceMethod.methodDetails.put(namesSet, new MethodDetails(serviceType, method, names));
                }
            });
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
        resp.setContentType(JSON_CONTENT_TYPE);
        String contentType = req.getContentType();
        if (contentType != null && !contentType.equals(JSON_CONTENT_TYPE)) {
            LOG.warn("Request content type not allowed " + contentType);
            resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            STREAMER.write(resp.getOutputStream(), new Message("Unsupported content type"));
        } else {
            String methodPath = req.getRequestURI().substring(req.getContextPath().length() + req.getServletPath().length());
            ServiceMethod serviceMethod = serviceMethodIndex.get(methodPath);
            if (serviceMethod == null) {
                LOG.warn("Request for unknown path " + methodPath);
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                STREAMER.write(resp.getOutputStream(), new Message("Method not found"));
            } else {
                ServletInputStream input = req.getInputStream();
                Map<String, Object> arguments;
                try {
                    arguments = deserializer.fromJson(input, serviceMethod.parameterTypes);
                } finally {
                    input.close();
                }
                if (arguments == null) {
                    LOG.warn("Could not parse arguments for " + methodPath);
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    STREAMER.write(resp.getOutputStream(), new Message("Bad parameters"));
                } else {
                    //TODO this lookup could probably be improved
                    MethodDetails methodDetails = serviceMethod.methodDetails.get(arguments.keySet());
                    if (methodDetails == null) {
                        LOG.warn("Could not find matching method " + methodPath + " " + arguments.keySet());
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        STREAMER.write(resp.getOutputStream(), new Message("No matching method found"));
                    } else {
                        Object[] orderedArguments = new Object[methodDetails.argumentOrder.length];
                        for (int argument = 0; argument < orderedArguments.length; argument++) {
                            orderedArguments[argument] = arguments.get(methodDetails.argumentOrder[argument]);
                        }
                        Object serviceInstance = injector.getInstance(methodDetails.service);
                        try {
                            Object result = methodDetails.method.invoke(serviceInstance, orderedArguments);
                            STREAMER.write(resp.getOutputStream(), result);
                        } catch (IllegalAccessException e) {
                            LOG.error("Not allowed to call method", e);
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            STREAMER.write(resp.getOutputStream(), new Message("Method not allowed"));
                        } catch (InvocationTargetException e) {
                            LOG.error("Error while calling service", e);
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            Throwable cause = e.getCause();
                            STREAMER.write(resp.getOutputStream(), new Message(cause.getClass().getName(), cause.getMessage()));
                        }
                    }
                }
            }
        }
    }

}
