package io.prometheus.jmx;

import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.File;
import java.net.InetSocketAddress;

public class WebServer {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: WebServer <[hostname:]port> <yaml configuration file>" );
            System.exit(1);
        }

        String keyStorePath = System.getProperty("keystore");
        String authConfigFile = System.getProperty("authConfig");

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

        JmxCollector jc = new JmxCollector(new File(args[1])).register();

        Server server = new Server(socket);

        //config SSL
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(keyStorePath);
        //passphrase password  - this is POC only, needs to be removed later
        sslContextFactory.setKeyStorePassword("qwerty");
        sslContextFactory.setKeyManagerPassword("qwerty");

        ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(https));
        sslConnector.setPort(port);
        server.setConnectors(new Connector[] { sslConnector });

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        //add basic HTTP auth (user/pass)
        context.setSecurityHandler(basicAuth("jcgrealm", authConfigFile));
        server.setHandler(context);

        context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
        server.start();
        server.join();
    }

	private static final SecurityHandler basicAuth(String realm, String configFile) {

		HashLoginService hashLoginService = new HashLoginService();
		hashLoginService.setConfig(configFile);
		hashLoginService.setName(realm);

		Constraint constraint = new Constraint();
		constraint.setName(Constraint.__BASIC_AUTH);
		constraint.setRoles(new String[]{"admin"});
		constraint.setAuthenticate(true);

		ConstraintMapping constraintMapping = new ConstraintMapping();
		constraintMapping.setConstraint(constraint);
		constraintMapping.setPathSpec("/*");

		ConstraintSecurityHandler constraintSecurityHandler = new ConstraintSecurityHandler();
		constraintSecurityHandler.setAuthenticator(new BasicAuthenticator());
		constraintSecurityHandler.setRealmName(realm);
		constraintSecurityHandler.addConstraintMapping(constraintMapping);
		constraintSecurityHandler.setLoginService(hashLoginService);

		return constraintSecurityHandler;

	}
}
