package io.prometheus.jmx.test.common;

/** Class to implement AuthenticationTestCredentials */
public class AuthenticationCredentials {

    private String username;
    private String password;
    private boolean isValid;

    /**
     * Constructor
     *
     * @param username
     * @param password
     * @param isValid
     */
    private AuthenticationCredentials(String username, String password, boolean isValid) {
        this.username = username;
        this.password = password;
        this.isValid = isValid;
    }

    /**
     * Method to get the username
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Method to get the password
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Method to return if the credentials are valid
     *
     * @return true if the credentials are valid, else false
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Method to create an AuthenticationTestCredentials
     *
     * @param username username
     * @param password password
     * @param isValid isValid
     * @return an AuthenticationTestCredentials
     */
    public static AuthenticationCredentials of(String username, String password, boolean isValid) {
        return new AuthenticationCredentials(username, password, isValid);
    }
}
