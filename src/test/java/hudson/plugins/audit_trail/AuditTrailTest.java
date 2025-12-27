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

import static hudson.plugins.audit_trail.LogFileAuditLogger.DEFAULT_LOG_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import hudson.Util;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Test interaction of audit-trail plugin with Jenkins core.
 *
 * @author Alan Harder
 * @author Pierre Beitz
 */
@WithJenkinsConfiguredWithCode
class AuditTrailTest {

    @Test
    void shouldGenerateTwoAuditLogs(JenkinsConfiguredWithCodeRule j, @TempDir Path tmpDir) throws Exception {
        // Given
        // Configure plugin
        File logFile = new File(tmpDir.toFile(), "test.log");
        JenkinsRule.WebClient wc = j.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        AuditTrailPlugin plugin = GlobalConfiguration.all().get(AuditTrailPlugin.class);
        LogFileAuditLogger logger = (LogFileAuditLogger) plugin.getLoggers().get(0);
        assertEquals(logFile.getPath(), logger.getLog(), "log path");
        assertEquals(1, logger.getLimit(), "log size");
        assertEquals(2, logger.getCount(), "log count");
        assertTrue(plugin.getLogBuildCause(), "log build cause");
        assertTrue(plugin.shouldLogCredentialsUsage(), "log credentials usage");
        // When
        createJobAndPush(j);

        // Then
        String log = Util.loadFile(new File(tmpDir.toFile(), "test.log.0"), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(".* job/test-job/ #1 Started by user" + " .*job/test-job/enable by .*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                () -> "logged actions: " + log);
    }

    @Issue("JENKINS-44129")
    @Test
    void shouldCorrectlyCleanUpFileHandlerOnApply(JenkinsConfiguredWithCodeRule j, @TempDir Path tmpDir)
            throws Exception {
        // Given
        JenkinsRule.WebClient wc = j.createWebClient();
        HtmlPage configure = wc.goTo("configure");
        HtmlForm form = configure.getFormByName("config");

        // When
        File logFile = new File(tmpDir.toFile(), "unique.log");
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(j, wc);

        // Then
        assertEquals(
                2,
                tmpDir.toFile().list().length,
                "Only two files should be present, the file opened by the FileHandler and its lock");
    }

    @Issue("JENKINS-60421")
    @Test
    @ConfiguredWithCode("jcasc-console-and-file.yml")
    void shouldGenerateAuditLogsWhenSetupWithJCasc(JenkinsConfiguredWithCodeRule j)
            throws IOException, ExecutionException, InterruptedException {
        // the injected jcasc assumes the temp directory is /tmp so let's skip windows
        assumeTrue(!System.getProperty("os.name").toLowerCase().contains("windows"));

        createJobAndPush(j);

        // https://github.com/jenkinsci/configuration-as-code-plugin/issues/899#issuecomment-524641582 log is 1, not 0
        // because of this.
        // this means that we technically always open 1 file for nothing. This could be improved by delaying the opening
        // of the file
        // after we are sure jcasc is done...
        // it's worth waiting for https://issues.jenkins-ci.org/browse/JENKINS-51856
        File logFile = new File("/tmp", "test.log.1");
        logFile.deleteOnExit();
        String log = Util.loadFile(logFile, StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(".* job/test-job/ #1 Started by user" + " .*job/test-job/enable by .*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                () -> "logged actions: " + log);

        // covering the console case will require refactoring as currently the console logger is directly using
        // System.out/err
        // we need to properly inject those...
    }

    @Issue("JENKINS-60421")
    @Test
    void loggerShouldBeProperlyConfiguredWhenLoadedFromXml() throws IOException {
        // the injected xml assumes the temp directory is /tmp so let's skip windows
        assumeTrue(!System.getProperty("os.name").toLowerCase().contains("windows"));

        AuditTrailPlugin plugin = load("sample.xml", getClass());
        LogFileAuditLogger logger = (LogFileAuditLogger) plugin.getLoggers().get(0);
        assertEquals("/tmp/xml-logs", logger.getLog(), "log path");
        assertEquals(100, logger.getLimit(), "log size");
        assertEquals(5, logger.getCount(), "log count");
        assertEquals(DEFAULT_LOG_SEPARATOR, logger.getLogSeparator(), "log separator");
        assertTrue(plugin.getLogBuildCause(), "log build cause");

        String message = "hello";
        plugin.getLoggers().get(0).log(message);

        File logFile = new File("/tmp", "xml-logs.0");
        logFile.deleteOnExit();
        String log = Util.loadFile(logFile, StandardCharsets.UTF_8);
        assertTrue(log.contains(message), () -> "logged actions: " + log);

        // covering the console case will require refactoring as currently the console logger is directly using
        // System.out/err
        // we need to properly inject those...
    }

    static AuditTrailPlugin load(String fileName, Class<?> clasz) {
        return (AuditTrailPlugin) Jenkins.XSTREAM2.fromXML(clasz.getResource(fileName));
    }

    private void createJobAndPush(JenkinsConfiguredWithCodeRule j)
            throws IOException, InterruptedException, ExecutionException {
        FreeStyleProject job = j.createFreeStyleProject("test-job");
        job.scheduleBuild2(0, new Cause.UserIdCause()).get();
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.getPage(new WebRequest(wc.createCrumbedUrl(job.getUrl() + "enable"), HttpMethod.POST));
    }
}
