package hudson.plugins.audit_trail;

import static hudson.plugins.audit_trail.AuditTrailRunListener.UNKNOWN_NODE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BooleanParameterDefinition;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PasswordParameterDefinition;
import hudson.model.Run;
import hudson.model.StringParameterDefinition;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mockito;

/**
 * @author Pierre Beitz
 */
@WithJenkins
class AuditTrailRunListenerTest {

    @TempDir
    Path tmpDir;

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(tmpDir.toFile());
    }

    @Issue("JENKINS-12848")
    @Test
    void jobParametersAreProperlyLogged(JenkinsRule j) throws Exception {
        String logFileName = "jobParametersAreProperlyLogged.log";
        File logFile = tmpDir.resolve(logFileName).toFile();
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("stringParam", "value1"),
                new BooleanParameterDefinition("booleanParam", false, "")));
        job.scheduleBuild2(0, new Cause.UserIdCause()).get();

        String log = Util.loadFile(tmpDir.resolve(logFileName + ".0").toFile(), UTF_8);
        assertTrue(
                Pattern.compile(
                                ".*, Parameters:\\[stringParam: \\{value1\\}, booleanParam: \\{false\\}\\].*",
                                Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                () -> "logged actions: " + log);
    }

    @Issue("JENKINS-12848")
    @Test
    void jobWithoutParameterIsProperlyLogged(JenkinsRule j) throws Exception {
        String logFileName = "jobWithoutParameterIsProperlyLogged.log";
        File logFile = tmpDir.resolve(logFileName).toFile();
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.scheduleBuild2(0, new Cause.UserIdCause()).get();

        String log = Util.loadFile(tmpDir.resolve(logFileName + ".0").toFile(), UTF_8);
        assertTrue(
                Pattern.compile(".*, Parameters:\\[\\].*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                () -> "logged actions: " + log);
    }

    @Issue("JENKINS-12848")
    @Test
    void jobWithSecretParameterIsProperlyLogged(JenkinsRule j) throws Exception {
        String logFileName = "jobWithSecretParameterIsProperlyLogged.log";
        File logFile = tmpDir.resolve(logFileName).toFile();
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.addProperty(
                new ParametersDefinitionProperty(new PasswordParameterDefinition("passParam", "thisIsASecret", "")));
        job.scheduleBuild2(0, new Cause.UserIdCause()).get();

        String log = Util.loadFile(tmpDir.resolve(logFileName + ".0").toFile(), UTF_8);
        assertTrue(
                Pattern.compile(".*, Parameters:\\[passParam: \\{\\*\\*\\*\\*\\}\\].*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                () -> "logged actions: " + log);
    }

    @Issue("JENKINS-62812")
    @Test
    void ifSetToNotLogBuildCauseShouldNotLogThem(JenkinsRule j) throws Exception {
        String logFileName = "ifSetToNotLogBuildCauseShouldNotLogThem.log";
        File logFile = tmpDir.resolve(logFileName).toFile();
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile)
                .withLogBuildCause(false)
                .sendConfiguration(j, wc);

        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.addProperty(
                new ParametersDefinitionProperty(new PasswordParameterDefinition("passParam", "thisIsASecret", "")));
        job.scheduleBuild2(0, new Cause.UserIdCause()).get();

        String log = Util.loadFile(tmpDir.resolve(logFileName + ".0").toFile(), UTF_8);
        assertTrue(log.isEmpty());
    }

    @Issue("JENKINS-71637")
    @Test
    void buildNodeNameIsProperlyExtractedFromTheRun() {
        var listener = new AuditTrailRunListener();

        var notAbstractBuild = Mockito.mock(Run.class);
        assertEquals(UNKNOWN_NODE, listener.buildNodeName(notAbstractBuild));

        var abstractBuildWithAnExistingNode = Mockito.mock(AbstractBuild.class);
        var aNode = Mockito.mock(Node.class);
        Mockito.when(aNode.getDisplayName()).thenReturn("hello");
        Mockito.when(abstractBuildWithAnExistingNode.getBuiltOn()).thenReturn(aNode);
        assertEquals("hello", listener.buildNodeName(abstractBuildWithAnExistingNode));

        var abstractBuildWithANonExistingNode = Mockito.mock(AbstractBuild.class);
        Mockito.when(abstractBuildWithANonExistingNode.getBuiltOnStr()).thenReturn("world");
        assertEquals("world", listener.buildNodeName(abstractBuildWithANonExistingNode));
    }
}
