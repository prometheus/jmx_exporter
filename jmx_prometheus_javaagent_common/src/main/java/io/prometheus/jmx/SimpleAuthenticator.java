package io.prometheus.jmx;

import com.sun.net.httpserver.BasicAuthenticator;

public class SimpleAuthenticator extends BasicAuthenticator {
    private String username;
    private String password;

    public SimpleAuthenticator(String realm, String username, String password) {
        super(realm);
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        return username != null && password != null
                && this.username != null && this.password != null
                && username.equals(this.username) && password.equals(this.password);
    }
}
