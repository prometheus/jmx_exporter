package io.prometheus.jmx;

import java.io.File;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.yaml.snakeyaml.Yaml;

import static io.prometheus.jmx.Config.loadConfig;

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

     File file = new File(args[1]);
     Config config = loadConfig((Map<String, Object>)new Yaml().load(new FileReader(file)), file);
     new JmxCollector(config, JmxCollector.Mode.STANDALONE).register();

     int minThreads = config.minThreads == null ? 5 : config.minThreads;
     int maxThreads = config.maxThreads == null ? 5 : config.maxThreads;
     new HTTPServer.Builder()
       .withInetSocketAddress(socket)
       .withRegistry(CollectorRegistry.defaultRegistry)
       .withDaemonThreads(true)
       .withExecutorService(new ThreadPoolExecutor(minThreads, maxThreads, 0L,
         TimeUnit.MILLISECONDS, new LinkedBlockingQueue(),
         NamedDaemonThreadFactory.defaultThreadFactory(true)))
       .build();
   }
}
