package hudson.plugins.audit_trail;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Util;

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

public abstract class AbstractLogFileAuditLogger extends AuditLogger {

    private static final Logger LOGGER = Logger.getLogger(AbstractLogFileAuditLogger.class.getName());
    static final String DEFAULT_LOG_SEPARATOR =" ";

    @Nonnull
    private String logSeparator;
    private String log;
    private int count = 1;

    private transient FileHandler handler;

    public AbstractLogFileAuditLogger(String log, int count, String logSeparator) {
        this.log = Util.replaceMacro(log, EnvVars.masterEnvVars);
        this.count = count;
        this.logSeparator = Optional.ofNullable(logSeparator).orElse(DEFAULT_LOG_SEPARATOR);
    }

    @SuppressFBWarnings(
            value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            justification = "value can be null if no config file exists")
    Object readResolve() {
        if (logSeparator == null) {
            logSeparator = DEFAULT_LOG_SEPARATOR;
        }
        return this;
    }

    final void configure() {
        // looks like https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6244047 is somehow still there
        // there is no way for us to know before hand what path we are looking to create as it would
        // mean having access to FileHandler#generate so either reflexion or catching the exception and retrieving
        // the path. Let's go with number 2.
        try {
            FileHandler h = null;
            try {
                h = getLogFileHandler();
            } catch (NoSuchFileException ex) {
                LOGGER.info("Couldn't create the file handler lock file, forcing creation of intermediate directories");
                String lockFileName = ex.getFile();
                boolean mkdirs = new File(lockFileName).getParentFile().mkdirs();
                if (mkdirs) {
                    h = getLogFileHandler();
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
    public void log(String event) {
        if (handler == null) return;
        handler.publish(new LogRecord(CONFIG, event));
    }

    @Override
    public void cleanUp() throws SecurityException {
        if(handler != null) {
            handler.close();
        }
    }

    abstract FileHandler getLogFileHandler() throws IOException;

    @Nonnull
    public String getLogSeparator() {
        return logSeparator;
    }

    public String getLog() {
        return log;
    }

    public int getCount() {
        return count;
    }

    public FileHandler getHandler() {
        return handler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractLogFileAuditLogger that = (AbstractLogFileAuditLogger) o;

        if (count != that.count) return false;
        if (!logSeparator.equals(that.logSeparator)) return false;
        if (log != null ? !log.equals(that.log) : that.log != null) return false;
        return handler != null ? handler.equals(that.handler) : that.handler == null;
    }

    @Override
    public int hashCode() {
        int result = logSeparator.hashCode();
        result = 31 * result + (log != null ? log.hashCode() : 0);
        result = 31 * result + count;
        result = 31 * result + (handler != null ? handler.hashCode() : 0);
        return result;
    }
}
