package hudson.plugins.audit_trail;

import org.graylog2.syslog4j.util.SyslogUtility;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.net.InetAddress.getLocalHost;

final class TlsLogger implements CdpLogger {

    private final String syslogServerHostname;
    private final String appName;
    private final int syslogServerPort;
    private final String keyStore;
    private final String trustStore;
    private final String keyStorePasswd;
    private final String trustStorePasswd;
    private final String messageId;

    LogReceptor logReceptor = null;

    public TlsLogger(String syslogServerHostname,
                     String appName,
                     int syslogServerPort,
                     String keyStore,
                     String trustStore,
                     String keyStorePasswd,
                     String trustStorePasswd) throws UnknownHostException {
        this.syslogServerHostname = syslogServerHostname;
        this.appName = appName;
        this.syslogServerPort = syslogServerPort;
        this.keyStore = keyStore;
        this.trustStore = trustStore;
        this.keyStorePasswd = keyStorePasswd;
        this.trustStorePasswd = trustStorePasswd;
        messageId = UUID.randomUUID().toString().concat("_").concat(this.appName);

        this.logReceptor = new LogReceptorEncrypted(this.syslogServerHostname,
                messageId,
                getLocalHost().getHostName(),
                this.appName,
                SyslogUtility.FACILITY_USER,
                this.syslogServerHostname,
                this.syslogServerPort,
                Boolean.TRUE,
                this.keyStore,
                this.trustStore,
                this.keyStorePasswd,
                this.trustStorePasswd);
    }

    public void handle(String event) throws Exception {
        Map<String, Map<String, String>> props = new HashMap<>();

        logReceptor.process(messageId, LogReceptor.Severity.CRITICAL, null, event);
        logReceptor.flush();
    }
}