package io.prometheus.jmx;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.regex.Pattern;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

public class JavaAgent {

    private static final Pattern PORT_PATTERN = Pattern.compile("^\\d{1,5}$");
   static HTTPServer server;

   public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
     String[] args = agentArgument.split(":");
     final boolean isWindows = isWindows();
     final int winAdditive = isWindows? 1: 0;
     if (args.length < 2 || args.length > 3 + winAdditive) {
       System.err.println(args.length + "Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration file>");
       System.exit(1);
     }

     int port;
     String file = null;
     InetSocketAddress socket = null;

     if(PORT_PATTERN.matcher(args[0]).matches())
     {
         port = Integer.parseInt(args[0]);
         socket = new InetSocketAddress(port);
         file = args[1] + (args.length < 3? "" : ":" + args[2]);
     }
     else if(PORT_PATTERN.matcher(args[1]).matches())
     {
       port = Integer.parseInt(args[1]);
       socket = new InetSocketAddress(args[0], port);
       file = args[2] + (args.length < 4? "" : ":" + args[3]);
     }
     else
     {
         System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration file>");
         System.exit(1);
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
