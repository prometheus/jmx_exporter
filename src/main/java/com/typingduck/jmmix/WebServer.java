package com.typingduck.jmmix;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Integer;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple webserver for exposing the values exported from JmxScraper.java.
 */
public class WebServer {

    static Logger logger = Logger.getLogger("jmmix"); 

    public static void main(String[] args) throws Exception
    {
		logger.setLevel(Level.WARNING);
        if (args.length < 3 || !args[1].equals("-c")) {
            System.err.println("Usage: WebServer -c <jmmix json configuration file>");
            logger.log(Level.SEVERE, "Usage: WebServer -c <jmmix json configuration file>");
            System.exit(1);
        }

        JSONParser parser = new JSONParser();

        JSONObject config = (JSONObject)parser.parse(new FileReader(args[2]));

        int port = (int)(long)(Long)config.get("port");
        String target = null;
        if (config.containsKey("target")) {
            target = (String)config.get("target");
        }
        List<String> whitelist = new LinkedList<String>();
        if (config.containsKey("bean_whitelist")) {
            JSONArray beanRegexs = (JSONArray) config.get("bean_whitelist");
            for(Object beanRegex : beanRegexs) {
                whitelist.add((String)beanRegex);
            }
        }
        List<String> blacklist = new LinkedList<String>();
        if (config.containsKey("bean_blacklist")) {
            JSONArray beanRegexs = (JSONArray) config.get("bean_blacklist");
            for(Object beanRegex : beanRegexs) {
                blacklist.add((String)beanRegex);
            }
        }

        Server server = new Server(port);
        server.setHandler(new MetricsHandler(target, whitelist, blacklist));
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
    List<String> whitelist;
    List<String> blacklist;

    static Logger logger = Logger.getLogger("jmmix"); 

    public MetricsHandler(String jmx_target,
                          List<String> whitelist, List<String>blacklist) {
        this.jmx_target = jmx_target;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
    }

    public void handle(String target,
                           Request baseRequest,
                           HttpServletRequest request,
                           HttpServletResponse response) 
            throws IOException, ServletException {
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
        baseRequest.setHandled(true);
        PrintWriter json_out = response.getWriter();
        json_out.println("[");
        try {
            PrometheusBeanFormatter formatter = new PrometheusBeanFormatter(json_out);
            JmxScraper sc = new JmxScraper(formatter);
            sc.setWhitelist(whitelist);
            sc.setBlacklist(blacklist);
            sc.doScrape(trgt);
            formatter.printJsonFormat("com.jmmx", "scraped", 1);
            json_out.flush();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "err:" + e);
            json_out.println("{ \"error\": \"" + e.toString().replace("\n", " ").replace("\t", " ") + "\"}");
            response.setStatus(500);
        }
        json_out.println("]");
    }
}
