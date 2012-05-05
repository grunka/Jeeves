package se.grunka.jeeves.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static final int BUFFER_SIZE = 8192;
    private static final String UTF8 = "UTF-8";
    private static final int CONNECTION_TIMEOUT = 10000;

    public Response post(URL url, String content) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            prepareConnection(connection);
            try {
                writeRequest(content.getBytes(UTF8_CHARSET), connection);
            } catch (ConnectException e) {
                LOGGER.warn("Could not open socket", e);
                return new Response(-1, "Could not open socket");
            }
            return readResponse(connection);
        } finally {
            connection.disconnect();
        }
    }

    private Response readResponse(HttpURLConnection connection) throws IOException {
        int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException e) {
            LOGGER.warn("No response from server", e);
            return new Response(-2, "No response from server");
        }
        InputStream input;
        if (responseCode == 200) {
            input = connection.getInputStream();
        } else {
            input = connection.getErrorStream();
        }
        return new Response(responseCode, readFullyAndClose(input));
    }

    private void writeRequest(byte[] content, HttpURLConnection connection) throws IOException {
        OutputStream output = connection.getOutputStream();
        try {
            output.write(content);
            output.flush();
        } finally {
            output.close();
        }
    }

    private void prepareConnection(HttpURLConnection connection) {
        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            throw new Error("POST not supported, fix your JVM");
        }
        connection.setRequestProperty("Connection", "close");
        connection.setRequestProperty("Content-type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setDoOutput(true);
        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(0);
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
