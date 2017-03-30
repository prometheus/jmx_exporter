package io.prometheus.jmx;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.jmx.configuration.Configuration;
import io.prometheus.jmx.configuration.InputArgumentsLoader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;
import java.net.InetSocketAddress;

public class WebServer {

  private static final String CONTEXT_PATH = "/";

  public static void main(String[] args) throws Exception {

    Configuration config = new InputArgumentsLoader()
            .load(args);

    InetSocketAddress socket;

    if (config.hasHostname()) {
      socket = new InetSocketAddress(config.obtainHostname(), config.obtainPort());
    } else {
      socket = new InetSocketAddress(config.obtainPort());
    }

    JmxCollector jc = new JmxCollector(new File(config.obtainConfigFilePath())).register();

    Server server = new Server(socket);
    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath(CONTEXT_PATH);
    server.setHandler(context);
    context.addServlet(new ServletHolder(new MetricsServlet()), config.obtainPath());
    server.start();
    server.join();

  }

}
