package io.prometheus.jmx;

import java.io.File;

import io.prometheus.client.exporter.HTTPServer;

public class WebServer {

   public static void main(String[] args) throws Exception {
     if (args.length < 2) {
       System.err.println("Usage: WebServer <[hostname:]port> <yaml configuration file>");
       System.exit(1);
     }

     String[] hostnamePort = args[0].split(":");
     int port;

     if (hostnamePort.length == 2) {
       port = Integer.parseInt(hostnamePort[1]);
     } else {
       port = Integer.parseInt(hostnamePort[0]);
     }

     JmxCollector jc = new JmxCollector(new File(args[1])).register();
     HTTPServer server = new HTTPServer(port);
   }
}
