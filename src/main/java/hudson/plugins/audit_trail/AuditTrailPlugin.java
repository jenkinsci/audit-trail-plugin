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

import hudson.Extension;
import hudson.Plugin;
import hudson.logging.LogRecorder;
import hudson.logging.LogRecorderManager;
import hudson.logging.WeakLogHandler;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.PluginServletFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Formatter;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Keep audit trail of particular Hudson operations, such as configuring jobs.
 * @author Alan.Harder@sun.com
 */
public class AuditTrailPlugin extends Plugin {
    private String log = "", pattern = ".*/(?:configSubmit|doDelete|postBuildResult|"
      + "cancelQueue|stop|toggleLogKeep|doWipeOutWorkspace|createItem|createView|toggleOffline)";
    private int limit = 1, count = 1;
    private boolean logBuildCause = true;
    private transient ServletContext context;

    public String getLog() { return log; }
    public int getLimit() { return limit; }
    public int getCount() { return count; }
    public String getPattern() { return pattern; }
    public boolean getLogBuildCause() { return logBuildCause; }

    @Override public void setServletContext(ServletContext context) {
        this.context = context;
    }

    @Override public void start() throws Exception {
        load();
        applySettings();

        // Add Filter to watch all requests and log matching ones
        PluginServletFilter.addFilter(new AuditTrailFilter());
    }

    @Override public void postInitialize() {
        // Add LogRecorder if not already configured.. but wait for Hudson to initialize:
        new Thread() {
            @Override public void run() {
                try { Thread.sleep(20000); } catch (InterruptedException ex) { }
                SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
                LogRecorderManager lrm = Hudson.getInstance().getLog();
                if (!lrm.logRecorders.containsKey("Audit Trail")) {
                    LogRecorder logRecorder = new LogRecorder("Audit Trail");
                    logRecorder.targets.add(
                        new LogRecorder.Target(AuditTrailFilter.class.getPackage().getName(), Level.CONFIG));
                    try { logRecorder.save(); } catch (Exception ex) { }
                    lrm.logRecorders.put("Audit Trail", logRecorder);
                }
                SecurityContextHolder.clearContext();
            }
        }.start();
    }

    @Override public void configure(StaplerRequest req, JSONObject formData)
            throws IOException, ServletException, FormException {
        log = formData.optString("log");
        limit = formData.optInt("limit", 1);
        count = formData.optInt("count", 1);
        pattern = formData.optString("pattern");
        logBuildCause = formData.optBoolean("logBuildCause", true);
        save();
        applySettings();
    }

    private void applySettings() {
        try {
            AuditTrailFilter.uriPattern = Pattern.compile(pattern);
        }
        catch (Exception ex) { ex.printStackTrace(); }

        LISTENER.setActive(logBuildCause);

        Logger logger = Logger.getLogger(AuditTrailFilter.class.getPackage().getName());
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
            handler.close();
        }
        if (log != null && log.length() > 0) try {
            FileHandler handler = new FileHandler(log, limit * 1024 * 1024, count, true);
            handler.setLevel(Level.CONFIG);
            handler.setFormatter(new Formatter() {
                SimpleDateFormat dateformat = new SimpleDateFormat("MMM d, yyyy h:mm:ss aa ");
                public synchronized String format(LogRecord record) {
                    return dateformat.format(new Date(record.getMillis()))
                           + record.getMessage() + '\n';
                }
            });
            logger.setLevel(Level.CONFIG);
            logger.addHandler(handler);
            // Workaround for SJSWS logging.. no need for audit trail to appear in logs/errors
            // since we have our own log file, BUT this handler ignores its level setting and
            // logs anything it receives.  So don't use parent handlers..
            logger.setUseParentHandlers(false);
            // ..but Hudson's LogRecorders run via a handler on the root logger so we'll
            // route messages directly to that handler..
            logger.addHandler(new RouteToHudsonHandler());
        }
        catch (IOException ex) { ex.printStackTrace(); }
    }

    private static class RouteToHudsonHandler extends Handler {
        public void publish(LogRecord record) {
            for (Handler handler : Logger.getLogger("").getHandlers()) {
                if (handler instanceof WeakLogHandler) {
                    handler.publish(record);
                }
            }
        }
        public void flush() { }
        public void close() { }
    }

    @Extension public static final AuditTrailRunListener LISTENER = new AuditTrailRunListener();

    public static class AuditTrailRunListener extends RunListener<Run> {
        private boolean active = false;
        private Logger LOG = Logger.getLogger(AuditTrailRunListener.class.getName());

        private AuditTrailRunListener() {
            super(Run.class);
        }

        private void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public void onStarted(Run run, TaskListener listener) {
            if (this.active) {
                StringBuilder buf = new StringBuilder(100);
                for (CauseAction action : run.getActions(CauseAction.class)) {
                    for (Cause cause : action.getCauses()) {
                        if (buf.length() > 0) buf.append(", ");
                        buf.append(cause.getShortDescription());
                    }
                }
                if (buf.length() == 0) buf.append("Started");
                LOG.config(run.getParent().getUrl() + " #" + run.getNumber() + ' ' + buf.toString());
            }
        }
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
                + "http://java.sun.com/j2se/1.5.0/docs/api/java/util/regex/Pattern.html"
                + "\">regular expression</a> (" + ex.getMessage() + ")");
        }
    }
}
