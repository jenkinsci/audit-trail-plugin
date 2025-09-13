package hudson.plugins.audit_trail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.EnvVars;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.Issue;

/**
 * @author Pierre Beitz
 */
class LogFileAuditLoggerTest {

    @TempDir
    Path folder;

    @Issue("JENKINS-56108")
    @Test
    void configuringAFileLoggerWithNonExistingParents() {
        Path logFile = folder.resolve("subdirectory").resolve("file");
        new LogFileAuditLogger(logFile.toString(), 5, 1, null);
        assertTrue(logFile.toFile().exists());
    }

    @Issue("JENKINS-67493")
    @Test
    void environmentVariablesAreProperlyExpanded() {
        EnvVars.masterEnvVars.put("EXPAND_ME", "expandMe");
        String logFile = folder.resolve("${EXPAND_ME}").toString();
        LogFileAuditLogger logger = new LogFileAuditLogger(logFile, 5, 1, null);
        assertEquals(folder.resolve("expandMe").toString(), logger.getLog());
    }
}
