package hudson.plugins.audit_trail;

import org.graylog2.syslog4j.util.SyslogUtility;

public class LogReceptorInfo {
    
    private final String regex;
    private final String type;
    private final String syslogHostName;
    private int facility = SyslogUtility.FACILITY_USER;
    private final int syslogPort;
    private final String keyStore;
    private final String keyStorePasswd;
    private final String trustStore;
    private final String trustStorePasswd;
    private boolean threaded;

    public LogReceptorInfo(String regex,
                    String type,
                    String syslogHostName,
                    int facility,
                    int syslogPort,
                    String keyStore,
                    String keyStorePasswd,
                    String trustStore,
                    String trustStorePasswd,
                    boolean threaded) {
        this.regex = regex;
        this.type = type;
        this.syslogHostName = syslogHostName;
        this.facility = facility;
        this.syslogPort = syslogPort;
        this.keyStore = keyStore;
        this.keyStorePasswd = keyStorePasswd;
        this.trustStore = trustStore;
        this.trustStorePasswd = trustStorePasswd;
        this.threaded = threaded;
    }

    /**
     * @return the regex
     */
    public String getRegex() {
        return this.regex;
    }

    /**
     * @return the type
     */
    public String getType() {
        return this.type;
    }

    /**
     * @return the syslogHostName
     */
    public String getSyslogHostName() {
        return this.syslogHostName;
    }

    /**
     * @return the facility
     */
    public int getFacility() {
        return this.facility;
    }

    /**
     * @return the syslogPort
     */
    public int getSyslogPort() {
        return this.syslogPort;
    }

    /**
     * @return the keyStore
     */
    public String getKeyStore() {
        return this.keyStore;
    }

    /**
     * @return the keyStorePasswd
     */
    public String getKeyStorePasswd() {
        return this.keyStorePasswd;
    }

    /**
     * @return the trustStore
     */
    public String getTrustStore() {
        return this.trustStore;
    }

    /**
     * @return the trustStorePasswd
     */
    public String getTrustStorePasswd() {
        return this.trustStorePasswd;
    }

    /**
     * @return the threaded
     */
    public boolean isThreaded() {
        return this.threaded;
    }

}
