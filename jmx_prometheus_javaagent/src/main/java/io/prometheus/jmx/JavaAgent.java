package io.prometheus.jmx;

import java.io.File;
import java.lang.instrument.Instrumentation;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

public class JavaAgent {
   static HTTPServer server;

   public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
     String[] args = agentArgument.split(":");
     if (args.length < 2 || args.length > 3) {
       System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration file>");
       System.exit(1);
     }

     int port;
     String file;

     if (args.length == 3) {
       port = Integer.parseInt(args[1]);
       file = args[2];
     } else {
       port = Integer.parseInt(args[0]);
       file = args[1];
     }

     new JmxCollector(new File(file)).register();
     DefaultExports.initialize();

     server = new HTTPServer(port);
   }
}
