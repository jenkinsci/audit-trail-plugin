package hudson.plugins.audit_trail;

import org.graylog2.syslog4j.impl.net.tcp.ssl.SSLTCPNetSyslogConfig;

public class LogReceptorEncrypted extends LogReceptor {

    public LogReceptorEncrypted(String syslogInstanceName,
                    String procId,
                    String defaultMsgHostName,
                    String defaultAppName,
                    int defaultFacility,
                    String syslogServerHostName,
                    int port,
                    boolean threaded, 
                    String keyStore,
                    String trustStore,
                    String keystorePass,
                    String trustStorePass) {

        SSLTCPNetSyslogConfig conf = new SSLTCPNetSyslogConfig();

        conf.setKeyStore(keyStore);
        conf.setKeyStorePassword(keystorePass);
        conf.setTrustStore(trustStore);
        conf.setTrustStorePassword(trustStorePass);
        conf.setThreaded(threaded);

        super.init(conf, procId, syslogInstanceName, defaultMsgHostName, defaultAppName, defaultFacility,
                        syslogServerHostName, port);
    }
}
