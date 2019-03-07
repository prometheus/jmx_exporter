package io.prometheus.jmx;

class AgentParameters {

	private final String hostName;
	private final int port;
	private final String settingsFilePath;

	AgentParameters(String hostName, int port, String settingsFilePath) {
		this.hostName = hostName;
		this.port = port;
		this.settingsFilePath = settingsFilePath;
	}

	String getHost() {
		return hostName;
	}

	int getPort() {
		return port;
	}

	String getSettingsFilePath() {
		return settingsFilePath;
	}
}
