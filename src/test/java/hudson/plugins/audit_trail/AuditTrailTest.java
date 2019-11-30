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
import jenkins.model.GlobalConfiguration;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test interaction of audit-trail plugin with Jenkins core.
 * @author Alan Harder
 */
public class AuditTrailTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private static final int TIMEOUT = 2000;
    private static final String LOG_LOCATION_INPUT_NAME = "_.log";
    private static final String LOG_FILE_SIZE_INPUT_NAME = "_.limit";
    private static final String LOG_FILE_COUNT_INPUT_NAME = "_.count";
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
        HtmlPage configure = wc.goTo("configure");
        HtmlForm form = configure.getFormByName("config");
        j.getButtonByCaption(form, ADD_LOGGER_BUTTON_TEXT).click();
        configure.getAnchorByText(LOG_FILE_COMBO_TEXT).click();
        wc.waitForBackgroundJavaScript(TIMEOUT);
        form.getInputByName(LOG_LOCATION_INPUT_NAME).setValueAttribute(logFile.getPath());
        form.getInputByName(LOG_FILE_SIZE_INPUT_NAME).setValueAttribute("1");
        form.getInputByName(LOG_FILE_COUNT_INPUT_NAME).setValueAttribute("2");
        j.submit(form);

        AuditTrailPlugin plugin = GlobalConfiguration.all().get(AuditTrailPlugin.class);
        LogFileAuditLogger logger = (LogFileAuditLogger) plugin.getLoggers().get(0);
        assertEquals("log path", logFile.getPath(), logger.getLog());
        assertEquals("log size", 1, logger.getLimit());
        assertEquals("log count", 2, logger.getCount());
        assertTrue("log build cause", plugin.getLogBuildCause());

        // When
        // Perform a couple actions to be logged
        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.scheduleBuild2(0, new Cause.UserIdCause()).get();
        wc.getPage(new WebRequest(wc.createCrumbedUrl(job.getUrl() + "enable"), HttpMethod.POST));

        // Then
        String log = Util.loadFile(new File(tmpDir.getRoot(), "test.log.0"), Charset.forName("UTF-8"));
        assertTrue("logged actions: " + log, Pattern.compile(".* job/test-job/ #1 Started by user"
                + " .*job/test-job/enable by .*", Pattern.DOTALL).matcher(log).matches());
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
}
