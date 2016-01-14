package io.prometheus.jmx;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileReader;

import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class WebServer {

   public static final FilenameFilter FILENAME_FILTER = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
         return name.endsWith(".yml") || name.endsWith(".yaml");
      }
   };

   public static void main(String[] args) throws Exception {
     if (args.length < 2) {
       System.err.println("Usage: WebServer <port> <yaml configuration files directory>");
       System.exit(1);
     }

     File configDirectory = new File(args[1]);

     if (!configDirectory.exists()) {
        System.err.println("Configuration directory does not exist: [" + configDirectory.getPath() + "]");
        System.exit(1);
     }

     File[] files = configDirectory.listFiles(FILENAME_FILTER);

     if (files.length == 0) {
        System.err.println("No configuration files found in [" + configDirectory.getPath() + "]");
        System.exit(1);
     }

     for (File file : files) {
        new JmxCollector(new FileReader(file)).register();
     }

     int port = Integer.parseInt(args[0]);
     Server server = new Server(port);
     ServletContextHandler context = new ServletContextHandler();
     context.setContextPath("/");
     server.setHandler(context);
     context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
     server.start();
     server.join();
   }
}
