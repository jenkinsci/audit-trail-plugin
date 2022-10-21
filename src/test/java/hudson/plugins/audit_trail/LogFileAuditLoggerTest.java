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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

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
}