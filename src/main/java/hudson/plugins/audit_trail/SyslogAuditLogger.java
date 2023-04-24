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
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default values are set in <code>/src/main/resources/hudson/plugins/audit_trail/SyslogAuditLogger/config.jelly</code>
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SyslogAuditLogger extends AuditLogger {

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


    @DataBoundConstructor
    public SyslogAuditLogger(String syslogServerHostname, int syslogServerPort,
                             String appName, String messageHostname,
                             String facility, String messageFormat) {
        this.syslogServerHostname = trimToNull(syslogServerHostname);
        this.syslogServerPort = defaultValue(syslogServerPort, DEFAULT_SYSLOG_SERVER_PORT);
        this.appName = defaultValue(trimToNull(appName), DEFAULT_APP_NAME);
        this.messageHostname = trimToNull(messageHostname);
        this.facility = defaultValue(Facility.fromLabel(trimToNull(facility)), DEFAULT_FACILITY);
        this.messageFormat = MessageFormat.valueOf(defaultValue(trimToNull(messageFormat), DEFAULT_MESSAGE_FORMAT.toString()));
        configure();
    }

    private Object readResolve() {
        configure();
        return this;
    }

    @Override
    public void log(String event) {

        if (syslogMessageSender == null) {
            LOGGER.log(Level.FINER, "skip log {0}, syslogMessageSender not configured", event);
            return;
        }
        LOGGER.log(Level.FINER, "Send audit message \"{0}\" to syslog server {1}", new Object[]{event, syslogMessageSender});

        try {
            syslogMessageSender.sendMessage(event);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Exception sending audit message to syslog server " + syslogMessageSender.toString(), e);
            LOGGER.warning(event);
        }
    }

    private void configure() {
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
        return "Syslog Logger";
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
        return "UDP";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SyslogAuditLogger)) return false;

        SyslogAuditLogger that = (SyslogAuditLogger) o;

        if (syslogServerPort != that.syslogServerPort) return false;
        if (appName != null ? !appName.equals(that.appName) : that.appName != null) return false;
        if (facility != that.facility) return false;
        if (messageFormat != that.messageFormat) return false;
        if (messageHostname != null ? !messageHostname.equals(that.messageHostname) : that.messageHostname != null)
            return false;
        if (syslogServerHostname != null ? !syslogServerHostname.equals(that.syslogServerHostname) : that.syslogServerHostname != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = syslogServerHostname != null ? syslogServerHostname.hashCode() : 0;
        result = 31 * result + syslogServerPort;
        result = 31 * result + (appName != null ? appName.hashCode() : 0);
        result = 31 * result + (messageHostname != null ? messageHostname.hashCode() : 0);
        result = 31 * result + (facility != null ? facility.hashCode() : 0);
        result = 31 * result + (messageFormat != null ? messageFormat.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SyslogAuditLogger{" +
                "syslogServerHostname='" + syslogServerHostname + '\'' +
                ", syslogServerPort=" + syslogServerPort +
                ", appName='" + appName + '\'' +
                ", messageHostname='" + messageHostname + '\'' +
                ", facility=" + facility +
                '}';
    }

    protected static final Logger LOGGER = Logger.getLogger(SyslogAuditLogger.class.getName());

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
            return "Syslog server";
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
