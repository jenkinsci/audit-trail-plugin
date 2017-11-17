package hudson.plugins.audit_trail;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.integration.jul.util.LevelHelper;
import com.cloudbees.syslog.sender.SyslogMessageSender;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Default values are set in
 * <code>../../resources/hudson.plugins.audit_trail.SslTlsSyslogAuditLogger/config.jelly</code>
 */
public class SslTlsSyslogAuditLogger extends AuditLogger {

    protected static final Logger LOGGER = Logger.getLogger(SslTlsSyslogAuditLogger.class.getName());

    public static final int DEFAULT_SYSLOG_SERVER_PORT = 514;
    public static final String DEFAULT_APP_NAME = "jenkins";
    public static final Facility DEFAULT_FACILITY = Facility.USER;
    public static final MessageFormat DEFAULT_MESSAGE_FORMAT = MessageFormat.RFC_3164;

    private transient SyslogMessageSender syslogMessageSender;
    private String syslogServerHostname;
    private int syslogServerPort;
    private String appName;
    private String messageHostname;
    private Facility facility;
    private MessageFormat messageFormat;

    // tls-ssl
    private String keyStore;
    private String keyStorePasswd;
    private String trustStore;
    private String trustStorePasswd;

    @DataBoundConstructor
    public SslTlsSyslogAuditLogger(String syslogServerHostname,
                    int syslogServerPort,
                    String appName,
                    String messageHostname,
                    String facility,
                    String messageFormat,
                    String keyStore,
                    String keyStorePasswd,
                    String trustStore,
                    String trustStorePasswd) {

        this.syslogServerHostname = trimToNull(syslogServerHostname);
        this.syslogServerPort = defaultValue(syslogServerPort, DEFAULT_SYSLOG_SERVER_PORT);
        this.appName = defaultValue(trimToNull(appName), DEFAULT_APP_NAME);
        this.messageHostname = trimToNull(messageHostname);
        this.facility = defaultValue(Facility.fromLabel(trimToNull(facility)), DEFAULT_FACILITY);
        this.messageFormat = MessageFormat.valueOf(defaultValue(trimToNull(messageFormat), DEFAULT_MESSAGE_FORMAT.toString()));

        this.keyStore = Objects.requireNonNull(keyStore, () -> null);
        this.keyStorePasswd = Objects.requireNonNull(keyStorePasswd, () -> null);
        this.trustStore = Objects.requireNonNull(trustStore, () -> null);
        this.trustStorePasswd = Objects.requireNonNull(trustStorePasswd, () -> null);
    }

    @Override
    public void log(String event) {

        LOGGER.log(Level.FINER, "Send audit message \"{0}\" to syslog server {1}", new Object[] { event, syslogMessageSender });

        try {
            // syslogMessageSender.sendMessage(event);

            String keyStore = "";
            String trustStore = "";
            String keystorePass = "";
            String trustStorePass = "";
            String syslogInstanceName = "";
            String procId = "";
            String defaultMsgHostName = "";
            String defaultAppName = "";
            int defaultFacility = 1;
            String syslogServerHostName = "";
            int port = 415;
            boolean threaded = true;

            LogReceptor logger = new LogReceptorEncrypted(keyStore,
                            trustStore,
                            keystorePass,
                            trustStorePass,
                            syslogInstanceName,
                            procId,
                            defaultMsgHostName,
                            defaultAppName,
                            defaultFacility,
                            syslogServerHostName,
                            port,
                            threaded);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception sending audit message to syslog server " + syslogMessageSender.toString(), e);
            LOGGER.warning(event);
        }
    }

    @Override
    public void configure() {
        if (syslogServerHostname == null || syslogServerHostname.isEmpty()) {
            LOGGER.fine("SyslogLogger not configured");
            return;
        }

        syslogMessageSender = new UdpSyslogMessageSender();
        ((UdpSyslogMessageSender) syslogMessageSender).setSyslogServerHostname(syslogServerHostname);
        ((UdpSyslogMessageSender) syslogMessageSender).setSyslogServerPort(syslogServerPort);
        ((UdpSyslogMessageSender) syslogMessageSender).setMessageFormat(messageFormat);
        ((UdpSyslogMessageSender) syslogMessageSender).setDefaultAppName(appName);
        ((UdpSyslogMessageSender) syslogMessageSender).setDefaultMessageHostname(messageHostname);
        ((UdpSyslogMessageSender) syslogMessageSender).setDefaultFacility(facility);

        LOGGER.log(Level.FINE, "SyslogAuditLogger: {0}", this);
    }

    public String getDisplayName() {
        return "Syslog TLS/SSL Logger";
    }

    public String getSyslogServerHostname() {
        return syslogServerHostname;
    }

    public int getSyslogServerPort() {
        return syslogServerPort;
    }

    public String getAppName() {
        return appName;
    }

    public String getMessageHostname() {
        return messageHostname;
    }

    public String getFacility() {
        return facility == null ? null : facility.label();
    }

    public String getMessageFormat() {
        return messageFormat == null ? null : messageFormat.name();
    }

    public String getNetworkProtocol() {
        return "TLS/SSL";
    }

    @Override
    public boolean equals(Object thisObject) {

        if (this == thisObject)
            return true;
        if (thisObject == null || getClass() != thisObject.getClass())
            return false;

        SslTlsSyslogAuditLogger that = (SslTlsSyslogAuditLogger) thisObject;

        return Objects.equals(syslogServerPort, that.syslogServerPort) &&
               Objects.equals(appName, that.appName) &&
               Objects.equals(facility, that.facility) &&
               Objects.equals(messageFormat, that.messageFormat) &&
               Objects.equals(messageHostname, that.messageHostname) &&
               Objects.equals(syslogServerHostname, that.syslogServerHostname) &&
               Objects.equals(keyStore, that.keyStore) &&
               Objects.equals(keyStorePasswd, that.keyStorePasswd) &&
               Objects.equals(trustStore, that.trustStore) &&
               Objects.equals(trustStorePasswd, that.trustStorePasswd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(syslogServerHostname, syslogServerPort, appName, messageHostname, facility, messageFormat, keyStore, keyStorePasswd,
                        trustStore, trustStorePasswd);
    }

    @Override
    public String toString() {
        return "SyslogTlsAuditLogger{" +
               "syslogServerHostname='" + syslogServerHostname + '\'' +
               ", syslogServerPort=" + syslogServerPort +
               ", appName='" + appName + '\'' +
               ", messageHostname='" + messageHostname + '\'' +
               ", facility=" + facility +
               '}';
    }

    @Nullable
    public static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        } else {
            return value;
        }
    }

    @Nullable
    public static <T> T defaultValue(T value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }

    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AuditLogger> {

        @Override
        public String getDisplayName() {
            return "Syslog TLS/SSL server";
        }

        public ListBoxModel doFillLevelFilterItems() {
            ListBoxModel items = new ListBoxModel();
            Level[] levels = LevelHelper.levels.toArray(new Level[0]);
            Arrays.sort(levels, LevelHelper.comparator());
            for (Level level : levels) {
                items.add(level.getName());
            }
            return items;
        }

        public ListBoxModel doFillNetworkProtocolItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("TLS");
            return items;
        }

        public ListBoxModel doFillMessageFormatItems() {
            ListBoxModel items = new ListBoxModel();
            for (MessageFormat messageFormat : MessageFormat.values()) {
                items.add(messageFormat.name());
            }
            return items;
        }

        public ListBoxModel doFillSeverityItems() {
            ListBoxModel items = new ListBoxModel();
            Severity[] severities = Severity.values();
            Arrays.sort(severities, Severity.comparator());
            for (Severity severity : severities) {
                items.add(severity.label());
            }
            return items;
        }

        public ListBoxModel doFillFacilityItems() {
            ListBoxModel items = new ListBoxModel();
            Facility[] facilities = Facility.values();
            Arrays.sort(facilities, Facility.comparator());
            for (Facility facility : facilities) {
                items.add(facility.label());
            }
            return items;
        }
    }

}
