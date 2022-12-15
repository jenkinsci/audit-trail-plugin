package hudson.plugins.audit_trail;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.logging.Level.CONFIG;
import static org.apache.commons.io.comparator.LastModifiedFileComparator.LASTMODIFIED_REVERSE;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @author Pierre Beitz
 */
public class LogFileAuditLogger extends AuditLogger {

    private static final Logger LOGGER = Logger.getLogger(LogFileAuditLogger.class.getName());
    static final String DEFAULT_LOG_SEPARATOR=" ";
    protected static final String DAILY_ROTATING_FILE_REGEX_PATTERN = "-[0-9]{4}-[0-9]{2}-[0-9]{2}" + ".*" + "(?<!lck)$";
    @Nonnull
    private String logSeparator;

    /**
     * If true, enables the daily rotation of the logs. Limit will be configure with a 0 value
     * (unlimitted size), and count will be set-up as 1 for FileHandler.
     * @see #getRotateDaily()
     */
    public boolean rotateDaily;

    private transient FileHandler handler;
    private transient ZonedDateTime initInstant;
    private transient Path basePattern;
    private transient final Object monitor = new Object();

    @Deprecated
    public LogFileAuditLogger(String log, int limit, int count, String logSeparator) {
        this(log, limit, count, logSeparator, false);
    }

    @DataBoundConstructor
    public LogFileAuditLogger(String log, int limit, int count, String logSeparator, boolean rotateDaily) {
        this.log = Util.replaceMacro(log, EnvVars.masterEnvVars);
        this.basePattern = Paths.get(log);
        this.limit = rotateDaily ? 0 : limit;
        this.count = count;
        this.logSeparator = Optional.ofNullable(logSeparator).orElse(DEFAULT_LOG_SEPARATOR);
        this.rotateDaily = rotateDaily;

        if (rotateDaily) {
            initializeDailyRotation();
        } else {
            configure();
        }
    }

    /**
     * Initializes initInstant to the instant obtained from the latest daily
     * log file saved on disk (if present), or if not present, to the current instant.
     */
    private void initializeDailyRotation() {
        Path directoryPath = basePattern.getParent();
        boolean directoryExists = false;
        if (directoryPath != null) {
            directoryExists = directoryPath.toFile().exists();
        }

        if (directoryExists) {
            // audit-log.log-2022-10-19-15-50.0
            Collection<File> files = FileUtils.listFiles(new File(directoryPath.toString()), new RegexFileFilter(".*" + FilenameUtils.getName(basePattern.toString()) + DAILY_ROTATING_FILE_REGEX_PATTERN), DirectoryFileFilter.DIRECTORY);
            if (files.size() > 0) {
                List<File> orderedList = files.stream().sorted(LASTMODIFIED_REVERSE).collect(Collectors.toList());
                File lastFile = orderedList.get(0);
                Matcher matcher = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}").matcher(lastFile.getName());
                if (matcher.find()) {
                    // Initialize initInstant with the date saved on the audit file name
                    // See example https://stackoverflow.com/questions/14385834/java-regex-group-0
                    initInstant = Instant.parse(matcher.group(0) + "T00:00:00.00Z").atZone(ZoneId.systemDefault());
                }
            }
        }
        // Initialize initInstant to the current instant
        if (initInstant == null) {
            initInstant = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
        }
        configure();
    }

    private boolean shouldRotate() {
        return ZonedDateTime.now().isAfter(initInstant.plus(Duration.ofDays(1)));
    }

    /**
     * Rotates the daily rotation logger
     */
    private void rotate() {
        if (handler != null) {
            handler.close();
        }
        initInstant = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
        configure();
        // After rotation we remove old files if needed
        removeOldFiles();
    }

    protected static String computePattern(ZonedDateTime initInstant, Path basePattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
        String formattedInstant = formatter.format(initInstant);
        String computedFileName = String.format("%s-%s", FilenameUtils.getName(basePattern.toString()) , formattedInstant);
        return computedFileName;
    }

    protected String computePattern() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
        String formattedInstant = formatter.format(initInstant);
        String computedFileName = String.format("%s-%s", FilenameUtils.getName(basePattern.toString()) , formattedInstant);
        Path parentFolder = basePattern.getParent();
        if (parentFolder != null) {
            return parentFolder.resolve(computedFileName).toString();
        }
        return computedFileName;
    }

    private void removeOldFiles() {
        Path directoryPath = basePattern.getParent();
        if (directoryPath != null) {
            Collection<File> files = FileUtils.listFiles(directoryPath.toFile(), new RegexFileFilter(".*" + FilenameUtils.getName(basePattern.toString()) + DAILY_ROTATING_FILE_REGEX_PATTERN), DirectoryFileFilter.DIRECTORY);
            if (files.size() > count) {
                List<File> orderedList = files.stream().sorted(LASTMODIFIED_REVERSE).collect(Collectors.toList());
                List<File> toDelete = orderedList.subList(count, orderedList.size());
                for (File file : toDelete) {
                    if (!file.delete()) {
                        LOGGER.log(Level.SEVERE, "File {0} could not be removed on rotate overation", file.getName());
                    }
                }
            }
        }
    }

    @SuppressFBWarnings(
          value="RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
          justification = "value can be null if no config file exists")
    private Object readResolve() {
        if(logSeparator == null) {
            logSeparator = DEFAULT_LOG_SEPARATOR;
        }
        if (rotateDaily) {
            this.basePattern = Paths.get(log);
            initializeDailyRotation();
        } else {
            configure();
        }
        return this;
    }

    @Override
    public void log(String event) {
        // to avoid synchronizing the whole method
        if (rotateDaily && shouldRotate()) {
            synchronized (monitor) {
                rotate();
            }
        }
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

    public boolean getRotateDaily() {
        return rotateDaily;
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
            String logFilePath = (rotateDaily ? computePattern() : log);
            try {
                h = new FileHandler(logFilePath, rotateDaily ? 0 : limit * 1024 * 1024, rotateDaily ? 1 : count, true);
            } catch (NoSuchFileException ex) {
                LOGGER.info("Couldn't create the file handler lock file, forcing creation of intermediate directories");
                String lockFileName = ex.getFile();
                boolean mkdirs = new File(lockFileName).getParentFile().mkdirs();
                if (mkdirs) {
                    h = new FileHandler(logFilePath, rotateDaily ? 0 : limit * 1024 * 1024, rotateDaily ? 1 : count, true);
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
