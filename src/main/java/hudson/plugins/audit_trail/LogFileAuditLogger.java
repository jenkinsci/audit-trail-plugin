package hudson.plugins.audit_trail;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import static java.util.logging.Level.CONFIG;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class LogFileAuditLogger extends AuditLogger {

    private transient FileHandler handler;

    @DataBoundConstructor
    public LogFileAuditLogger(String log, int limit, int count) {
        this.log = log;
        this.limit = limit;
        this.count = count;
    }

    @Override
    public void log(String event) {
        if (handler == null) return;
        handler.publish(new LogRecord(CONFIG, event));
    }

    private String log;
    private int limit = 1;
    private int count = 1;

    public String getLog() { return log; }

    public int getLimit() { return limit; }

    public int getCount() { return count; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogFileAuditLogger)) return false;

        LogFileAuditLogger that = (LogFileAuditLogger) o;

        if (count != that.count) return false;
        if (limit != that.limit) return false;
        if (log != null ? !log.equals(that.log) : that.log != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = log != null ? log.hashCode() : 0;
        result = 31 * result + limit;
        result = 31 * result + count;
        return result;
    }

    @Override
    public void configure() {
        try {
            FileHandler h = new FileHandler(log, limit * 1024 * 1024, count, true);
            h.setFormatter(new Formatter() {
                SimpleDateFormat dateformat = new SimpleDateFormat("MMM d, yyyy h:mm:ss,SSS aa ");

                public synchronized String format(LogRecord record) {
                    return dateformat.format(new Date(record.getMillis()))
                            + record.getMessage() + '\n';
                }
            });
            h.setLevel(CONFIG);
            handler = h;

        } catch (IOException ex) { ex.printStackTrace(); }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AuditLogger> {

        @Override
        public String getDisplayName() {
            return "Log file";
        }
    }

}
