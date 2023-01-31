package hudson.plugins.audit_trail;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Descriptor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.io.comparator.LastModifiedFileComparator.LASTMODIFIED_REVERSE;

public class LogFileDailyRotationAuditLogger extends AbstractLogFileAuditLogger {

    private static final Logger LOGGER = Logger.getLogger(LogFileDailyRotationAuditLogger.class.getName());
    static final String DAILY_ROTATING_FILE_REGEX_PATTERN = "-[0-9]{4}-[0-9]{2}-[0-9]{2}" + ".*" + "(?<!lck)$";

    private transient ZonedDateTime initInstant;
    private transient Path basePattern;

    String getLogFilePath() {
        return computePattern();
    }

    @Override
    FileHandler getLogFileHandler() throws IOException {
        return new FileHandler(getLogFilePath(), 0, 1, true);
    }

    @DataBoundConstructor
    public LogFileDailyRotationAuditLogger(String log, int count, String logSeparator) {
        super(log, count, logSeparator);
        this.basePattern = Paths.get(log);
        initializeDailyRotation();
    }

    @SuppressFBWarnings(
            value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            justification = "value can be null if no config file exists")
    Object readResolve() {
        this.basePattern = Paths.get(getLog());
        super.readResolve();
        initializeDailyRotation();
        return this;
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

    String computePattern() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
        String formattedInstant = formatter.format(initInstant);
        String computedFileName = String.format("%s-%s", FilenameUtils.getName(basePattern.toString()) , formattedInstant);
        Path parentFolder = basePattern.getParent();
        if (parentFolder != null) {
            return parentFolder.resolve(computedFileName).toString();
        }
        return computedFileName;
    }

    private boolean shouldRotate() {
        return ZonedDateTime.now().isAfter(initInstant.plus(Duration.ofDays(1)));
    }

    /**
     * Rotates the daily rotation logger
     */
    private void rotate() {
        if (getHandler() != null) {
            getHandler().close();
        }
        initInstant = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
        configure();
        // After rotating remove old files
        removeOldFiles();
    }

    private void removeOldFiles() {
        Path directoryPath = basePattern.getParent();
        if (directoryPath != null) {
            Collection<File> files = FileUtils.listFiles(directoryPath.toFile(), new RegexFileFilter(".*" + FilenameUtils.getName(basePattern.toString()) + DAILY_ROTATING_FILE_REGEX_PATTERN), DirectoryFileFilter.DIRECTORY);
            if (files.size() > getCount()) {
                List<File> orderedList = files.stream().sorted(LASTMODIFIED_REVERSE).collect(Collectors.toList());
                List<File> toDelete = orderedList.subList(getCount(), orderedList.size());
                for (File file : toDelete) {
                    if (!file.delete()) {
                        LOGGER.log(Level.SEVERE, "File {0} could not be removed on rotate overation", file.getName());
                    }
                }
            }
        }
    }

    @Override
    public void log(String event) {
        // to avoid synchronizing the whole method
        if (shouldRotate()) {
            synchronized (this) {
                if (shouldRotate()) rotate();
            }
        }
        super.log(event);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AuditLogger> {

        @Override
        public String getDisplayName() {
            return "Log file daily rotation";
        }
    }
}