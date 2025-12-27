package hudson.plugins.audit_trail;

import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.Util;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Pierre Beitz
 */
@WithJenkins
class AuditTrailFilterTest {
    public static final int LONG_DELAY = 50000;

    @TempDir
    Path tmpDir;

    @Test
    void cancelItemLogsTheQueryStringAndTheUser(JenkinsRule j) throws Exception {
        File logFile = tmpDir.resolve("test.log").toFile();
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.scheduleBuild2(LONG_DELAY, new Cause.UserIdCause());
        WebRequest request =
                new WebRequest(new URL(wc.createCrumbedUrl("queue/cancelItem") + "&id=1"), HttpMethod.POST);

        try {
            wc.getPage(request);
        } catch (FailingHttpStatusCodeException e) {
            // silently ignore, the API is currently undocumented and returns a 404
            // see https://issues.jenkins-ci.org/browse/JENKINS-21311
        }

        String log = Util.loadFile(tmpDir.resolve("test.log.0").toFile(), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(".*id=1.*job/test-job.*by \\QNA from 127.0.0.1\\E.*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                () -> "logged actions: " + log);
    }

    @Issue("JENKINS-15731")
    @Test
    void createItemLogsTheNewItemName(JenkinsRule j) throws Exception {
        File logFile = tmpDir.resolve("create-item.log").toFile();
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        String jobName = "Job With Space";
        HtmlPage configure = wc.goTo("view/all/newJob");
        HtmlForm form = configure.getFormByName("createItem");
        form.getInputByName("name").setValue(jobName);
        form.getInputByName("name").blur();
        // not clear to me why the input is not visible in the test (yet it exists in the page)
        // for some reason the two next calls are needed
        form.getInputByValue("hudson.model.FreeStyleProject").click(false, false, false, true, false, true, false);
        form.getInputByValue("hudson.model.FreeStyleProject").setChecked(true);
        wc.waitForBackgroundJavaScript(50);
        j.submit(form);

        String log = Util.loadFile(tmpDir.resolve("create-item.log.0").toFile(), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(".*createItem \\(" + jobName + "\\).*by \\QNA from 127.0.0.1\\E.*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                () -> "logged actions: " + log);
    }
}
