package hudson.plugins.audit_trail;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import groovy.lang.Binding;
import hudson.Extension;
import hudson.cli.GroovyCommand;
import hudson.cli.GroovyshCommand;
import hudson.model.User;
import hudson.util.RemotingDiagnostics;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import jenkins.model.Jenkins;
import jenkins.util.ScriptListener;
import org.kohsuke.stapler.StaplerRequest;

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
    private static final Map<Class<?>, String> FEATURE_MAPPING;

    static {
        FEATURE_MAPPING = Map.of(
                RemotingDiagnostics.class, "Script Console Controller",
                GroovyshCommand.class, "CLI/GroovySh",
                GroovyCommand.class, "CLI/GroovyCommand");
    }

    @Inject
    AuditTrailPlugin configuration;

    /**
     * @inheritDoc
     */
    @Override
    public void onScriptExecution(
            String script,
            @CheckForNull Binding $,
            @NonNull Object feature,
            @CheckForNull Object $$,
            @NonNull String $$$,
            @CheckForNull User user) {
        if (!configuration.getLogScriptUsage()) {
            return;
        }
        StringBuilder builder = new StringBuilder();

        if (user != null) {
            builder.append(String.format(
                    "A groovy script was executed by user '%s'. Origin: %s. ",
                    user.getId(), prettyPrintFeature(feature)));
        } else {
            builder.append(String.format("A groovy script was executed. Origin: %s.", prettyPrintFeature(feature)));
        }

        builder.append("\nThe executed script: \n");
        builder.append(script);
        String log = builder.toString();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Detected groovy script usage, details: {0}", new Object[] {log});
        }
        for (AuditLogger logger : configuration.getLoggers()) {
            logger.log(log);
        }
    }

    private String prettyPrintFeature(Object feature) {
        if (feature instanceof Class<?>) {
            return FEATURE_MAPPING.getOrDefault(feature, feature.toString());
        }
        // currently it seems the feature is always a class
        return feature.toString();
    }
}
