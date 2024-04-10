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

import static hudson.plugins.audit_trail.AuditTrailTest.load;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import hudson.Util;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jenkins.model.GlobalConfiguration;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author <a href="mailto:tomasz.sek.88@gmail.com">Tomasz Sęk</a>
 * @author Pierre Beitz
 */
@WithJenkins
class ConsoleAuditLoggerTest {

    @Issue("JENKINS-51331")
    @Test
    void shouldConfigureConsoleAuditLogger(JenkinsRule j) throws Exception {
        // Given
        JenkinsRule.WebClient wc = j.createWebClient();
        HtmlPage configure = wc.goTo("configure");
        HtmlForm form = configure.getFormByName("config");
        j.getButtonByCaption(form, "Add Logger").click();
        configure.getAnchorByText("Console").click();
        wc.waitForBackgroundJavaScript(2000);

        // When
        j.submit(form);

        // Then
        // submit configuration page without any errors
        AuditTrailPlugin plugin = GlobalConfiguration.all().get(AuditTrailPlugin.class);
        assertEquals(1, plugin.getLoggers().size(), "amount of loggers");
        AuditLogger logger = plugin.getLoggers().get(0);
        assertInstanceOf(ConsoleAuditLogger.class, logger, "ConsoleAuditLogger should be configured");
        assertEquals(ConsoleAuditLogger.Output.STD_OUT, ((ConsoleAuditLogger) logger).getOutput(), "output");
    }

    @Issue("JENKINS-12848")
    @Test
    void loggerShouldBeProperlyConfiguredWhenLoadedFromXml() throws IOException {
        // the injected xml assumes the temp directory is /tmp so let's skip windows
        assumeTrue(!System.getProperty("os.name").toLowerCase().contains("windows"));

        AuditTrailPlugin plugin = load("jenkins-12848.xml", getClass());
        LogFileAuditLogger logger = (LogFileAuditLogger) plugin.getLoggers().get(0);
        String logSeparator = ";";
        assertEquals(logSeparator, logger.getLogSeparator(), "log separator");

        String message = "hello";
        plugin.getLoggers().get(0).log(message);

        File logFile = new File("/tmp", "xml-logs-12848.0");
        logFile.deleteOnExit();
        String log = Util.loadFile(logFile, StandardCharsets.UTF_8);
        assertTrue(log.contains(logSeparator + message), () -> "logged actions: " + log);
    }
}
