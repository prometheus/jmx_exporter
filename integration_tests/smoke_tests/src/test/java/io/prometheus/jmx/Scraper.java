package io.prometheus.jmx;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Assert;

import java.util.Arrays;
import java.util.List;

public class Scraper {

    private final OkHttpClient client;
    private final String metricsUrl;

    public Scraper(String host, int port) {
        metricsUrl = "http://" + host + ":" + port + "/metrics";
        client = new OkHttpClient();
    }

    public List<String> scrape(long timeoutMillis) {
        long start = System.currentTimeMillis();
        Exception exception = null;
        while (System.currentTimeMillis() - start < timeoutMillis) {
            try {
                Request request = new Request.Builder()
                        .header("Accept", "application/openmetrics-text; version=1.0.0; charset=utf-8")
                        .url(metricsUrl)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    return Arrays.asList(response.body().string().split("\\n"));
                }
            } catch (Exception e) {
                exception = e;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
        if (exception != null) {
            exception.printStackTrace();
        }
        Assert.fail("Timeout while getting metrics from " + metricsUrl + " (orig port: " + 9000 + ")");
        return null; // will not happen
    }
}
