package io.prometheus.jmx;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.io.FileReader;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.management.MalformedObjectNameException;

public class JavaAgent {
   static Server server;

   static JmxCollector previousJmxCollector = null;

  /**
   * This will load the config located at @configPath and replace the previous one.
   * @param configPath
   * @throws IOException
   */
   public static void loadConfig(String configPath) throws IOException {
     if(previousJmxCollector != null) {
       CollectorRegistry.defaultRegistry.unregister(previousJmxCollector);
     }
     final FileReader in = new FileReader(configPath);
     try {
       previousJmxCollector = new JmxCollector(in).register();
     } catch (MalformedObjectNameException e) {
       e.printStackTrace();
     }
   }

  /**
   * This tries to create a watch service for the filesystem's path.
   * @param path
   * @return
   */
   public static WatchService getWatchServiceForPath(Path path) {
     final FileSystem fileSystem = path.getFileSystem();
     WatchService watchService = null;
     try {
       watchService = fileSystem.newWatchService();
       System.out.println("Succcessfuly created a watcher");
     } catch (IOException e) {
       System.out.println("Unable to create a WatchService for this FileSystem");
       e.printStackTrace();
     }
     return watchService;
   }

  /**
   * This creates a thread that will poll for filesystem changes in the background and triggers a reload of the
   * configuration.
   * @param configPath
   * @param stopLatch the stop latch to trigger a shutdown
   * @return the watcher thread if the filesystem enables it or an empty thread
   * @throws IOException
   */
   public static Thread getConfigReloaderThread(final String configPath, final CountDownLatch stopLatch) throws IOException {
     final File configFile = new File(configPath);
     final Path path = configFile.toPath();
     final WatchService watchService = getWatchServiceForPath(path);
     loadConfig(configPath);
     if(watchService != null) {
       path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
       final Thread configReloader = new Thread(new Runnable() {
         public void run() {
           while(stopLatch.getCount() > 0){
             try {
               final WatchKey poll = watchService.poll(1, TimeUnit.SECONDS);
               if(poll != null){
                 final List<WatchEvent<?>> watchEvents = poll.pollEvents();
                 for (WatchEvent<?> evt : watchEvents) {
                   final WatchEvent<Path> event = (WatchEvent<Path>) evt;
                   if(path.getParent().resolve(event.context()).equals(path) &&
                           event.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY) ||
                           event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)
                           ){
                     loadConfig(configPath);
                     break;
                   }
                 }
               }
             } catch (IOException e) {
               e.printStackTrace();
             } catch (InterruptedException e) {
               e.printStackTrace();
             }
           }
         }
       });
       return configReloader;
     }
     return new Thread();
   }

   public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
     String[] args = agentArgument.split(":");
     if (args.length != 2) {
       System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=<port>:<yaml configuration file>");
       System.exit(1);
     }
     final CountDownLatch stopLatch = new CountDownLatch(1);
     Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
       public void run() {
         stopLatch.countDown();
       }
     }));
     getConfigReloaderThread(args[1], stopLatch).start();
     DefaultExports.initialize();

     int port = Integer.parseInt(args[0]);
     server = new Server(port);
     QueuedThreadPool pool = new QueuedThreadPool();
     pool.setDaemon(true);
     server.setThreadPool(pool);
     ServletContextHandler context = new ServletContextHandler();
     context.setContextPath("/");
     server.setHandler(context);
     context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
     server.start();
   }
}
