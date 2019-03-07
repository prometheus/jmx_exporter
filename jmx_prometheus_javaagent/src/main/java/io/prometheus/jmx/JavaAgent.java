package io.prometheus.jmx;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

public class JavaAgent {

	static final String DEFAULT_HOST = "0.0.0.0";

	private static final String IPV6_REGEX = "((\\[.*\\]+):)";
	private static final String DNS_IPV4_REGEX = "(((\\w+\\.)+\\w+):)";
	private static final String PORT_REGEX = "(([0-9]+):)";
	private static final String PATH_REGEX = "(.*)";

	private static final Pattern IPV6_PATTERN = Pattern.compile("^" + IPV6_REGEX + PORT_REGEX + PATH_REGEX);
	private static final Pattern PORT_PATTERN = Pattern.compile("^" + PORT_REGEX + PATH_REGEX);
	private static final Pattern DNS_IPV4_PATTERN = Pattern.compile("^" + DNS_IPV4_REGEX + PORT_REGEX + PATH_REGEX);

	static HTTPServer server;

	public static void agentmain(String agentArgument, Instrumentation instrumentation) throws Exception {
		premain(agentArgument, instrumentation);
	}

	public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {

		AgentParameters params = parseParameters(agentArgument);
		InetSocketAddress socket = new InetSocketAddress(params.getHost(), params.getPort());
		new BuildInfoCollector().register();
		new JmxCollector(new File(params.getSettingsFilePath())).register();
		DefaultExports.initialize();
		server = new HTTPServer(socket, CollectorRegistry.defaultRegistry, true);
	}

	static AgentParameters parseParameters(String agentArguments) {

		Matcher ipv6Matcher = IPV6_PATTERN.matcher(agentArguments);
		Matcher portMatcher = PORT_PATTERN.matcher(agentArguments);
		Matcher dnsMatcher = DNS_IPV4_PATTERN.matcher(agentArguments);

		try {
			if (ipv6Matcher.find()) {

				String host = ipv6Matcher.group(2);
				int port = Integer.parseInt(ipv6Matcher.group(4));
				String settingsFile = ipv6Matcher.group(5);
				return new AgentParameters(host, port, settingsFile);
			} else if (portMatcher.find()) {

				int port = Integer.parseInt(portMatcher.group(2));
				String settingsFile = portMatcher.group(3);
				return new AgentParameters(DEFAULT_HOST, port, settingsFile);
			} else if (dnsMatcher.find()) {

				String host = dnsMatcher.group(2);
				int port = Integer.parseInt(dnsMatcher.group(5));
				String settingsFile = dnsMatcher.group(6);
				return new AgentParameters(host, port, settingsFile);
			} else {
				throw new IllegalArgumentException("Could not parse arguments: " + agentArguments);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Usage: -javaagent:/path/to/JavaAgent.jar=[host]:<port>:<yaml configuration file>", e);
		}
	}
}
