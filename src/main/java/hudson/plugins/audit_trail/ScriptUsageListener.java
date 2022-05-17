package hudson.plugins.audit_trail;

import hudson.Extension;
import jenkins.model.Jenkins;
import jenkins.model.ScriptListener;
import org.kohsuke.stapler.StaplerRequest;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Log when a (privileged) Groovy script is executed.
 *
 * @see Jenkins#_doScript(StaplerRequest, org.kohsuke.stapler.StaplerResponse, javax.servlet.RequestDispatcher, hudson.remoting.VirtualChannel, hudson.security.ACL)
 * @see hudson.cli.GroovyCommand#run()
 * @see hudson.cli.GroovyshCommand#run()
 * @see org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval#using(String, org.jenkinsci.plugins.scriptsecurity.scripts.Language, String)
 *
 * @author Jan Meiswinkel
 */
@Extension
public class ScriptUsageListener implements ScriptListener {
    private static final Logger LOGGER = Logger.getLogger(ScriptUsageListener.class.getName());

    @Inject
    AuditTrailPlugin configuration;

    /**
     * Called when a (privileged) groovy script is executed.
     *
     * @see Jenkins#_doScript(StaplerRequest, org.kohsuke.stapler.StaplerResponse, javax.servlet.RequestDispatcher, hudson.remoting.VirtualChannel, hudson.security.ACL)
     * @param script The script to be executed.
     * @param origin Descriptive identifier of the origin that is responsible for executing the script (Console, Run, ...).
     */
    @Override
    public void onScript(String script, String origin) {
        if (!configuration.getLogScriptUsage()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("A groovy script was executed. Origin: ");
        builder.append(origin);
        builder.append("\nThe executed script: \n");
        builder.append(script);
        String log = builder.toString();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Detected groovy script usage, details: {0}", new Object[]{log});
        }
        for (AuditLogger logger : configuration.getLoggers()) {
            logger.log(log);
        }
    }
}
