package se.grunka.jeeves;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    private static class ArgumentContainer {
        public final Map<String, Object> arguments = new HashMap<String, Object>();
    }
}
