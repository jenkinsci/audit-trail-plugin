package hudson.plugins.audit_trail;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class ConsoleAuditLogger extends AuditLogger {
    private final Output output;
    private final PrintStream out;
    private final String dateFormat;
    private final SimpleDateFormat sdf;
    @DataBoundConstructor
    public ConsoleAuditLogger(Output output, String dateFormat) {
        if (output == null)
            throw new NullPointerException("output can not be null");
        if (dateFormat == null)
            throw new NullPointerException("dateFormat can not be null");

        this.output = output;
        switch (output) {
            case STD_ERR:
                out = System.err;
                break;
            case STD_OUT:
                out = System.out;
                break;
            default:
                throw new IllegalArgumentException("Unsupported output " + output);
        }
        this.dateFormat = dateFormat;
        sdf = new SimpleDateFormat(dateFormat);
    }

    @Override
    public void log(String event) {
        synchronized (sdf) {
            this.out.println(sdf.format(new Date()) + " - " + event);
        }
    }

    @Override
    public void configure() {

    }

    public Output getOutput() {
        return output;
    }

    public String getDateFormat() {
        return this.dateFormat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsoleAuditLogger)) return false;

        ConsoleAuditLogger that = (ConsoleAuditLogger) o;

        if (!dateFormat.equals(that.dateFormat)) return false;
        if (output != that.output) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = output.hashCode();
        result = 31 * result + dateFormat.hashCode();
        return result;
    }

    public enum Output {STD_OUT, STD_ERR}

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
}
