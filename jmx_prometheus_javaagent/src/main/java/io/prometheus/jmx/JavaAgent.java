package io.prometheus.jmx;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.*;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import sun.reflect.annotation.ExceptionProxy;

public class JavaAgent {
   
   static HTTPServer server;

   public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
     String[] args = agentArgument.split(":");
     if (args.length < 2 || args.length > 4) {
       System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration file>:<exporter property file>");
       System.exit(1);
     }

     int port;
     String propertyFile;
     String file;
     InetSocketAddress socket;

     if (args.length == 4) {
       port = Integer.parseInt(args[1]);
       socket = new InetSocketAddress(args[0], port);
       file = args[2];
       propertyFile = args[3];
     } else {
       port = Integer.parseInt(args[0]);
       socket = new InetSocketAddress(port);
       file = args[1];
       propertyFile = args[2];
     }

     new JmxCollector(new File(file), propertyFile).register();
     DefaultExports.initialize();
     server = new HTTPServer(socket, CollectorRegistry.defaultRegistry);
   }
}
