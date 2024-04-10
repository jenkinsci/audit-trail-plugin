package hudson.plugins.audit_trail;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.Util;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.slaves.DumbSlave;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CredentialUsageListenerTest {

    @Test
    void jobCredentialUsageIsLogged(JenkinsRule r, @TempDir Path tmpDir) throws Exception {
        String logFileName = "jobCredentialUsageIsProperlyLogged.log";
        File logFile = new File(tmpDir.toFile(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        FreeStyleProject job = r.createFreeStyleProject("test-job");
        String id = "id";
        Credentials creds =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, "description", "username", "password");
        CredentialsProvider.track(job, creds);

        String log = Util.loadFile(new File(tmpDir.toFile(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(".*test-job.*used credentials '" + id + "'.*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                () -> "logged actions: " + log);
    }

    @Test
    void nodeCredentialUsageIsLogged(JenkinsRule r, @TempDir Path tmpDir) throws Exception {
        String logFileName = "nodeCredentialUsageIsProperlyLogged.log";
        File logFile = new File(tmpDir.toFile(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        DumbSlave dummyAgent = r.createSlave();
        dummyAgent.setNodeName("test-agent");
        String id = "id";
        Credentials creds =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, "description", "username", "password");
        CredentialsProvider.track(dummyAgent, creds);

        String log = Util.loadFile(new File(tmpDir.toFile(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(".*test-agent.*used credentials '" + id + "'.*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                () -> "logged actions: " + log);
    }

    @Test
    void itemCredentialUsageIsLogged(JenkinsRule r, @TempDir Path tmpDir) throws Exception {
        String logFileName = "itemCredentialUsageIsProperlyLogged.log";
        File logFile = new File(tmpDir.toFile(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);
        // 'Folder' because it is a non-traditional item to access credentials.
        Item item = r.createFolder("test-item");

        String id = "id";
        Credentials creds =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, "description", "username", "password");
        CredentialsProvider.track(item, creds);
        String log = Util.loadFile(new File(tmpDir.toFile(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(".*test-item.*used credentials '" + id + "'.*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                () -> "logged actions: " + log);
    }

    @Test
    void disabledLoggingOptionIsRespected(JenkinsRule r, @TempDir Path tmpDir) throws Exception {
        String logFileName = "disabledCredentialUsageIsRespected.log";
        File logFile = new File(tmpDir.toFile(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile)
                .withLogCredentialsUsage(false)
                .sendConfiguration(r, wc);

        FreeStyleProject job = r.createFreeStyleProject("test-job");
        String id = "id";
        Credentials creds =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, "description", "username", "password");
        CredentialsProvider.track(job, creds);

        String log = Util.loadFile(new File(tmpDir.toFile(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(log.isEmpty());
    }
}
