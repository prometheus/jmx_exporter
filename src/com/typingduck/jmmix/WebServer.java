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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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
        if (args.length < 3 || !args[1].equals("-c")) {
            System.err.println("Usage: WebServer -c <json config>");
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

    public MetricsHandler(String jmx_target,
                          List<String> whitelist, List<String>blacklist) {
        this.jmx_target = jmx_target;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
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
            JmxScraper sc = new JmxScraper(formatter);
            sc.setWhitelist(whitelist);
            sc.setBlacklist(blacklist);
            sc.doScrape(trgt);
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



/*
Copyright (c) 2013 typingduck

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
