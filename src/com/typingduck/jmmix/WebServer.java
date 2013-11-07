package com.typingduck.jmmix;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Integer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;


/**
 * Simple webserver for exposing the values exported from JmxScraper.java.
 */
public class WebServer
{
    public static void main(String[] args) throws Exception
    {
        if (args.length < 2) {
            System.err.println("Usage: WebServer <open-port>");
            System.err.println("Usage: WebServer <open-port> <target-host:port>");
            System.exit(1);
        }
        int port;
        String target;

        if (args.length < 3) {
            port = Integer.parseInt(args[1]);
            target = null;
        } else {
            port = Integer.parseInt(args[1]);
            target = args[2];
        }

        Server server = new Server(port);
        server.setHandler(new MetricsHandler(target));
        server.start();
        server.join();
    }
}


/**
  * Servlet that scrapes JMX attributes from target and expose as JSON.
  * Target can either be passed in at contruction or as a query parameter.
  */
class MetricsHandler extends AbstractHandler {
    String jmx_target;

    public MetricsHandler(String jmx_target) {
        this.jmx_target = jmx_target;
    }

    public void handle(String target,
                       HttpServletRequest request, HttpServletResponse response,
                       int dispatch)
            throws IOException, ServletException {

        Request base_request = (request instanceof Request) ?
                (Request)request :
                HttpConnection.getCurrentConnection().getRequest();
        base_request.setHandled(true);
        String trgt = jmx_target;
        if (null != request.getParameter("target")) {
            trgt = request.getParameter("target");
        }
        if (trgt == null) {
            throw new ServletException("Need to supply a target=host:port query parameter");
        }

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("X-Prometheus-API-Version", "0.0.2");  // Prometheus specific
        PrintWriter io = response.getWriter();
        io.println("[");
        try {
            PrometheusBeanFormatter formatter = new PrometheusBeanFormatter(io);
            new JmxScraper(formatter).doScrape(trgt);
            formatter.printJsonFormat("com.jmmx", "scraped", 1);
            io.flush();
        } catch (Exception e) {
            System.err.println("err:" + e);
            io.println("{ \"error\": \"" + e.toString().replace("\n", " ").replace("\t", " ") + "\"}");
            response.setStatus(500);
        }
        io.println("]");
    }
}
