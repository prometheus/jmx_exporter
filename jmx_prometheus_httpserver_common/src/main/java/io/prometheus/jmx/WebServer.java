package io.prometheus.jmx;

import java.io.File;
import java.net.InetSocketAddress;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;

public class WebServer {

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
     new HTTPServer(socket, CollectorRegistry.defaultRegistry);
   }
}
