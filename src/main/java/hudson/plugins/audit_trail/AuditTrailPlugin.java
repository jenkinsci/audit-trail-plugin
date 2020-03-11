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
import hudson.Extension;

import hudson.XmlFile;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Keep audit trail of particular Jenkins operations, such as configuring jobs.
 *
 * @author Alan Harder
 * @author Pierre Beitz
 */
@Symbol("audit-trail")
@Extension
public class AuditTrailPlugin extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger(AuditTrailPlugin.class.getName());
    private String pattern = ".*/(?:configSubmit|doDelete|postBuildResult|enable|disable|"
            + "cancelQueue|stop|toggleLogKeep|doWipeOutWorkspace|createItem|createView|toggleOffline|"
            + "cancelQuietDown|quietDown|restart|exit|safeExit)";
    private boolean logBuildCause = true;

    private List<AuditLogger> loggers = new ArrayList<>();

    private transient String log;

    public String getPattern() { return pattern; }
    public boolean getLogBuildCause() { return logBuildCause; }
    public List<AuditLogger> getLoggers() { return loggers; }

    public AuditTrailPlugin() {
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) {
        // readResolve makes sure loggers is initialized, so it should never be null.
        // TODO this should probably be moved somewhere else
        loggers.forEach(AuditLogger::cleanUp);
        req.bindJSON(this, formData);
        return true;
    }

    @DataBoundSetter
    public void setPattern(String pattern) {
        this.pattern = Optional.ofNullable(pattern).orElse("");
        updateFilterPattern();
        save();
    }

    @DataBoundSetter
    public void setLogBuildCause(boolean logBuildCause) {
        this.logBuildCause = logBuildCause;
        save();
    }

    private void updateFilterPattern() {
        try {
            AuditTrailFilter.setPattern(pattern);
        } catch (PatternSyntaxException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @deprecated as of 2.6
     **/
    @Deprecated
    public DescriptorExtensionList<AuditLogger, Descriptor<AuditLogger>> getLoggerDescriptors() {
        return AuditLogger.all();
    }

    @DataBoundSetter
    public void setLoggers(List<AuditLogger> loggers) {
        this.loggers = Optional.ofNullable(loggers).orElse(Collections.emptyList());
    }

    /**
     * @deprecated as of 2.6
     **/
    @Restricted(DoNotUse.class)
    @Deprecated
    public void onFinalized(Run run) {
        LOGGER.warning("AuditTrailPlugin#onFinalized does nothing anymore, please update your script");
    }

    /**
     * @deprecated as of 2.6
     **/
    @Restricted(DoNotUse.class)
    @Deprecated
    public void onFinalized(AbstractBuild build) {
        LOGGER.warning("AuditTrailPlugin#onFinalized does nothing anymore, please update your script");
    }

    /**
     * Backward compatibility
     */
    private Object readResolve() {
        if (log != null) {
            if (loggers == null) {
                loggers = new ArrayList<>();
            }
            LogFileAuditLogger logger = new LogFileAuditLogger(log, 1, 1, null);
            if (!loggers.contains(logger))
                loggers.add(logger);
            log = null;
        }
        updateFilterPattern();
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
        } catch (Exception ex) {
            return FormValidation.errorWithMarkup("Invalid <a href=\""
                    + "http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html"
                    + "\">regular expression</a> (" + ex.getMessage() + ")");
        }
    }

    @Override
    protected XmlFile getConfigFile() {
        return new XmlFile(new File(Jenkins.get().getRootDir(),"audit-trail.xml"));
    }
}
