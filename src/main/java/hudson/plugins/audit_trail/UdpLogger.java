package hudson.plugins.audit_trail;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.UUID;

final class UdpLogger implements CdpLogger {

    private final String syslogServerHostname;
    private final String appName;
    private final int syslogServerPort;
    private final String messageId;

    private final LogReceptorLossy logReceptorLossy;
    private final int writeRetries = 5; //default

    public UdpLogger(String syslogServerHostname,
                     String appName,
                     int syslogServerPort) throws UnknownHostException {
        this.syslogServerHostname = syslogServerHostname;
        this.appName = appName;
        this.syslogServerPort = syslogServerPort;
        messageId = UUID.randomUUID().toString().concat("_").concat(this.appName);

//        int writeRetries, String syslogInstanceName, String procId, String defaultMsgHostName,
//                String defaultAppName, int defaultFacility, String syslogServerHostName, int port

        logReceptorLossy = new LogReceptorLossy(this.writeRetries, syslogServerHostname, messageId,
                syslogServerHostname,
                appName, 1, this.syslogServerHostname, this.syslogServerPort);
    }

    public void handle(String event) throws Exception {
        logReceptorLossy.process(messageId, LogReceptor.Severity.CRITICAL, new HashMap<>(), event);
        logReceptorLossy.flush();
    }

}
