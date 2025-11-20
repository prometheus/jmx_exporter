package io.prometheus.jmx.ssl;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import nl.altindag.ssl.SSLFactory;

public final class EnhanceableSslRMIClientSocketFactory
        implements RMIClientSocketFactory, Serializable {

    private static final long serialVersionUID = 7523870139487859635L;

    private final SSLFactory sslFactory;

    public EnhanceableSslRMIClientSocketFactory(SSLFactory sslFactory) {
        this.sslFactory = sslFactory;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return sslFactory.getSslSocketFactory().createSocket(host, port);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        return this.getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }
}
