package se.grunka.jeeves.io;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.grunka.jeeves.Message;
import se.grunka.jeeves.ObjectStreamer;

public class HttpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);
    private static final ObjectStreamer STREAMER = new ObjectStreamer();
    private static final int CONNECTION_TIMEOUT = 10000;

    public Response request(URL url, Object content, Type returnType) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            prepareConnection(connection);
            try {
                STREAMER.write(connection.getOutputStream(), content);
            } catch (ConnectException e) {
                LOGGER.warn("Could not open socket", e);
                return new Response(-1, "Could not open socket");
            }
            return readResponse(connection, returnType);
        } finally {
            connection.disconnect();
        }
    }

    private Response readResponse(HttpURLConnection connection, Type returnType) throws IOException {
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
            returnType = Message.class;
        }
        return new Response(responseCode, STREAMER.read(input, returnType));
        //TODO handle json parse exceptions
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
}
