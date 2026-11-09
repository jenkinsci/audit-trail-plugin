package hudson.plugins.audit_trail;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.Util;
import hudson.cli.GroovyCommand;
import hudson.cli.GroovyshCommand;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirDeletionStrategy.IgnoreFailures;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

@WithJenkins
class ScriptUsageListenerTest {

    private JenkinsRule r;

    @TempDir(deletionStrategy = IgnoreFailures.class)
    private File tmpDir;

    private static final String SCRIPT = "println('light of the world')";

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        r = rule;
    }

    @Test
    void consoleUsageIsLogged() throws Exception {
        String logFileName = "consoleUsageIsProperlyLogged.log";
        File logFile = new File(tmpDir, logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        RequestDispatcher view = mock(RequestDispatcher.class);
        StaplerRequest req = mock(StaplerRequest.class);
        StaplerResponse rsp = mock(StaplerResponse.class);

        when(req.getMethod()).thenReturn("POST");
        when(req.getParameter("script")).thenReturn(SCRIPT);
        when(req.getView(r.jenkins, "_scriptText.jelly")).thenReturn(view);
        r.jenkins.doScriptText(req, rsp);

        String log = Util.loadFile(new File(tmpDir, logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(
                                ".*A groovy script was executed by user 'SYSTEM'\\. Origin: Script Console Controller\\..*The "
                                        + "executed script:.*" + Pattern.quote(SCRIPT) + ".*",
                                Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                "logged actions: " + log);
    }

    @Test
    void groovyCliUsageIsLogged() throws Exception {
        String logFileName = "cliUsageIsProperlyLogged.log";
        File logFile = new File(tmpDir, logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        GroovyCommand cmd = new GroovyCommand();
        cmd.script = "=";
        InputStream scriptStream = new ByteArrayInputStream(SCRIPT.getBytes(StandardCharsets.UTF_8));
        cmd.main(new ArrayList<>(), Locale.ENGLISH, scriptStream, System.out, System.err);

        String log = Util.loadFile(new File(tmpDir, logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(
                                ".*A groovy script was executed\\. Origin: CLI/GroovyCommand.*The executed script:.*"
                                        + Pattern.quote(SCRIPT) + ".*",
                                Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                "logged actions: " + log);
    }

    @Test
    void groovyShCliUsageIsLogged() throws Exception {
        String logFileName = "cliUsageIsProperlyLogged2.log";
        File logFile = new File(tmpDir, logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        GroovyshCommand cmd = new GroovyshCommand();
        InputStream scriptStream = new ByteArrayInputStream(SCRIPT.getBytes(StandardCharsets.UTF_8));
        cmd.main(new ArrayList<>(), Locale.ENGLISH, scriptStream, System.out, System.err);

        String log = Util.loadFile(new File(tmpDir, logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(
                                ".*A groovy script was executed\\. Origin: CLI/GroovySh.*The executed script:.*"
                                        + Pattern.quote(SCRIPT) + ".*",
                                Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                "logged actions: " + log);
    }

    @Test
    void disabledLoggingOptionIsRespected() throws Exception {
        String logFileName = "disabledCredentialUsageIsRespected.log";
        File logFile = new File(tmpDir, logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile)
                .withLogScriptUsage(false)
                .sendConfiguration(r, wc);

        GroovyCommand cmd = new GroovyCommand();
        cmd.script = "=";
        InputStream scriptStream = new ByteArrayInputStream(SCRIPT.getBytes(StandardCharsets.UTF_8));
        cmd.main(new ArrayList<>(), Locale.ENGLISH, scriptStream, System.out, System.err);

        String log = Util.loadFile(new File(tmpDir, logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(log.isEmpty());
    }
}
