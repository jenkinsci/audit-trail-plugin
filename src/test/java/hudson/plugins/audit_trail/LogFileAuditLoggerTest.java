package hudson.plugins.audit_trail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.EnvVars;
import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirDeletionStrategy.IgnoreFailures;
import org.jvnet.hudson.test.Issue;

/**
 * Created by Pierre Beitz
 * on 2019-05-05.
 */
class LogFileAuditLoggerTest {

    @TempDir(deletionStrategy = IgnoreFailures.class)
    private File folder;

    @Issue("JENKINS-56108")
    @Test
    void configuringAFileLoggerWithNonExistingParents() {
        Path logFile = folder.toPath().resolve("subdirectory").resolve("file");
        new LogFileAuditLogger(logFile.toString(), 5, 1, null);
        assertTrue(logFile.toFile().exists());
    }

    @Issue("JENKINS-67493")
    @Test
    void environmentVariablesAreProperlyExpanded() {
        Path rootFolder = folder.toPath();
        EnvVars.masterEnvVars.put("EXPAND_ME", "expandMe");
        String logFile = rootFolder.resolve("${EXPAND_ME}").toString();
        LogFileAuditLogger logger = new LogFileAuditLogger(logFile, 5, 1, null);
        assertEquals(rootFolder.resolve("expandMe").toString(), logger.getLog());
    }
}
