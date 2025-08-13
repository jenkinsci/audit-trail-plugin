package hudson.plugins.audit_trail;

import static hudson.plugins.audit_trail.BasicNodeNameRetriever.UNKNOWN_NODE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import hudson.model.labels.LabelAtom;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
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

    @Issue("JENKINS-71637")
    @Test
    public void shouldWorkflowRunAgentName() throws Exception {
        var logFileName = "shouldLogWorkflowRunAgentName.log";
        var logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile)
                .withLogBuildCause(true)
                .sendConfiguration(j, wc);

        j.createOnlineSlave(new LabelAtom("node-1"));

        var workflowJob = j.createProject(WorkflowJob.class, "job-1");
        workflowJob.setDefinition(new CpsFlowDefinition(
                """
                        pipeline {
                          agent {
                            label 'node-1'
                          }
                          stages {
                            stage('single agent') {
                              steps {
                                echo 'hello'
                              }
                            }
                          }
                        }
                        """,
                true));
        workflowJob.save();
        var run = workflowJob.scheduleBuild2(0).get();

        System.out.println(run.getLog());

        var log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);

        // the API creates agents with name slaveN
        assertThat(log, containsString("slave0"));
    }

    @Issue("JENKINS-71637")
    @Test
    public void shouldLogPerStageWorkflowRunAgentName() throws Exception {
        var logFileName = "shouldLogPerStageWorkflowRunAgentName.log";
        var logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile)
                .withLogBuildCause(true)
                .sendConfiguration(j, wc);

        j.createOnlineSlave(new LabelAtom("node-1"));

        var workflowJob = j.createProject(WorkflowJob.class, "job-1");
        workflowJob.setDefinition(new CpsFlowDefinition(
                """
                        pipeline {
                          agent none
                          stages {
                            stage('first-agent') {
                              agent {
                                label 'built-in'
                              }
                              steps {
                                echo 'hello'
                              }
                            }
                            stage('second-agent') {
                              agent {
                                label 'node-1'
                              }
                              steps {
                                echo 'hello'
                              }
                            }
                          }
                        }
                        """,
                true));
        workflowJob.save();
        var run = workflowJob.scheduleBuild2(0).get();

        System.out.println(run.getLog());

        var log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);

        assertThat(log, containsString("Built-In Node"));
        // the API creates agents with name slaveN
        assertThat(log, containsString("slave0"));
    }
}
