package se.grunka.jeeves.io;

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
    private static final Charset CHARSET = Charset.forName("UTF-8");

    public Response post(URL url, String content) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            prepareConnection(connection);

            try {
                writeRequest(content, connection);
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
        if (responseCode == 200) {
            InputStream input = connection.getInputStream();
            try {
                return new Response(responseCode, read(input));
            } finally {
                input.close();
            }
        } else {
            InputStream error = connection.getErrorStream();
            try {
                return new Response(responseCode, read(error));
            } finally {
                error.close();
            }
        }
    }

    private void writeRequest(String content, HttpURLConnection connection) throws IOException {
        OutputStream output = connection.getOutputStream();
        try {
            output.write(content.getBytes(CHARSET));
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
        connection.setConnectTimeout(10000);
    }

    private String read(InputStream input) throws IOException {
        byte[] buffer = new byte[4096];
        String result = "";
        int bytes;
        while ((bytes = input.read(buffer)) != -1) {
            result += new String(buffer, 0, bytes);
        }
        return result;
    }
}
