package hudson.plugins.audit_trail;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static java.util.logging.Level.CONFIG;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @author Pierre Beitz
 */
public class LogFileAuditLogger extends AuditLogger {

    private static final Logger LOGGER = Logger.getLogger(LogFileAuditLogger.class.getName());
    static final String DEFAULT_LOG_SEPARATOR=" ";
    @Nonnull
    private String logSeparator;

    private transient FileHandler handler;

    @DataBoundConstructor
    public LogFileAuditLogger(String log, int limit, int count, String logSeparator) {
        this.log = log;
        this.limit = limit;
        this.count = count;
        this.logSeparator = Optional.ofNullable(logSeparator).orElse(DEFAULT_LOG_SEPARATOR);
        configure();
    }

    private Object readResolve() {
        if(logSeparator == null) {
            logSeparator = DEFAULT_LOG_SEPARATOR;
        }
        configure();
        return this;
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

    @Nonnull
    public String getLogSeparator() {
        return logSeparator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogFileAuditLogger)) return false;

        LogFileAuditLogger that = (LogFileAuditLogger) o;

        if (count != that.count) return false;
        if (limit != that.limit) return false;
        if (!logSeparator.equals(that.logSeparator)) return false;
        if (log != null ? !log.equals(that.log) : that.log != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = log != null ? log.hashCode() : 0;
        result = 31 * result + limit;
        result = 31 * result + count;
        result = 31 * result + logSeparator.hashCode();
        return result;
    }

    private void configure() {
        // looks like https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6244047 is somehow still there
        // there is no way for us to know before hand what path we are looking to create as it would
        // mean having access to FileHandler#generate so either reflexion or catching the exception and retrieving
        // the path. Let's go with number 2.
        try {
            FileHandler h = null;
            try {
                h = new FileHandler(log, limit * 1024 * 1024, count, true);
            } catch (NoSuchFileException ex) {
                LOGGER.info("Couldn't create the file handler lock file, forcing creation of intermediate directories");
                String lockFileName = ex.getFile();
                boolean mkdirs = new File(lockFileName).getParentFile().mkdirs();
                if (mkdirs) {
                    h = new FileHandler(log, limit * 1024 * 1024, count, true);
                }
            }
            if (h != null) {
                h.setFormatter(new Formatter() {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy h:mm:ss,SSS aa");

                    @Override
                    public synchronized String format(LogRecord record) {
                        return dateFormat.format(new Date(record.getMillis())) + getLogSeparator()
                                + record.getMessage() + '\n';
                    }
                });
                h.setLevel(CONFIG);
                handler = h;
            } else {
                LOGGER.severe("Couldn't configure the plugin, as the file handler wasn't successfully created. You should report this issue");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Couldn't configure the plugin, you should report this issue", ex);
        }
    }

    @Override
    public void cleanUp() throws SecurityException {
        if(handler != null) {
            handler.close();
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AuditLogger> {

        @Override
        public String getDisplayName() {
            return "Log file";
        }
    }

}
