/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Alan Harder
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

import hudson.DescriptorExtensionList;
import hudson.Plugin;
import hudson.model.*;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import hudson.util.PluginServletFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Keep audit trail of particular Jenkins operations, such as configuring jobs.
 * @author Alan Harder
 */
public class AuditTrailPlugin extends Plugin {
    private String pattern = ".*/(?:configSubmit|doDelete|postBuildResult|"
      + "cancelQueue|stop|toggleLogKeep|doWipeOutWorkspace|createItem|createView|toggleOffline)";
    private boolean logBuildCause = true;

    private List<AuditLogger> loggers = new ArrayList<AuditLogger>();

    private transient boolean started;

    private transient String log;
    private transient int limit = 1, count = 1;

    public String getPattern() { return pattern; }
    public boolean getLogBuildCause() { return logBuildCause; }
    public List<AuditLogger> getLoggers() { return loggers; }

    @Override public void start() throws Exception {
        // Set a default value; will be overridden by load() once customized:
        load();
        applySettings();

        // Add Filter to watch all requests and log matching ones
        PluginServletFilter.addFilter(new AuditTrailFilter(this));
    }

    @Override public void configure(StaplerRequest req, JSONObject formData)
            throws IOException, ServletException, FormException {
        pattern = formData.optString("pattern");
        logBuildCause = formData.optBoolean("logBuildCause", true);
        loggers = Descriptor.newInstancesFromHeteroList(
                req, formData, "loggers", getLoggerDescriptors());
        save();
        applySettings();
    }

    public DescriptorExtensionList<AuditLogger, Descriptor<AuditLogger>> getLoggerDescriptors() {
        return Jenkins.getInstance().getDescriptorList(AuditLogger.class);
    }


    private void applySettings() {
        try {
            AuditTrailFilter.setPattern(pattern);
        }
        catch (PatternSyntaxException ex) { ex.printStackTrace(); }

        for (AuditLogger logger : loggers) {
            logger.configure();
        }
        started = true;
    }

    /* package */ void onStarted(Run run) {
        if (this.started) {
            StringBuilder buf = new StringBuilder(100);
            for (CauseAction action : run.getActions(CauseAction.class)) {
                for (Cause cause : action.getCauses()) {
                    if (buf.length() > 0) buf.append(", ");
                    buf.append(cause.getShortDescription());
                }
            }
            if (buf.length() == 0) buf.append("Started");

            for (AuditLogger logger : loggers) {
                logger.log(run.getParent().getUrl() + " #" + run.getNumber() + ' ' + buf.toString());
            }

        }
    }

    public void onFinalized(Run run) {
        if (run instanceof AbstractBuild) {
            onFinalized((AbstractBuild) run);
        }

    }
    public void onFinalized(AbstractBuild build) {
        if (this.started) {
            StringBuilder causeBuilder = new StringBuilder(100);
            for (CauseAction action : build.getActions(CauseAction.class)) {
                for (Cause cause : action.getCauses()) {
                    if (causeBuilder.length() > 0) causeBuilder.append(", ");
                    causeBuilder.append(cause.getShortDescription());
                }
            }
            if (causeBuilder.length() == 0) causeBuilder.append("Started");

            for (AuditLogger logger : loggers) {
                String message = build.getFullDisplayName() +
                        " " + causeBuilder.toString() +
                        " on node " + (build.getBuiltOn() == null ? "#unknown#" : build.getBuiltOn().getDisplayName()) +
                        " started at " + build.getTimestampString2() +
                        " completed in " + build.getDuration() + "ms" +
                        " completed: " + build.getResult();
                logger.log(message);
            }

        }
    }

    /* package */ void onRequest(String uri, String extra, String username) {
        if (this.started) {
            for (AuditLogger logger : loggers) {
                logger.log(uri + extra + " by " + username);
            }
        }
    }


    /**
     * Backward compatibility
     */
    private Object readResolve() {
        if (log != null) {
            if (loggers == null) {
                loggers = new ArrayList<AuditLogger>();
            }
            LogFileAuditLogger logger = new LogFileAuditLogger(log, limit, count);
            if (!loggers.contains(logger))
                loggers.add(logger);
            log = null;
        }
        return this;
    }

    /**
     * Validate regular expression syntax.
     */
    public FormValidation doRegexCheck(@QueryParameter final String value)
            throws IOException, ServletException {
        // No permission needed for simple syntax check
        try {
            Pattern.compile(value);
            return FormValidation.ok();
        }
        catch (Exception ex) {
            return FormValidation.errorWithMarkup("Invalid <a href=\""
                + "http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html"
                + "\">regular expression</a> (" + ex.getMessage() + ")");
        }
    }

}
