package hudson.plugins.audit_trail;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.inject.Inject;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @author Pierre Beitz
 */
@Extension
public class AuditTrailRunListener extends RunListener<Run> {

    @Inject
    AuditTrailPlugin configuration;

    public AuditTrailRunListener() {
        super(Run.class);
    }

    @Override
    public void onStarted(Run run, TaskListener listener) {
        StringBuilder buf = new StringBuilder(100);
        for (CauseAction action : run.getActions(CauseAction.class)) {
            for (Cause cause : action.getCauses()) {
                if (buf.length() > 0) buf.append(", ");
                buf.append(cause.getShortDescription());
            }
        }
        if (buf.length() == 0) buf.append("Started");

        for (AuditLogger logger : configuration.getLoggers()) {
            logger.log(run.getParent().getUrl() + " #" + run.getNumber() + ' ' + buf.toString());
        }
    }

    @Override
    public void onFinalized(Run run) {
        StringBuilder causeBuilder = new StringBuilder(100);
        for (CauseAction action : run.getActions(CauseAction.class)) {
            for (Cause cause : action.getCauses()) {
                if (causeBuilder.length() > 0) causeBuilder.append(", ");
                causeBuilder.append(cause.getShortDescription());
            }
        }
        if (causeBuilder.length() == 0) causeBuilder.append("Started");

        for (AuditLogger logger : configuration.getLoggers()) {
            String message = run.getFullDisplayName() +
                  " " + causeBuilder.toString() +
                  " on node " + buildNodeName(run) +
                  " started at " + run.getTimestampString2() +
                  " completed in " + run.getDuration() + "ms" +
                  " completed: " + run.getResult();
            logger.log(message);
        }
    }

    private String buildNodeName(Run run) {
        if (run instanceof AbstractBuild) {
            Node node = ((AbstractBuild) run).getBuiltOn();
            if (node != null) {
                return node.getDisplayName();
            }
        }
        return "#unknown#";
    }
}
