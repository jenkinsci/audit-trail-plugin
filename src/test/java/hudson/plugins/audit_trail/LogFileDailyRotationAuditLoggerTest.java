package hudson.plugins.audit_trail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import hudson.Util;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class LogFileDailyRotationAuditLoggerTest {

    /**
     * Ensures that if Daily Rotation is enabled a subdirectory with the corresponded logger
     * file gets created
     */
    @Test
    void configuringAFileLoggerWithDailyRotationAndNonExistingParents(@TempDir Path folder) {
        Path logFile = folder.resolve("subdirectory").resolve("file");
        LogFileDailyRotationAuditLogger logFileAuditLogger =
                new LogFileDailyRotationAuditLogger(logFile.toString(), 1, null);
        Path logFileRotating = folder.resolve("subdirectory").resolve(logFileAuditLogger.computePattern());
        assertFalse(logFile.toFile().exists());
        assertTrue(logFileRotating.toFile().exists());
    }

    /**
     * A test that ensures that the logger reuses the same file if restarted within the same day
     */
    @Test
    void logFileIsReusedIfRestartedWithDailyRotation(@TempDir Path folder) throws IOException {
        Path logFile = folder.resolve("file");
        LogFileDailyRotationAuditLogger logFileAuditLogger =
                new LogFileDailyRotationAuditLogger(logFile.toString(), 0, null);
        logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line1");
        Path logFileRotating = folder.resolve(logFileAuditLogger.computePattern());
        assertTrue(logFileRotating.toFile().exists());
        logFileAuditLogger.cleanUp();
        LogFileDailyRotationAuditLogger logFileAuditLogger2 =
                new LogFileDailyRotationAuditLogger(logFile.toString(), 0, null);
        logFileAuditLogger2.log("configuringAFileLoggerRotatingDaily - line2");

        String directoryPath = logFile.toFile().getParent();
        Collection<File> directoryFiles = FileUtils.listFiles(
                new File(directoryPath),
                new RegexFileFilter(".*" + logFile.toFile().getName()
                        + LogFileDailyRotationAuditLogger.DAILY_ROTATING_FILE_REGEX_PATTERN),
                DirectoryFileFilter.DIRECTORY);
        assertEquals(1, directoryFiles.size());

        String log = Util.loadFile(logFileRotating.toFile(), StandardCharsets.UTF_8);
        assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line1"));
        assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line2"));
    }

    /**
     * A test that ensures that the log file rotates in the next day
     */
    @Test
    void logFileProperlyRotatingInNextDayWithDailyRotation(@TempDir Path folder) throws IOException {
        ZonedDateTime zonedDateTime1 = ZonedDateTime.now();
        ZonedDateTime zonedDateTime2 = zonedDateTime1.plusDays(1);

        try (MockedStatic<ZonedDateTime> mockedLocalDateTime = Mockito.mockStatic(
                ZonedDateTime.class, Mockito.withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS))) {
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime1);
            // Check that the log file is created with the corresponded format (Today date)
            Path logFile = folder.resolve("file");
            LogFileDailyRotationAuditLogger logFileAuditLogger =
                    new LogFileDailyRotationAuditLogger(logFile.toString(), 2, null);
            logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line1");
            Path logFileRotating = folder.resolve(logFileAuditLogger.computePattern());
            assertTrue(logFileRotating.toFile().exists());

            // Check that there is ONLY one file generated at this point (Today date)
            String directoryPath = logFile.toFile().getParent();
            Collection<File> directoryFiles = FileUtils.listFiles(
                    new File(directoryPath),
                    new RegexFileFilter(".*" + logFile.toFile().getName()
                            + LogFileDailyRotationAuditLogger.DAILY_ROTATING_FILE_REGEX_PATTERN),
                    DirectoryFileFilter.DIRECTORY);
            assertEquals(1, directoryFiles.size());

            // Log something and check it appears in the logger file
            String log = Util.loadFile(logFileRotating.toFile(), StandardCharsets.UTF_8);
            assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line1"));

            // Increase +1 day
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime2);

            // Log something else
            logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line2");

            // Check that the corresponded is the ONLY one which appear on this file (Today +1)
            logFileRotating = folder.resolve(logFileAuditLogger.computePattern());
            log = Util.loadFile(logFileRotating.toFile(), StandardCharsets.UTF_8);
            assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line2"));
            assertFalse(log.contains("configuringAFileLoggerRotatingDaily - line1"));
        }
    }

    /**
     * A test that ensures that old files get removed after rotation
     */
    @Test
    void oldLogFilesProperlyRemovedWithDailyRotation(@TempDir Path folder) throws IOException {
        // test seems to be flaky on Windows, let's skip it for now I have no Windows machine to debug
        assumeTrue(!System.getProperty("os.name").toLowerCase().contains("windows"));
        ZonedDateTime zonedDateTime1 = ZonedDateTime.now();
        ZonedDateTime zonedDateTime2 = zonedDateTime1.plusDays(1);
        ZonedDateTime zonedDateTime3 = zonedDateTime1.plusDays(3);

        try (MockedStatic<ZonedDateTime> mockedLocalDateTime = Mockito.mockStatic(
                ZonedDateTime.class, Mockito.withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS))) {
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime1);
            Path logFile = folder.resolve("file");
            LogFileDailyRotationAuditLogger logFileAuditLogger =
                    new LogFileDailyRotationAuditLogger(logFile.toString(), 2, null);

            // Today: Log something
            logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line1");
            File logFileRotating1 =
                    folder.resolve(logFileAuditLogger.computePattern()).toFile();

            // Today+1 Log something
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime2);
            logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line2");
            File logFileRotating2 =
                    folder.resolve(logFileAuditLogger.computePattern()).toFile();

            // Today+2 Log something
            mockedLocalDateTime.when(ZonedDateTime::now).thenReturn(zonedDateTime3);
            logFileAuditLogger.log("configuringAFileLoggerRotatingDaily - line3");
            File logFileRotating3 =
                    folder.resolve(logFileAuditLogger.computePattern()).toFile();

            // Check that the oldest file got removed after rotation
            assertFalse(logFileRotating1.exists());
            assertTrue(logFileRotating2.exists());
            assertTrue(logFileRotating3.exists());

            // Check that that files contains their expected content
            String log = Util.loadFile(logFileRotating2, StandardCharsets.UTF_8);
            assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line2"));
            log = Util.loadFile(logFileRotating3, StandardCharsets.UTF_8);
            assertTrue(log.contains("configuringAFileLoggerRotatingDaily - line3"));

            // Check that there are only two log files
            String directoryPath = logFile.toFile().getParent();
            Collection<File> directoryFiles = FileUtils.listFiles(
                    new File(directoryPath),
                    new RegexFileFilter(".*" + logFile.toFile().getName()
                            + LogFileDailyRotationAuditLogger.DAILY_ROTATING_FILE_REGEX_PATTERN),
                    DirectoryFileFilter.DIRECTORY);
            assertEquals(2, directoryFiles.size());
        }
    }
}
