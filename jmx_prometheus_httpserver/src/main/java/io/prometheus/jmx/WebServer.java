package io.prometheus.jmx;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.jmx.common.http.ConfigurationException;
import io.prometheus.jmx.common.http.HTTPServerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WebServer {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd | HH:mm:ss.SSS", Locale.getDefault());

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: WebServer <[hostname:]port> <yaml configuration file>");
            System.exit(1);
        }

        InetSocketAddress socket;
        int colonIndex = args[0].lastIndexOf(':');

        if (colonIndex < 0) {
            int port = Integer.parseInt(args[0]);
            socket = new InetSocketAddress(port);
        } else {
            int port = Integer.parseInt(args[0].substring(colonIndex + 1));
            String host = args[0].substring(0, colonIndex);
            socket = new InetSocketAddress(host, port);
        }

        new BuildInfoCollector().register();
        new JmxCollector(new File(args[1]), JmxCollector.Mode.STANDALONE).register();

        try {
            new HTTPServerFactory().createHTTPServer(socket, CollectorRegistry.defaultRegistry, false, new File(args[1]));
        } catch (ConfigurationException e) {
            System.err.println("Configuration Exception : " + e.getMessage());
            System.exit(1);
        }

        System.out.println(
                String.format("%s | %s | INFO | %s | %s",
                        SIMPLE_DATE_FORMAT.format(new Date()),
                        Thread.currentThread().getName(),
                        WebServer.class.getName(),
                        "Running"));
    }
}
