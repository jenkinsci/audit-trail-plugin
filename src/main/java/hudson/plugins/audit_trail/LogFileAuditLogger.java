package hudson.plugins.audit_trail;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Descriptor;
import java.io.IOException;
import java.util.logging.FileHandler;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @author Pierre Beitz
 */
public class LogFileAuditLogger extends AbstractLogFileAuditLogger {

    private int limit = 1;

    @DataBoundConstructor
    public LogFileAuditLogger(String log, int limit, int count, String logSeparator) {
        super(log, count, logSeparator);
        this.limit = limit;
        configure();
    }

    @SuppressFBWarnings(
            value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            justification = "value can be null if no config file exists")
    Object readResolve() {
        super.readResolve();
        configure();
        return this;
    }

    @Override
    FileHandler getLogFileHandler() throws IOException {
        return new FileHandler(getLog(), getLimit() * 1024 * 1024, getCount(), true);
    }

    public int getLimit() {
        return limit;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AuditLogger> {

        @Override
        public String getDisplayName() {
            return "Log file";
        }
    }
}
