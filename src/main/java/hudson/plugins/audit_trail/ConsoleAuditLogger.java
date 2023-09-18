package hudson.plugins.audit_trail;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class ConsoleAuditLogger extends AuditLogger {
    public enum Output {
        STD_OUT,
        STD_ERR
    }

    private final Output output;
    private final String dateFormat;
    private final String logPrefix;
    private transient PrintStream out;
    private transient SimpleDateFormat sdf;
    private transient String logPrefixPadded;

    @DataBoundConstructor
    public ConsoleAuditLogger(Output output, String dateFormat, String logPrefix) {
        if (output == null) {
            throw new NullPointerException("output can not be null");
        }
        if (dateFormat == null) {
            throw new NullPointerException("dateFormat can not be null");
        }

        this.logPrefix = logPrefix;
        this.output = output;
        if (output != Output.STD_ERR && output != Output.STD_OUT) {
            throw new IllegalArgumentException("Unsupported output " + output);
        }

        this.dateFormat = dateFormat;

        // validate the dataFormat
        new SimpleDateFormat(dateFormat);
        configure();
    }

    private Object readResolve() {
        configure();
        return this;
    }

    @Override
    public void log(String event) {
        synchronized (output) {
            this.out.println(sdf.format(new Date()) + this.logPrefixPadded + event);
        }
    }

    private void configure() {
        synchronized (output) {
            switch (output) {
                case STD_ERR:
                    out = System.err;
                    break;
                case STD_OUT:
                    out = System.out;
                    break;
            }
            sdf = new SimpleDateFormat(dateFormat);
            this.logPrefixPadded = getLogPrefixPadded();
        }
    }

    public Output getOutput() {
        return output;
    }

    public String getDateFormat() {
        return this.dateFormat;
    }

    public String getLogPrefix() {
        return this.logPrefix;
    }

    private Boolean hasLogPrefix() {
        return this.logPrefix != null && !this.logPrefix.equals("");
    }

    private String getLogPrefixPadded() {
        if (hasLogPrefix()) {
            if (logPrefixPadded == null) {
                logPrefixPadded = String.format(" - %s - ", getLogPrefix());
            }
            return logPrefixPadded;
        }

        return " - ";
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AuditLogger> {

        @Override
        public String getDisplayName() {
            return "Console";
        }

        public ListBoxModel doFillOutputItems() {
            ListBoxModel items = new ListBoxModel();
            Output[] outputs = Output.values();
            for (Output output : outputs) {
                items.add(output.name());
            }
            return items;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsoleAuditLogger)) return false;

        ConsoleAuditLogger that = (ConsoleAuditLogger) o;

        if (!dateFormat.equals(that.dateFormat)) return false;
        if (output != that.output) return false;
        if (!logPrefix.equals(that.logPrefix)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = output.hashCode();
        result = 31 * result + dateFormat.hashCode();
        result = 31 * result + logPrefix.hashCode();
        return result;
    }
}
