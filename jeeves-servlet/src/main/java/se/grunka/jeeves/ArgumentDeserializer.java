package se.grunka.jeeves;

import com.google.gson.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

class ArgumentDeserializer {
    private final ThreadLocal<Map<String, Class<?>>> currentParameterTypes = new ThreadLocal<Map<String, Class<?>>>();
    private final Gson argumentParser = new GsonBuilder().registerTypeAdapter(ArgumentContainer.class, new JsonDeserializer<ArgumentContainer>() {
        @Override
        public ArgumentContainer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ArgumentContainer result = new ArgumentContainer();
            Map<String, Class<?>> parameterTypes = currentParameterTypes.get();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                String argument = entry.getKey();
                Class<?> parameterType = parameterTypes.get(argument);
                if (parameterType == null) {
                    throw new JsonParseException("Unrecognized parameter " + argument);
                }
                result.arguments.put(argument, context.deserialize(entry.getValue(), parameterType));
            }
            return result;
        }
    }).create();

    public Map<String, Object> fromJsonInputStream(InputStream inputStream, Map<String, Class<?>> parameterTypes) throws IOException {
        InputStreamReader reader = new InputStreamReader(inputStream);
        currentParameterTypes.set(parameterTypes);
        try {
            ArgumentContainer argumentContainer = argumentParser.fromJson(reader, ArgumentContainer.class);
            if (argumentContainer == null) {
                return Collections.emptyMap();
            } else {
                return argumentContainer.arguments;
            }
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Unable to parse arguments", e);
        } finally {
            reader.close();
            currentParameterTypes.remove();
        }
    }

    public Map<String, Object> fromRequestParameters(Map<String, String[]> parameterMap, Map<String, Class<?>> parameterTypes) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Map.Entry<String, String[]> entry : new TreeMap<String, String[]>(parameterMap).entrySet()) {
            String key = entry.getKey();
            Object value;
            if (key.endsWith("[]")) {
                key = key.substring(0, key.length() - 2);
                value = entry.getValue();
            } else {
                value = entry.getValue()[0];
            }
            int firstNameEnd = key.indexOf('[');
            if (firstNameEnd == -1) {
                result.put(key, value);
            } else {
                String[] names = key.substring(firstNameEnd + 1, key.length() - 1).split("\\]\\[");
                String currentName = key.substring(0, firstNameEnd);
                Object current = result.get(currentName);
                String previousName = currentName;
                Object previous = result;
                for (String name : names) {
                    currentName = name;
                    if (current == null) {
                        if (isNumeric(currentName)) {
                            current = new ArrayList<Object>();
                        } else {
                            current = new HashMap<String, Object>();
                        }
                        addValue(previous, previousName, current);
                    }
                    previous = current;
                    previousName = currentName;
                    if (current instanceof Map) {
                        current = ((Map) current).get(currentName);
                    } else {
                        current = null;
                    }
                }
                addValue(previous, previousName, value);
            }
        }
        currentParameterTypes.set(parameterTypes);
        try {
            ArgumentContainer argumentContainer = argumentParser.fromJson(argumentParser.toJson(result), ArgumentContainer.class);
            return argumentContainer.arguments;
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Unable to parse arguments", e);
        } finally {
            currentParameterTypes.remove();
        }
    }

    @SuppressWarnings("unchecked")
    private void addValue(Object target, String name, Object value) {
        if (target instanceof Map) {
            ((Map) target).put(name, value);
        } else if (target instanceof List) {
            ((List) target).add(value);
        } else {
            throw new IllegalArgumentException("Bad state, target is " + (target == null ? "null" : target.getClass().getName()) + " for " + name);
        }
    }

    private boolean isNumeric(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

}
