package hudson.plugins.audit_trail;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.Util;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.slaves.DumbSlave;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class CredentialUsageListenerTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void jobCredentialUsageIsLogged() throws Exception {
        String logFileName = "jobCredentialUsageIsProperlyLogged.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        FreeStyleProject job = r.createFreeStyleProject("test-job");
        String id = "id";
        Credentials creds = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, id, "description", "username", "password");
        CredentialsProvider.track(job, creds);

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue("logged actions: " + log, Pattern.compile(".*test-job.*used credentials '" + id + "'.*", Pattern.DOTALL).matcher(log).matches());
    }

    @Test
    public void nodeCredentialUsageIsLogged() throws Exception {
        String logFileName = "nodeCredentialUsageIsProperlyLogged.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        DumbSlave dummyAgent = r.createSlave();
        dummyAgent.setNodeName("test-agent");
        String id = "id";
        Credentials creds = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, id, "description", "username", "password");
        CredentialsProvider.track(dummyAgent, creds);

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue("logged actions: " + log, Pattern.compile(".*test-agent.*used credentials '" + id + "'.*", Pattern.DOTALL).matcher(log).matches());
    }

    @Test
    public void itemCredentialUsageIsLogged() throws Exception {
        String logFileName = "itemCredentialUsageIsProperlyLogged.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);
        // 'Folder' because it is a non-traditional item to access credentials.
        Item item = r.createFolder("test-item");

        String id = "id";
        Credentials creds = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, id, "description", "username", "password");
        CredentialsProvider.track(item, creds);
        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue("logged actions: " + log, Pattern.compile(".*test-item.*used credentials '" + id + "'.*", Pattern.DOTALL).matcher(log).matches());
    }

    @Test
    public void disabledLoggingOptionIsRespected() throws Exception {
        String logFileName = "disabledCredentialUsageIsRespected.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).withLogCredentialsUsage(false).sendConfiguration(r, wc);

        FreeStyleProject job = r.createFreeStyleProject("test-job");
        String id = "id";
        Credentials creds = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, id, "description", "username", "password");
        CredentialsProvider.track(job, creds);

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(log.isEmpty());
    }
}
