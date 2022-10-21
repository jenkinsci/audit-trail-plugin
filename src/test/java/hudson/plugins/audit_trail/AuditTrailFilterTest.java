package hudson.plugins.audit_trail;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.Util;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
        assertTrue("logged actions: " + log, Pattern.compile(".*id=1.*job/test-job.*by \\QNA from 127.0.0.1\\E.*", Pattern.DOTALL).matcher(log).matches());
    }

    @Issue("JENKINS-15731")
    @Test
    public void createItemLogsTheNewItemName() throws Exception {
        File logFile = new File(tmpDir.getRoot(), "create-item.log");
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        String jobName = "Job With Space";
        HtmlPage configure = wc.goTo("view/all/newJob");
        HtmlForm form = configure.getFormByName("createItem");
        form.getInputByName("name").setValueAttribute(jobName);
        form.getInputByName("name").blur();
        // not clear to me why the input is not visible in the test (yet it exists in the page)
        // for some reason the two next calls are needed
        form.getInputByValue("hudson.model.FreeStyleProject").click(false, false, false, true, false, true, false);
        form.getInputByValue("hudson.model.FreeStyleProject").setChecked(true);
        wc.waitForBackgroundJavaScript(50);
        j.submit(form);

        String log = Util.loadFile(new File(tmpDir.getRoot(), "create-item.log.0"), StandardCharsets.UTF_8);
        assertTrue("logged actions: " + log, Pattern.compile(".*createItem \\(" + jobName + "\\).*by \\QNA from 127.0.0.1\\E.*", Pattern.DOTALL).matcher(log).matches());
    }

    @Test
    public void createItemLogsTheNewItemNameWithRotateDaily() throws Exception {
        File logFile = new File(tmpDir.getRoot(), "create-item.log");
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfigurationToRotateDaily(j, wc);

        String jobName = "Job With Space";
        HtmlPage configure = wc.goTo("view/all/newJob");
        HtmlForm form = configure.getFormByName("createItem");
        form.getInputByName("name").setValueAttribute(jobName);
        form.getInputByName("name").blur();
        // not clear to me why the input is not visible in the test (yet it exists in the page)
        // for some reason the two next calls are needed
        form.getInputByValue("hudson.model.FreeStyleProject").click(false, false, false, true, false, true, false);
        form.getInputByValue("hudson.model.FreeStyleProject").setChecked(true);
        wc.waitForBackgroundJavaScript(50);
        j.submit(form);

        ZonedDateTime initInstant = ZonedDateTime.now();
        String logRotateComputedName = LogFileAuditLogger.computePattern(initInstant, Paths.get(logFile.getPath()));

        // Check that a file with the corresponded expected format was created
        Path logFileRotating = tmpDir.getRoot().toPath().resolve(logRotateComputedName);
        Assert.assertTrue(logFileRotating.toFile().exists());

        // Check that the action was logged in the file
        String log = Util.loadFile(new File(tmpDir.getRoot(), logRotateComputedName), StandardCharsets.UTF_8);
        assertTrue("logged actions: " + log, Pattern.compile(".*createItem \\(" + jobName + "\\).*by \\QNA from 127.0.0.1\\E.*", Pattern.DOTALL).matcher(log).matches());

        // Check that there is only one daily log file in the directory
        String directoryPath = logFile.getParent();
        Collection<File> directoryFiles = FileUtils.listFiles(new File(directoryPath), new RegexFileFilter(".*" + logFile.getName() + LogFileAuditLogger.DAILY_ROTATING_FILE_REGEX_PATTERN), DirectoryFileFilter.DIRECTORY);
        assertThat(directoryFiles.size(), is(1));
    }
}
