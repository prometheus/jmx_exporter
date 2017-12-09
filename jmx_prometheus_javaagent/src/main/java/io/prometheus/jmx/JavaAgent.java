package io.prometheus.jmx;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

public class JavaAgent {

   static HTTPServer server;

   public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
     String[] args = agentArgument.split(":");
     final boolean isWindows = isWindows();
     final int winAdditive = isWindows? 1: 0;
     if (args.length < 2 + winAdditive || args.length > 3 + winAdditive) {
       System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration file>");
       System.exit(1);
     }

     int port;
     String file;
     InetSocketAddress socket;

     if (args.length == 3 + winAdditive) {
       port = Integer.parseInt(args[1]);
       socket = new InetSocketAddress(args[0], port);
       file = args[2] + (winAdditive == 0? "": ":" + args[3]);
     } else {
       port = Integer.parseInt(args[0]);
       socket = new InetSocketAddress(port);
       file = args[1] + (winAdditive == 0? "" : ":" + args[2]);
     }

     new JmxCollector(new File(file)).register();
     DefaultExports.initialize();
     server = new HTTPServer(socket, CollectorRegistry.defaultRegistry, true);
   }

   private static boolean isWindows()
   {
       String os = System.getProperty("os.name");
       return os != null && os.startsWith("Windows");
   }
}
