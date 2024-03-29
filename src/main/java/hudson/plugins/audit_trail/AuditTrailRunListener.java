package hudson.plugins.audit_trail;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Node;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @author Pierre Beitz
 */
@Extension
public class AuditTrailRunListener extends RunListener<Run> {
    private static final Logger LOGGER = Logger.getLogger(AuditTrailRunListener.class.getName());

    private static final String MASKED = "****";
    static final String UNKNOWN_NODE = "#unknown#";

    @Inject
    AuditTrailPlugin configuration;

    public AuditTrailRunListener() {
        super(Run.class);
    }

    @Override
    public void onStarted(Run run, TaskListener listener) {
        if (configuration.shouldLogBuildCause()) {
            StringBuilder builder = new StringBuilder(100);
            dumpCauses(run, builder);
            dumpParameters(run, builder);

            for (AuditLogger logger : configuration.getLoggers()) {
                logger.log(run.getParent().getUrl() + " #" + run.getNumber() + ' ' + builder.toString());
            }
        }
    }

    @Override
    public void onFinalized(Run run) {
        if (configuration.shouldLogBuildCause()) {
            StringBuilder builder = new StringBuilder(100);
            dumpCauses(run, builder);
            dumpParameters(run, builder);

            for (AuditLogger logger : configuration.getLoggers()) {
                String message = run.getFullDisplayName() + " "
                        + builder.toString() + " on node "
                        + buildNodeName(run) + " started at "
                        + run.getTimestampString2() + " completed in "
                        + run.getDuration() + "ms" + " completed: "
                        + run.getResult();
                logger.log(message);
            }
        }
    }

    private void dumpParameters(Run<?, ?> run, StringBuilder builder) {
        builder.append(", Parameters:[");
        ParametersAction parameters = run.getAction(ParametersAction.class);
        if (parameters != null) {
            builder.append(StreamSupport.stream(parameters.spliterator(), false)
                    .map(this::prettyPrintParameter)
                    .collect(Collectors.joining(", ")));
        }
        builder.append("]");
    }

    private String prettyPrintParameter(ParameterValue param) {
        return param.getName() + ": {" + (param.isSensitive() ? MASKED : param.getValue()) + "}";
    }

    private void dumpCauses(Run<?, ?> run, StringBuilder buf) {
        for (CauseAction action : run.getActions(CauseAction.class)) {
            for (Cause cause : action.getCauses()) {
                if (buf.length() > 0) buf.append(", ");
                buf.append(cause.getShortDescription());
            }
        }
        if (buf.length() == 0) buf.append("Started");
    }

    String buildNodeName(Run<?, ?> run) {
        if (run instanceof AbstractBuild) {
            var abstractBuild = (AbstractBuild<?, ?>) run;
            Node node = abstractBuild.getBuiltOn();
            if (node != null) {
                return node.getDisplayName();
            }
            return abstractBuild.getBuiltOnStr() != null ? abstractBuild.getBuiltOnStr() : "built-in";
        } else {
            LOGGER.log(
                    Level.INFO,
                    "Run is not an AbstractBuild but a {0}, will log the build node as {1}.",
                    new Object[] {run.getClass().getName(), UNKNOWN_NODE});
        }
        return UNKNOWN_NODE;
    }
}
