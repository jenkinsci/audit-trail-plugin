package hudson.plugins.audit_trail;

import static hudson.plugins.audit_trail.AuditTrailRunListener.UNKNOWN_NODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

/**
 * Created by Pierre Beitz
 * on 31/12/2019.
 */
public class AuditTrailRunListenerTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Issue("JENKINS-12848")
    @Test
    public void jobParametersAreProperlyLogged() throws Exception {
        String logFileName = "jobParametersAreProperlyLogged.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("stringParam", "value1"),
                new BooleanParameterDefinition("booleanParam", false, "")));
        job.scheduleBuild2(0, new Cause.UserIdCause()).get();

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                "logged actions: " + log,
                Pattern.compile(
                                ".*, Parameters:\\[stringParam: \\{value1\\}, booleanParam: \\{false\\}\\].*",
                                Pattern.DOTALL)
                        .matcher(log)
                        .matches());
    }

    @Issue("JENKINS-12848")
    @Test
    public void jobWithoutParameterIsProperlyLogged() throws Exception {
        String logFileName = "jobWithoutParameterIsProperlyLogged.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.scheduleBuild2(0, new Cause.UserIdCause()).get();

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                "logged actions: " + log,
                Pattern.compile(".*, Parameters:\\[\\].*", Pattern.DOTALL)
                        .matcher(log)
                        .matches());
    }

    @Issue("JENKINS-12848")
    @Test
    public void jobWithSecretParameterIsProperlyLogged() throws Exception {
        String logFileName = "jobWithSecretParameterIsProperlyLogged.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.addProperty(
                new ParametersDefinitionProperty(new PasswordParameterDefinition("passParam", "thisIsASecret", "")));
        job.scheduleBuild2(0, new Cause.UserIdCause()).get();

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                "logged actions: " + log,
                Pattern.compile(".*, Parameters:\\[passParam: \\{\\*\\*\\*\\*\\}\\].*", Pattern.DOTALL)
                        .matcher(log)
                        .matches());
    }

    @Issue("JENKINS-62812")
    @Test
    public void ifSetToNotLogBuildCauseShouldNotLogThem() throws Exception {
        String logFileName = "ifSetToNotLogBuildCauseShouldNotLogThem.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile)
                .withLogBuildCause(false)
                .sendConfiguration(j, wc);

        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.addProperty(
                new ParametersDefinitionProperty(new PasswordParameterDefinition("passParam", "thisIsASecret", "")));
        job.scheduleBuild2(0, new Cause.UserIdCause()).get();

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(log.isEmpty());
    }

    @Issue("JENKINS-71637")
    @Test
    public void buildNodeNameIsProperlyExtractedFromTheRun() {
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
