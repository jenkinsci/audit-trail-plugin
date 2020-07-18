package hudson.plugins.audit_trail;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequest;
import hudson.Util;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * Created by Pierre Beitz
 * on 18/11/2019.
 */
public class AuditTrailFilterTest {
    public static final int LONG_DELAY = 50000;
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void cancelItemLogsTheQueryStringAndTheUser() throws Exception {
        File logFile = new File(tmpDir.getRoot(), "test.log");
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.scheduleBuild2(LONG_DELAY, new Cause.UserIdCause());
        WebRequest request = new WebRequest(new URL(wc.createCrumbedUrl("queue/cancelItem") + "&id=1"), HttpMethod.POST);

        try {
            wc.getPage(request);
        } catch (FailingHttpStatusCodeException e) {
            // silently ignore, the API is currently undocumented and returns a 404
            // see https://issues.jenkins-ci.org/browse/JENKINS-21311
        }

        String log = Util.loadFile(new File(tmpDir.getRoot(), "test.log.0"), StandardCharsets.UTF_8);
        assertTrue("logged actions: " + log, Pattern.compile(".*id=1.*job/test-job.*by \\Q127.0.0.1\\E.*", Pattern.DOTALL).matcher(log).matches());
    }
}
