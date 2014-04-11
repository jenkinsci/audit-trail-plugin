package hudson.plugins.audit_trail;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.logging.LogRecorder;
import hudson.logging.LogRecorderManager;
import hudson.logging.WeakLogHandler;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.security.ACL;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
    public void log(Category category, String event) {
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
    public void configure() {
        try {
            FileHandler h = new FileHandler(log, limit * 1024 * 1024, count, true);
            h.setFormatter(new Formatter() {
                SimpleDateFormat dateformat = new SimpleDateFormat("MMM d, yyyy h:mm:ss aa ");

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
