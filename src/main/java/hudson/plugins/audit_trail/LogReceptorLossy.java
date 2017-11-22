package hudson.plugins.audit_trail;

import org.graylog2.syslog4j.impl.net.udp.UDPNetSyslogConfig;

public class LogReceptorLossy extends LogReceptor {
    UDPNetSyslogConfig conf = new UDPNetSyslogConfig();

    public LogReceptorLossy(int writeRetries, String syslogInstanceName, String procId, String defaultMsgHostName,
                            String defaultAppName, int defaultFacility, String syslogServerHostName, int port) {
        conf.setWriteRetries(writeRetries);
        super.init(conf, procId, syslogInstanceName, defaultMsgHostName, defaultAppName, defaultFacility,
                syslogServerHostName, port);
    }
}
