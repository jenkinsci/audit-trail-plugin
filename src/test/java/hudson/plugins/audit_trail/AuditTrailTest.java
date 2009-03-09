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

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.Util;
import hudson.model.Cause.UserCause;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import java.io.File;
import java.util.regex.Pattern;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test interaction of audit-trail plugin with Hudson core.
 * @author Alan.Harder@sun.com
 */
public class AuditTrailTest extends HudsonTestCase {

    public void testPlugin() throws Exception {
        // Configure plugin
        File tmpDir = createTmpDir(), logFile = new File(tmpDir, "test.log");
        WebClient wc = new WebClient();
        HtmlForm form = wc.goTo("configure").getFormByName("config");
        form.getInputByName("log").setValueAttribute(logFile.getPath());
        form.getInputByName("limit").setValueAttribute("1");
        form.getInputByName("count").setValueAttribute("2");
        submit(form);

        AuditTrailPlugin plugin = Hudson.getInstance().getPlugin(AuditTrailPlugin.class);
        assertEquals("log path", logFile.getPath(), plugin.getLog());
        assertEquals("log size", 1, plugin.getLimit());
        assertEquals("log count", 2, plugin.getCount());
        assertTrue("log build cause", plugin.getLogBuildCause());

        // Perform a couple actions to be logged
        FreeStyleProject job = createFreeStyleProject("test-job");
        job.scheduleBuild2(0, new UserCause()).get();
        wc.goTo(job.getUrl() + "doWipeOutWorkspace");

        String log = Util.loadFile(new File(tmpDir, "test.log.0"));
        assertTrue("logged actions: " + log, Pattern.compile(".* job/test-job/ #1 Started by user"
            + " .*job/test-job/doWipeOutWorkspace by .*", Pattern.DOTALL).matcher(log).matches());
    }
}
