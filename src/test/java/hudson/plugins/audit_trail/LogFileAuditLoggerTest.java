package hudson.plugins.audit_trail;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;

import java.nio.file.Path;

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
        LogFileAuditLogger logFileAuditLogger = new LogFileAuditLogger(logFile.toString(), 5, 1);
        logFileAuditLogger.configure();
        Assert.assertTrue(logFile.toFile().exists());
    }
}
