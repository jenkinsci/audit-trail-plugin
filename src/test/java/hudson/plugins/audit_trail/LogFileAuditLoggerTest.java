package hudson.plugins.audit_trail;

import hudson.EnvVars;
import hudson.Util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Collection;

/**
 * Created by Pierre Beitz
 * on 2019-05-05.
 */
public class LogFileAuditLoggerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Issue("JENKINS-56108")
    @Test
    public void configuringAFileLoggerWithNonExistingParents() {
        Path logFile = folder.getRoot().toPath().resolve("subdirectory").resolve("file");
        new LogFileAuditLogger(logFile.toString(), 5, 1, null, false);
        Assert.assertTrue(logFile.toFile().exists());
    }

    @Issue("JENKINS-67493")
    @Test
    public void environmentVariablesAreProperlyExpanded() {
        Path rootFolder = folder.getRoot().toPath();
        EnvVars.masterEnvVars.put("EXPAND_ME", "expandMe");
        String logFile = rootFolder.resolve("${EXPAND_ME}").toString();
        LogFileAuditLogger logger = new LogFileAuditLogger(logFile, 5, 1, null, false);
        Assert.assertEquals(rootFolder.resolve("expandMe").toString(), logger.getLog());
    }

    /**
     * Ensures that if Daily Rotation is enabled a subdirectory with the corresponded logger
     * file gets created
     */
    @Test
    public void configuringAFileLoggerWithDailyRotationAndNonExistingParents() {
        Path logFile = folder.getRoot().toPath().resolve("subdirectory").resolve("file");
        LogFileAuditLogger logFileAuditLogger = new LogFileAuditLogger(logFile.toString(), 0, 1, null, true);
        Path logFileRotating = folder.getRoot().toPath().resolve("subdirectory").resolve(logFileAuditLogger.computePattern());
        Assert.assertFalse(logFile.toFile().exists());
        Assert.assertTrue(logFileRotating.toFile().exists());
    }

    /**
     * A test that ensures that the logger reuses the same file if restarted within the same day
     */
    @Test
    public void logFileIsReusedIfRestartedWithDailyRotation() throws IOException {
        Path logFile = folder.getRoot().toPath().resolve("file");
        LogFileAuditLogger logFileAuditLogger = new LogFileAuditLogger(logFile.toString(), 0, 1, null, true);
        logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line1");
        Path logFileRotating = folder.getRoot().toPath().resolve(logFileAuditLogger.computePattern());
        Assert.assertTrue(logFileRotating.toFile().exists());
        logFileAuditLogger.cleanUp();
        LogFileAuditLogger logFileAuditLogger2 = new LogFileAuditLogger(logFile.toString(), 0, 1, null, true);
        logFileAuditLogger2.log("configuringAFileLoggerRotatingDaily - line2");

        String directoryPath = logFile.toFile().getParent();
        Collection<File> directoryFiles = FileUtils.listFiles(new File(directoryPath), new RegexFileFilter(".*" + logFile.toFile().getName() + LogFileAuditLogger.DAILY_ROTATING_FILE_REGEX_PATTERN), DirectoryFileFilter.DIRECTORY);
        Assert.assertEquals(directoryFiles.size(), 1);

        String log = Util.loadFile(logFileRotating.toFile(), StandardCharsets.UTF_8);
        Assert.assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line1"));
        Assert.assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line2"));
    }

    /**
     * A test that ensures that the log file rotates in the next day
     */
    @Test
    public void logFileProperlyRotatingInNextDayWithDailyRotation() throws IOException {
        ZonedDateTime zonedDateTime1 = ZonedDateTime.now();
        ZonedDateTime zonedDateTime2 = zonedDateTime1.plusDays(1);

        try (MockedStatic<ZonedDateTime> mockedLocalDateTime = Mockito.mockStatic(ZonedDateTime.class, Mockito.withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS))) {
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime1);
            // Check that the log file is created with the corresponded format (Today date)
            Path logFile = folder.getRoot().toPath().resolve("file");
            LogFileAuditLogger logFileAuditLogger = new LogFileAuditLogger(logFile.toString(), 0, 2, null, true);
            logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line1");
            Path logFileRotating = folder.getRoot().toPath().resolve(logFileAuditLogger.computePattern());
            Assert.assertTrue(logFileRotating.toFile().exists());

            // Check that there is ONLY one file generated at this point (Today date)
            String directoryPath = logFile.toFile().getParent();
            Collection<File> directoryFiles = FileUtils.listFiles(new File(directoryPath), new RegexFileFilter(".*" + logFile.toFile().getName() + LogFileAuditLogger.DAILY_ROTATING_FILE_REGEX_PATTERN), DirectoryFileFilter.DIRECTORY);
            Assert.assertEquals(directoryFiles.size(), 1);

            // Log something and check it appears in the logger file
            String log = Util.loadFile(logFileRotating.toFile(), StandardCharsets.UTF_8);
            Assert.assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line1"));

            // Increase +1 day
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime2);

            // Log something else
            logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line2");

            // Check that the corresponded is the ONLY one which appear on this file (Today +1)
            logFileRotating = folder.getRoot().toPath().resolve(logFileAuditLogger.computePattern());
            log = Util.loadFile(logFileRotating.toFile(), StandardCharsets.UTF_8);
            Assert.assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line2"));
            Assert.assertFalse(log.contains("configuringAFileLoggerRotatingDaily - line1"));
        }
    }

    /**
     * A test that ensures that old files get removed after rotation
     */
    @Test
    public void oldLogFilesProperlyRemovedWithDailyRotation() throws IOException {
        ZonedDateTime zonedDateTime1 = ZonedDateTime.now();
        ZonedDateTime zonedDateTime2 = zonedDateTime1.plusDays(1);
        ZonedDateTime zonedDateTime3 = zonedDateTime1.plusDays(3);

        try (MockedStatic<ZonedDateTime> mockedLocalDateTime = Mockito.mockStatic(ZonedDateTime.class, Mockito.withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS))) {
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime1);
            Path logFile = folder.getRoot().toPath().resolve("file");
            LogFileAuditLogger logFileAuditLogger = new LogFileAuditLogger(logFile.toString(), 0, 2, null, true);

            // Today: Log something
            logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line1");
            File logFileRotating1 = folder.getRoot().toPath().resolve(logFileAuditLogger.computePattern()).toFile();

            // Today+1 Log something
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime2);
            logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line2");
            File logFileRotating2 = folder.getRoot().toPath().resolve(logFileAuditLogger.computePattern()).toFile();

            // Today+2 Log something
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime3);
            logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line3");
            File logFileRotating3 = folder.getRoot().toPath().resolve(logFileAuditLogger.computePattern()).toFile();

            // Check that the oldest file got removed after rotation
            Assert.assertFalse(logFileRotating1.exists());
            Assert.assertTrue(logFileRotating2.exists());
            Assert.assertTrue(logFileRotating3.exists());

            // Check that that files contains their expected content
            String log = Util.loadFile(logFileRotating2, StandardCharsets.UTF_8);
            Assert.assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line2"));
            log = Util.loadFile(logFileRotating3, StandardCharsets.UTF_8);
            Assert.assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line3"));

            // Check that there are only two log files
            String directoryPath = logFile.toFile().getParent();
            Collection<File> directoryFiles = FileUtils.listFiles(new File(directoryPath), new RegexFileFilter(".*" + logFile.toFile().getName() + LogFileAuditLogger.DAILY_ROTATING_FILE_REGEX_PATTERN), DirectoryFileFilter.DIRECTORY);
            Assert.assertEquals(directoryFiles.size(), 2);
        }
    }
}