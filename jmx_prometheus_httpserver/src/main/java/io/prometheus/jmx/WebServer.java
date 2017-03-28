package io.prometheus.jmx;

import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;
import java.net.InetSocketAddress;

public class WebServer {

  private static final String CONTEXT_PATH = "/";

  public static void main(String[] args) throws Exception {

    ConfigurationLoader loader = new ConfigurationLoader(args);
    Configuration config = loader.loadConfiguration();

    InetSocketAddress socket;

    if (config.hasHostname()) {
      socket = new InetSocketAddress(config.retrieveHostname(), config.retrievePort());
    } else {
      socket = new InetSocketAddress(config.retrievePort());
    }

    JmxCollector jc = new JmxCollector(new File(config.retrieveConfigFilePath())).register();

    Server server = new Server(socket);
    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath(CONTEXT_PATH);
    server.setHandler(context);
    context.addServlet(new ServletHolder(new MetricsServlet()), config.retrievePath());
    server.start();
    server.join();

  }

}
