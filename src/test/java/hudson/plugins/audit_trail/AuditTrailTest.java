/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Alan Harder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.audit_trail;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.Util;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Test interaction of audit-trail plugin with Jenkins core.
 *
 * @author Alan Harder
 */
public class AuditTrailTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private static final int TIMEOUT = 2000;
    private static final String LOG_LOCATION_INPUT_NAME = "_.log";
    private static final String LOG_FILE_SIZE_INPUT_NAME = "_.limit";
    private static final String LOG_FILE_COUNT_INPUT_NAME = "_.count";
    private static final String PATTERN_INPUT_NAME= "pattern";
    private static final String ADD_LOGGER_BUTTON_TEXT = "Add Logger";
    private static final String LOG_FILE_COMBO_TEXT = new LogFileAuditLogger.DescriptorImpl().getDisplayName();

    @After
    public void tearDown() {
        tmpDir.delete();
    }

    @Test
    public void shouldGenerateTwoAuditLogs() throws Exception {
        // Given
        // Configure plugin
        File logFile = new File(tmpDir.getRoot(), "test.log");
        JenkinsRule.WebClient wc = j.createWebClient();
        configurePlugin(j, logFile, wc);

        AuditTrailPlugin plugin = GlobalConfiguration.all().get(AuditTrailPlugin.class);
        LogFileAuditLogger logger = (LogFileAuditLogger) plugin.getLoggers().get(0);
        assertEquals("log path", logFile.getPath(), logger.getLog());
        assertEquals("log size", 1, logger.getLimit());
        assertEquals("log count", 2, logger.getCount());
        assertTrue("log build cause", plugin.getLogBuildCause());

        // When
        createJobAndPush();

        // Then
        String log = Util.loadFile(new File(tmpDir.getRoot(), "test.log.0"), Charset.forName("UTF-8"));
        assertTrue("logged actions: " + log, Pattern.compile(".* job/test-job/ #1 Started by user"
            + " .*job/test-job/enable by .*", Pattern.DOTALL).matcher(log).matches());
    }

    static void configurePlugin(JenkinsRule j, File logFile, JenkinsRule.WebClient wc) throws Exception {
        HtmlPage configure = wc.goTo("configure");
        HtmlForm form = configure.getFormByName("config");
        j.getButtonByCaption(form, ADD_LOGGER_BUTTON_TEXT).click();
        configure.getAnchorByText(LOG_FILE_COMBO_TEXT).click();
        wc.waitForBackgroundJavaScript(TIMEOUT);
        form.getInputByName(LOG_LOCATION_INPUT_NAME).setValueAttribute(logFile.getPath());
        form.getInputByName(LOG_FILE_SIZE_INPUT_NAME).setValueAttribute("1");
        form.getInputByName(LOG_FILE_COUNT_INPUT_NAME).setValueAttribute("2");
        form.getInputByName(PATTERN_INPUT_NAME).setValueAttribute(".*/(?:enable|cancelItem)");
        j.submit(form);
    }

    @Issue("JENKINS-44129")
    @Test
    public void shouldCorrectlyCleanUpFileHandlerOnApply() throws Exception {
        // Given
        JenkinsRule.WebClient wc = j.createWebClient();
        HtmlPage configure = wc.goTo("configure");
        HtmlForm form = configure.getFormByName("config");
        j.getButtonByCaption(form, ADD_LOGGER_BUTTON_TEXT).click();
        configure.getAnchorByText(LOG_FILE_COMBO_TEXT).click();
        wc.waitForBackgroundJavaScript(AuditTrailTest.TIMEOUT);
        File logFile = new File(tmpDir.getRoot(), "unique.log");
        form.getInputByName(LOG_LOCATION_INPUT_NAME).setValueAttribute(logFile.getPath());
        form.getInputByName(LOG_FILE_SIZE_INPUT_NAME).setValueAttribute("1");
        form.getInputByName(LOG_FILE_COUNT_INPUT_NAME).setValueAttribute("1");
        j.submit(form);

        // When
        j.submit(form);

        // Then
        assertEquals("Only two files should be present, the file opened by the FileHandler and its lock",
            2, tmpDir.getRoot().list().length);
    }

    @Issue("JENKINS-60421")
    @Test
    @ConfiguredWithCode("jcasc-console-and-file.yml")
    public void shouldGenerateAuditLogsWhenSetupWithJCasc() throws IOException, ExecutionException, InterruptedException {
        // the injected jcasc assumes the temp directory is /tmp so let's skip windows
        assumeTrue(!System.getProperty("os.name").toLowerCase().contains("windows"));

        createJobAndPush();

        // https://github.com/jenkinsci/configuration-as-code-plugin/issues/899#issuecomment-524641582 log is 1, not 0 because of this.
        // this means that we technically always open 1 file for nothing. This could be improved by delaying the opening of the file
        // after we are sure jcasc is done...
        // it's worth waiting for https://issues.jenkins-ci.org/browse/JENKINS-51856
        File logFile = new File("/tmp", "test.log.1");
        logFile.deleteOnExit();
        String log = Util.loadFile(logFile, StandardCharsets.UTF_8);
        assertTrue("logged actions: " + log, Pattern.compile(".* job/test-job/ #1 Started by user"
              + " .*job/test-job/enable by .*", Pattern.DOTALL).matcher(log).matches());

        // covering the console case will require refactoring as currently the console logger is directly using System.out/err
        // we need to properly inject those...
    }

    @Issue("JENKINS-60421")
    @Test
    public void loggerShouldBeProperlyConfiguredWhenLoadedFromXml() throws IOException, ExecutionException, InterruptedException {
        // the injected xml assumes the temp directory is /tmp so let's skip windows
        assumeTrue(!System.getProperty("os.name").toLowerCase().contains("windows"));

        AuditTrailPlugin plugin = load();
        LogFileAuditLogger logger = (LogFileAuditLogger) plugin.getLoggers().get(0);
        assertEquals("log path", "/tmp/xml-logs", logger.getLog());
        assertEquals("log size", 100, logger.getLimit());
        assertEquals("log count", 5, logger.getCount());
        assertTrue("log build cause", plugin.getLogBuildCause());

        String message = "hello";
        plugin.getLoggers().get(0).log(message);

        File logFile = new File("/tmp", "xml-logs.0");
        logFile.deleteOnExit();
        String log = Util.loadFile(logFile, StandardCharsets.UTF_8);
        assertTrue("logged actions: " + log, log.contains(message));

        // covering the console case will require refactoring as currently the console logger is directly using System.out/err
        // we need to properly inject those...
    }

    private AuditTrailPlugin load() {
        return (AuditTrailPlugin) Jenkins.XSTREAM2.fromXML(
              getClass().getResource("sample.xml"));
    }

    private void createJobAndPush() throws IOException, InterruptedException, ExecutionException {
        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.scheduleBuild2(0, new Cause.UserIdCause()).get();
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.getPage(new WebRequest(wc.createCrumbedUrl(job.getUrl() + "enable"), HttpMethod.POST));
    }
}
