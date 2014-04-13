package hudson.plugins.audit_trail;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.integration.jul.SyslogHandler;
import com.cloudbees.syslog.integration.jul.util.LevelHelper;
import com.cloudbees.syslog.sender.SyslogMessageSender;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.*;

import static java.util.logging.Level.CONFIG;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SyslogAuditLogger extends AuditLogger {

    public static final int DEFAULT_SYSLOG_SERVER_PORT = 514;
    public static final String DEFAULT_APP_NAME = "jenkins";
    public static final Facility DEFAULT_FACILITY = Facility.USER;
    public static final MessageFormat DEFAULT_MESSAGE_FORMAT = MessageFormat.RFC_3164;

    private transient SyslogMessageSender syslogMessageSender;
    private String syslogServerHostname;
    private int syslogServerPort = DEFAULT_SYSLOG_SERVER_PORT;
    private String appName = DEFAULT_APP_NAME;
    private String messageHostname;
    private Facility facility = DEFAULT_FACILITY;
    private MessageFormat messageFormat = DEFAULT_MESSAGE_FORMAT;


    @DataBoundConstructor
    public SyslogAuditLogger(String syslogServerHostname, int syslogServerPort,
                             String appName, String messageHostname,
                             String facility, String messageFormat) {
        this.syslogServerHostname = trimToNull(syslogServerHostname);
        this.syslogServerPort = defaultValue(syslogServerPort, DEFAULT_SYSLOG_SERVER_PORT);
        this.appName = trimToNull(appName);
        this.messageHostname = trimToNull(messageHostname);
        this.facility = defaultValue(Facility.fromLabel(trimToNull(facility)), DEFAULT_FACILITY);
        this.messageFormat = MessageFormat.valueOf(defaultValue(trimToNull(messageFormat), DEFAULT_MESSAGE_FORMAT.toString()));
    }

    @Override
    public void log(String event) {

        if (syslogMessageSender == null) {
            LOGGER.log(Level.FINER, "skip log %s, syslogMessageSender not configured", event);
            return;
        }
        try {
            syslogMessageSender.sendMessage(event);
        } catch (IOException e) {
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

        LOGGER.log(Level.FINE, "SyslogAuditLogger: %s", this);
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
        new Exception("getFacility():" + facility).printStackTrace();
        return facility == null ? null : facility.label();
    }

    public String getMessageFormat() {
        return messageFormat == null ? null : messageFormat.name();
    }

    public String getNetworkProtocol() {
        return "UDP";
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
