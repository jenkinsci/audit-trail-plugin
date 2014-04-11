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

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class LogFileAuditLogger extends AuditLogger {

    private static Logger WEB_LOGGER = Logger.getLogger(AuditTrailFilter.class.getName());

    private static Logger RUN_LOGGER = Logger.getLogger(AuditTrailRunListener.class.getName());

    @DataBoundConstructor
    public LogFileAuditLogger(String log, int limit, int count) {
        this.log = log;
        this.limit = limit;
        this.count = count;
    }

    @Override
    public void log(Category category, String event) {
        switch (category) {
            case WEB:
                WEB_LOGGER.config(event);
                break;
            case RUN:
                RUN_LOGGER.config(event);
                break;
        }
    }

    private String log;
    private int limit = 1;
    private int count = 1;

    public String getLog() { return log; }

    public int getLimit() { return limit; }

    public int getCount() { return count; }



    @Override
    public void configure() {
        Logger logger = Logger.getLogger(AuditTrailFilter.class.getPackage().getName());
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
            handler.close();
        }
        if (log != null && log.length() > 0) try {
            FileHandler handler = new FileHandler(log, limit * 1024 * 1024, count, true);
            handler.setLevel(Level.CONFIG);
            handler.setFormatter(new Formatter() {
                SimpleDateFormat dateformat = new SimpleDateFormat("MMM d, yyyy h:mm:ss aa ");
                public synchronized String format(LogRecord record) {
                    return dateformat.format(new Date(record.getMillis()))
                            + record.getMessage() + '\n';
                }
            });
            logger.setLevel(Level.CONFIG);
            logger.addHandler(handler);
            // Workaround for SJSWS logging.. no need for audit trail to appear in logs/errors
            // since we have our own log file, BUT this handler ignores its level setting and
            // logs anything it receives.  So don't use parent handlers..
            logger.setUseParentHandlers(false);
            // ..but Jenkins' LogRecorders run via a handler on the root logger so we'll
            // route messages directly to that handler..
            logger.addHandler(new RouteToJenkinsHandler());
        }
        catch (IOException ex) { ex.printStackTrace(); }

        // TODO should rely on @Initializer(after = InitMilestone.COMPLETED)
        // Add LogRecorder if not already configured.. but wait for Jenkins to initialize:
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException ex) {}

                SecurityContext old = ACL.impersonate(ACL.SYSTEM);
                try {
                    LogRecorderManager lrm = Hudson.getInstance().getLog();
                    if (!lrm.logRecorders.containsKey("Audit Trail")) {
                        LogRecorder logRecorder = new LogRecorder("Audit Trail");
                        logRecorder.targets.add(
                                new LogRecorder.Target(AuditTrailFilter.class.getPackage().getName(), Level.CONFIG));
                        try {
                            logRecorder.save();
                        } catch (Exception ex) {
                        }
                        lrm.logRecorders.put("Audit Trail", logRecorder);
                    }
                } finally {
                    SecurityContextHolder.setContext(old);
                }
            }
        }.start();
    }

    private static class RouteToJenkinsHandler extends Handler {
        public void publish(LogRecord record) {
            for (Handler handler : Logger.getLogger("").getHandlers()) {
                if (handler instanceof WeakLogHandler) {
                    handler.publish(record);
                }
            }
        }
        public void flush() { }
        public void close() { }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AuditLogger> {

        @Override
        public String getDisplayName() {
            return "Log file";
        }
    }

}
