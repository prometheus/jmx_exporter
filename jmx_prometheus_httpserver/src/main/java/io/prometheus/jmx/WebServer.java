package io.prometheus.jmx;

import java.io.File;
import java.net.InetSocketAddress;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

public class WebServer {

   public static void main(String[] args) throws Exception {
     if (args.length < 2) {
       System.err.println("Usage: WebServer <[hostname:]port> <yaml configuration file> <[tls]>");
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
     new JmxCollector(new File(args[1]), JmxCollector.Mode.STANDALONE).register();
     
     if (args[2].equals("tls"))  {
        HttpsServer httpsServer = HttpsServer.create(socket, 3);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        // to handle keystore types other than jks
        String kstype = System.getProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType());
        KeyStore ks = KeyStore.getInstance(kstype);
        char[] passphrase = System.getProperty("javax.net.ssl.keyStore.passphrase",
                    System.getProperty("javax.net.ssl.keyStorePassword")).toCharArray();
        ks.load(new FileInputStream(System.getProperty("javax.net.ssl.keyStore")), passphrase);
        kmf.init(ks, passphrase);
        sslContext.init(kmf.getKeyManagers(), null, null);
        SSLParameters sslParameters = sslContext.getDefaultSSLParameters();
        sslParameters.setProtocols(new String[] {"TLSv1.2"});
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        new HTTPServer(httpsServer, CollectorRegistry.defaultRegistry, false);
     } else {
        new HTTPServer(socket, CollectorRegistry.defaultRegistry);
     }
   }
}
