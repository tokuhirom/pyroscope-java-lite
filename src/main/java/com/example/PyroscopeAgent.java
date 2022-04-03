package com.example;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.stream.Collectors;

class PyroscopeAgent {
    private final HttpUrl httpUrl;
    private final String applicationName;
    private final PyroscopeAggregator pyroscopeAggregator;
    private final long sleepIntervalMillis;
    private final long uploadIntervalMillis;
    private final OkHttpClient okhttpClient;
    private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");

    private static final Logger logger = LoggerFactory.getLogger(PyroscopeAgent.class);

    public PyroscopeAgent(
            String url,
            String applicationName,
            Duration sleepInterval,
            Duration uploadInterval,
            Duration connectTimeout,
            Duration readTimeout,
            Duration writeTimeout
    ) {
        this.httpUrl = HttpUrl.parse(url);
        this.applicationName = applicationName;
        this.pyroscopeAggregator = new PyroscopeAggregator();
        this.sleepIntervalMillis = sleepInterval.toMillis();
        this.uploadIntervalMillis = uploadInterval.toMillis();

        this.okhttpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .writeTimeout(writeTimeout)
                .build();
    }

    public void start() {
        this.startAggregationThread();
        this.startSnapshotThread();
    }

    @SuppressWarnings("BusyWait")
    private void startSnapshotThread() {
        Thread thread = new Thread(() -> {
            long nextSleep = uploadIntervalMillis;

            while (true) {
                try {
                    Thread.sleep(nextSleep);

                    long start = System.currentTimeMillis();

                    PyroscopeAggregator.Snapshot snapshot = pyroscopeAggregator.snapshot();
                    String content = snapshot.getSnapshot()
                            .entrySet()
                            .stream()
                            .map(
                                    it -> it.getKey() + " " + it.getValue()
                            ).collect(Collectors.joining("\n"));

                    HttpUrl url = this.httpUrl
                            .newBuilder()
                            .addPathSegment("ingest")
                            .addQueryParameter("name", applicationName)
                            .addQueryParameter("from", String.valueOf(snapshot.getFrom()))
                            .addQueryParameter("until", String.valueOf(snapshot.getUntil()))
                            .build();
                    Request request = new Request.Builder()
                            .url(url)
                            .post(RequestBody.create(content, OCTET_STREAM))
                            .build();
                    Response response = this.okhttpClient.newCall(request)
                            .execute();

                    logger.info("Sending request: url={} content={}bytes", url, content.length());
                    ResponseBody body = response.body();
                    logger.info(
                            "Sent: url={} code={} body={}",
                            url,
                            response.code(),
                            body == null ? "(null)" : body.string());

                    long elapsed = System.currentTimeMillis() - start;

                    nextSleep = Math.max(uploadIntervalMillis - elapsed, 0);
                } catch (Exception e) {
                    System.out.println("Got error: " + e.getMessage());
                }
            }
        });
        thread.setName("pyroscope-java-lite-uploader");
        thread.setDaemon(true);
        thread.start();
    }

    public void startAggregationThread() {
        Thread aggregationThread = new Thread(() -> {
            while (true) {
                try {
                    pyroscopeAggregator.aggregate();

                    Thread.sleep(sleepIntervalMillis);
                } catch (Exception e) {
                    System.out.println("Got error: " + e.getMessage());
                }
            }
        });
        aggregationThread.setName("pyroscope-java-lite-aggregator");
        aggregationThread.setDaemon(true);
        aggregationThread.start();
    }
}
