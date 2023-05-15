package io.prometheus.jmx;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;


public class JavaAgentMultiPortTest {
    static final String IFC = "0.0.0.0";
    static final String HOST = "some_host.dc";
    static final int RESTARTS = 3, minBackoff = 0, maxBackoff = 100;
    // hostname=localhost,portStart=1000,portEnd=1010,timeout=500,backoffMin=2000,backoffMax=4000,configFile=/some/path.yaml
    @Test(expected = Exception.class)
    public void testFailureOnOriginalConfig() {
        JavaAgentMultiPort.Config config1 = JavaAgentMultiPort.parseConfig("some_host.dc:111:./config_file.yaml");
    }
    @Test
    public void testConfigParsing() {
        JavaAgentMultiPort.Config config = JavaAgentMultiPort.parseConfig("hostname=some_host.dc,portStart=222,portEnd=333,timeout=500,backoffMin=2000,backoffMax=4000,configFile=/some/path.yaml");

        Assert.assertEquals(222, config.portStart);
        Assert.assertEquals(333, config.portEnd);
        Assert.assertEquals(500, config.timeout);
        Assert.assertEquals(2000, config.backoffMin);
        Assert.assertEquals(4000, config.backoffMax);
        Assert.assertEquals("/some/path.yaml", config.configFile);
        Assert.assertEquals(HOST, config.host);
        Assert.assertEquals(new InetSocketAddress(HOST, 222), config.socket);
    }


    @Test
    public void testMinimalConfigParsing() {
        JavaAgentMultiPort.Config config = JavaAgentMultiPort.parseConfig("portStart=222,portEnd=333,configFile=/some/path.yaml");

        Assert.assertEquals(222, config.portStart);
        Assert.assertEquals(333, config.portEnd);
        Assert.assertEquals(500, config.timeout);
        Assert.assertEquals(2000, config.backoffMin);
        Assert.assertEquals(4000, config.backoffMax);
        Assert.assertEquals("/some/path.yaml", config.configFile);
        Assert.assertNull(config.host);
        Assert.assertEquals(new InetSocketAddress(IFC, 222), config.socket);
    }

    @Test
    public void testPartialConfigParsing() {
        JavaAgentMultiPort.Config config = JavaAgentMultiPort.parseConfig("portStart=222,portEnd=333,backoffMin=500,configFile=/some/path.yaml");

        Assert.assertEquals(222, config.portStart);
        Assert.assertEquals(333, config.portEnd);
        Assert.assertEquals(500, config.timeout);
        Assert.assertEquals(500, config.backoffMin);
        Assert.assertEquals(4000, config.backoffMax);
        Assert.assertEquals("/some/path.yaml", config.configFile);
        Assert.assertNull(config.host);
        Assert.assertEquals(new InetSocketAddress(IFC, 222), config.socket);
    }
    @Test(expected = IllegalArgumentException.class)
    public void testIncompleteConfig1() {
        JavaAgentMultiPort.parseConfig("portEnd=333,timeout=500,backoffMin=2000,backoffMax=4000,configFile=/some/path.yaml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompleteConfig2() {
        JavaAgentMultiPort.parseConfig("portStart=222,timeout=500,backoffMin=2000,backoffMax=4000,configFile=/some/path.yaml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompelteConfig3() {
        JavaAgentMultiPort.parseConfig("portStart=222,portEnd=333,timeout=500,backoffMin=2000,backoffMax=4000");
    }

    @Test
    public void testSingleServerStart() throws IOException, InterruptedException {
        JavaAgentMultiPort.startMultipleServers(new JavaAgentMultiPort.Config(IFC, 2000, 2000,500,2000,4000, "some_file.yaml", JavaAgentMultiPort.createInetSocket(IFC, 2000)), RESTARTS, minBackoff, maxBackoff);

        Socket socket = new Socket();
        final InetAddress inetAddress = InetAddress.getByName(IFC);
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 2000);
        socket.connect(inetSocketAddress, 500);
        Assert.assertTrue(socket.isConnected());
        socket.close();
    }

    /**
     * Tests port rolling
     * It has to be able to open 5 server with on ports between 2000 and 2004.
     * <p>
     * Calling JavaAgentMultiPort.startServer() instead of JavaAgentMultiPort.premain(), because it is not able to register multiple
     * server from one JVM. Which is not a issue for us, because we won't be doing that. The problem we are solving
     * with this is socket binding.
     *
     * @throws IOException
     */
    @Test
    public void testServerStarts() throws IOException, InterruptedException {
        JavaAgentMultiPort.startMultipleServers(new JavaAgentMultiPort.Config(IFC, 3000, 3004, 500, 2000, 4000, "some_file.yaml", JavaAgentMultiPort.createInetSocket(IFC, 3000)), RESTARTS, minBackoff, maxBackoff);
        JavaAgentMultiPort.startMultipleServers(new JavaAgentMultiPort.Config(IFC, 3000, 3004, 500, 2000, 4000, "some_file.yaml", JavaAgentMultiPort.createInetSocket(IFC, 3000)), RESTARTS, minBackoff, maxBackoff);
        JavaAgentMultiPort.startMultipleServers(new JavaAgentMultiPort.Config(IFC, 3000, 3004, 500, 2000, 4000, "some_file.yaml", JavaAgentMultiPort.createInetSocket(IFC, 3000)), RESTARTS, minBackoff, maxBackoff);
        JavaAgentMultiPort.startMultipleServers(new JavaAgentMultiPort.Config(IFC, 3000, 3004, 500, 2000, 4000, "some_file.yaml", JavaAgentMultiPort.createInetSocket(IFC, 3000)), RESTARTS, minBackoff, maxBackoff);
        JavaAgentMultiPort.startMultipleServers(new JavaAgentMultiPort.Config(IFC, 3000, 3004, 500, 2000, 4000, "some_file.yaml", JavaAgentMultiPort.createInetSocket(IFC, 3000)), RESTARTS, minBackoff, maxBackoff);

        for (int i = 3000; i <= 3004; i++) {
            Socket socket = new Socket();
            final InetAddress inetAddress = InetAddress.getByName(IFC);
            final InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, i);
            socket.connect(inetSocketAddress, 500);
            Assert.assertTrue(socket.isConnected());
            socket.close();
        }
    }

    @Test(expected = IOException.class)
    public void testOverbookedServerStart() throws IOException, InterruptedException {
        JavaAgentMultiPort.startMultipleServers(new JavaAgentMultiPort.Config(IFC, 4000, 4001,500, 2000,4000, "some_file.yaml", JavaAgentMultiPort.createInetSocket(IFC, 4000)), RESTARTS, minBackoff, maxBackoff);
        JavaAgentMultiPort.startMultipleServers(new JavaAgentMultiPort.Config(IFC, 4000, 4001,500, 2000,4000, "some_file.yaml", JavaAgentMultiPort.createInetSocket(IFC, 4000)), RESTARTS, minBackoff, maxBackoff);

        for (int i = 4000; i <= 4001; i++) {
            Socket socket = new Socket();
            final InetAddress inetAddress = InetAddress.getByName(IFC);
            final InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, i);
            socket.connect(inetSocketAddress, 500);
            Assert.assertTrue(socket.isConnected());
            socket.close();
        }
        JavaAgentMultiPort.startMultipleServers(new JavaAgentMultiPort.Config(IFC, 4000, 4001,500,2000,4000, "some_file.yaml", JavaAgentMultiPort.createInetSocket(IFC, 4000)), RESTARTS, minBackoff, maxBackoff);
    }
}
