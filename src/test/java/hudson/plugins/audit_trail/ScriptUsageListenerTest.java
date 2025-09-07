package hudson.plugins.audit_trail;

import static java.nio.charset.StandardCharsets.UTF_8;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

@WithJenkins
public class ScriptUsageListenerTest {

    @TempDir
    Path tmpDir;

    private final String script = "println('light of the world')";

    @Test
    void consoleUsageIsLogged(JenkinsRule r) throws Exception {
        String logFileName = "consoleUsageIsProperlyLogged.log";
        File logFile = tmpDir.resolve(logFileName).toFile();
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        RequestDispatcher view = mock(RequestDispatcher.class);
        StaplerRequest req = mock(StaplerRequest.class);
        StaplerResponse rsp = mock(StaplerResponse.class);

        when(req.getMethod()).thenReturn("POST");
        when(req.getParameter("script")).thenReturn(script);
        when(req.getView(r.jenkins, "_scriptText.jelly")).thenReturn(view);
        r.jenkins.doScriptText(req, rsp);

        var log = Util.loadFile(tmpDir.resolve(logFileName + ".0").toFile(), UTF_8);
        assertTrue(
                Pattern.compile(
                                ".*A groovy script was executed by user 'SYSTEM'\\. Origin: Script Console Controller\\..*The "
                                        + "executed script:.*" + Pattern.quote(script) + ".*",
                                Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                "logged actions: " + log);
    }

    @Test
    void groovyCliUsageIsLogged(JenkinsRule r) throws Exception {
        String logFileName = "cliUsageIsProperlyLogged.log";
        File logFile = tmpDir.resolve(logFileName).toFile();
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        GroovyCommand cmd = new GroovyCommand();
        cmd.script = "=";
        InputStream scriptStream = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        cmd.main(new ArrayList<>(), Locale.ENGLISH, scriptStream, System.out, System.err);

        var log = Util.loadFile(tmpDir.resolve(logFileName + ".0").toFile(), UTF_8);
        assertTrue(
                Pattern.compile(
                                ".*A groovy script was executed\\. Origin: CLI/GroovyCommand.*The executed script:.*"
                                        + Pattern.quote(script) + ".*",
                                Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                "logged actions: " + log);
    }

    @Test
    void groovyShCliUsageIsLogged(JenkinsRule r) throws Exception {
        String logFileName = "cliUsageIsProperlyLogged2.log";
        File logFile = tmpDir.resolve(logFileName).toFile();
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        GroovyshCommand cmd = new GroovyshCommand();
        InputStream scriptStream = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        cmd.main(new ArrayList<>(), Locale.ENGLISH, scriptStream, System.out, System.err);

        var log = Util.loadFile(tmpDir.resolve(logFileName + ".0").toFile(), UTF_8);
        assertTrue(
                Pattern.compile(
                                ".*A groovy script was executed\\. Origin: CLI/GroovySh.*The executed script:.*"
                                        + Pattern.quote(script) + ".*",
                                Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                "logged actions: " + log);
    }

    @Test
    void disabledLoggingOptionIsRespected(JenkinsRule r) throws Exception {
        String logFileName = "disabledCredentialUsageIsRespected.log";
        File logFile = tmpDir.resolve(logFileName).toFile();
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile)
                .withLogScriptUsage(false)
                .sendConfiguration(r, wc);

        GroovyCommand cmd = new GroovyCommand();
        cmd.script = "=";
        InputStream scriptStream = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        cmd.main(new ArrayList<>(), Locale.ENGLISH, scriptStream, System.out, System.err);

        var log = Util.loadFile(tmpDir.resolve(logFileName + ".0").toFile(), UTF_8);
        assertTrue(log.isEmpty());
    }
}
