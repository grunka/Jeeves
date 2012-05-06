package se.grunka.jeeves;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class ObjectStreamer {
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static final Gson GSON = new Gson();

    public void write(OutputStream output, Object value) throws IOException {
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(output, UTF8_CHARSET));
            try {
                GSON.toJson(value, writer);
            } finally {
                writer.close();
            }
        } finally {
            output.close();
        }
    }

    public <T> T read(InputStream input, Type responseType) throws IOException {
        try {
            Reader reader = new BufferedReader(new InputStreamReader(input, UTF8_CHARSET));
            try {
                return GSON.fromJson(reader, responseType);
            } catch (JsonParseException e) {
                return null;
            } finally {
                reader.close();
            }
        } finally {
            input.close();
        }
    }
}
