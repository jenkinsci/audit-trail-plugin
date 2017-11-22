package hudson.plugins.audit_trail;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.integration.jul.util.LevelHelper;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default values are set in
 * <code>../../resources/hudson.plugins.audit_trail.SslTlsSyslogAuditLogger/config.jelly</code>
 */
public class SyslogAuditLogger extends AuditLogger {

    public static final int DEFAULT_SYSLOG_SERVER_PORT = 514;
    public static final String DEFAULT_APP_NAME = "jenkins";
    public static final Facility DEFAULT_FACILITY = Facility.USER;
    public static final MessageFormat DEFAULT_MESSAGE_FORMAT = MessageFormat.RFC_3164;
    protected static final Logger LOGGER = Logger.getLogger(SyslogAuditLogger.class.getName());
    private transient CdpLogger cdpLogger;

    private String syslogServerHostname;
    private int syslogServerPort;
    private String appName;
    private String messageHostname;
    private Facility facility;
    private MessageFormat messageFormat;
    private String networkProtocol;

    // tls-ssl
    private String keyStore;
    private String keyStorePasswd;
    private String trustStore;
    private String trustStorePasswd;

    @DataBoundConstructor
    public SyslogAuditLogger(String syslogServerHostname,
                             int syslogServerPort,
                             String appName,
                             String messageHostname,
                             String facility,
                             String messageFormat,
                             String networkProtocol,
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
        this.networkProtocol = defaultValue(networkProtocol, "UDP");

        this.keyStore = Objects.requireNonNull(keyStore, () -> null);
        this.keyStorePasswd = Objects.requireNonNull(keyStorePasswd, () -> null);
        this.trustStore = Objects.requireNonNull(trustStore, () -> null);
        this.trustStorePasswd = Objects.requireNonNull(trustStorePasswd, () -> null);

        try {
            if (null != networkProtocol && "TLS".equalsIgnoreCase(networkProtocol)) {
                this.cdpLogger = new TlsLogger(this.syslogServerHostname, this.appName, this.syslogServerPort, this.keyStore, this.trustStore, this.keyStorePasswd, this.trustStorePasswd);
            }
            if (null != networkProtocol && "UDP".equalsIgnoreCase(networkProtocol)) {
                this.cdpLogger = new UdpLogger(this.syslogServerHostname, this.appName, this.syslogServerPort);
            } else {
                LOGGER.log(Level.WARNING, "Invalid networkProtocol defined. expected only TLS or UDP ");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    @Override
    public void log(String event) {

        try {
            LOGGER.log(Level.FINER, "Send audit message \"{0}\" to syslog server {1}", new Object[]{event, this.cdpLogger});
            this.cdpLogger.handle(event);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception sending audit message to syslog server " + this.cdpLogger, e);
            LOGGER.warning(event);
        }
    }

    @Override
    public void configure() {
    }

    public String getDisplayName() {
        return "Syslog TLS-UDP Logger";
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
        return "TLS-UDP";
    }

    @Override
    public boolean equals(Object thisObject) {

        if (this == thisObject)
            return true;
        if (thisObject == null || getClass() != thisObject.getClass())
            return false;

        SyslogAuditLogger that = (SyslogAuditLogger) thisObject;

        return Objects.equals(syslogServerPort, that.syslogServerPort) &&
               Objects.equals(appName, that.appName) &&
               Objects.equals(facility, that.facility) &&
               Objects.equals(messageFormat, that.messageFormat) &&
               Objects.equals(messageHostname, that.messageHostname) &&
               Objects.equals(syslogServerHostname, that.syslogServerHostname) &&
               Objects.equals(keyStore, that.keyStore) &&
               Objects.equals(keyStorePasswd, that.keyStorePasswd) &&
               Objects.equals(trustStore, that.trustStore) &&
               Objects.equals(cdpLogger, that.cdpLogger) &&
               Objects.equals(trustStorePasswd, that.trustStorePasswd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(syslogServerHostname, syslogServerPort, appName, messageHostname, facility, messageFormat, keyStore, keyStorePasswd,
                trustStore, trustStorePasswd, cdpLogger);
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

    @Extension
    public static class DescriptorImpl extends Descriptor<AuditLogger> {

        @Override
        public String getDisplayName() {
            return "Syslog (TLS & UDP) server";
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
            items.add("UDP");
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
