package io.prometheus.jmx;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;

public class WebServer extends HTTPServer {

  public WebServer(InetSocketAddress addr, CollectorRegistry registry) throws IOException {
    super(addr, registry);
    this.server.createContext("/health", new HttpHandler() {
      @Override
      public void handle(HttpExchange httpExchange) throws IOException {
        OutputStream response = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 2);
        response.write("ok".getBytes());
        response.flush();
        response.close();
      }
    });
  }

  public static void main(String[] args) throws Exception {
     if (args.length < 2) {
       System.err.println("Usage: WebServer <[hostname:]port> <yaml configuration file>");
       System.exit(1);
     }

     String[] hostnamePort = args[0].split(":");
     int port;
     InetSocketAddress socket;
     
     if (hostnamePort.length == 2) {
       port = Integer.parseInt(hostnamePort[1]);
       socket = new InetSocketAddress(hostnamePort[0], port);
     } else {
       port = Integer.parseInt(hostnamePort[0]);
       socket = new InetSocketAddress(port);
     }

     new BuildInfoCollector().register();
     new JmxCollector(new File(args[1])).register();
     new WebServer(socket, CollectorRegistry.defaultRegistry);
   }
}
