package hudson.plugins.audit_trail;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import hudson.Util;
import hudson.model.labels.LabelAtom;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.RealJenkinsRule;

/**
 * Created by Pierre Beitz
 * on 16/08/2025.
 */
public class AuditTrailRunListenerFreestyleTest {

    @Rule
    public RealJenkinsRule rr = new RealJenkinsRule().omitPlugins("workflow-job");

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Issue("JENKINS-71637")
    @Test
    public void shouldLogPerStageWorkflowRunAgentNameWithoutPipelineInstalled() throws Throwable {
        var logFileName = "shouldLogPerStageWorkflowRunAgentName.log";
        var rootDir = tmpDir.getRoot();
        var logFile = new File(rootDir, logFileName);
        rr.startJenkins();
        rr.runRemotely(j -> {
            var wc = j.createWebClient();
            new SimpleAuditTrailPluginConfiguratorHelper(logFile)
                    .withLogBuildCause(true)
                    .sendConfiguration(j, wc);

            var node1Label = new LabelAtom("node-1");
            j.createOnlineSlave(node1Label);

            var freestyle = j.createFreeStyleProject("job-1");
            freestyle.setAssignedLabel(node1Label);
            freestyle.save();

            freestyle.scheduleBuild2(0).get();
            var log = Util.loadFile(new File(rootDir, logFileName + ".0"), StandardCharsets.UTF_8);
            assertThat(log, containsString("slave0"));
        });
    }
}
