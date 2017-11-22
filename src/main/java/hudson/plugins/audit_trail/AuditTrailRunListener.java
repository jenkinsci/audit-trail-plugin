package hudson.plugins.audit_trail;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class AuditTrailRunListener extends RunListener<Run> {

    public AuditTrailRunListener() {
        super(Run.class);
    }

    @Override
    public void onStarted(Run run, TaskListener listener) {
        AuditTrailPlugin plugin = (AuditTrailPlugin) Jenkins.getInstance().getPlugin("audit-trail");
        plugin.onStarted(run);
    }

    @Override
    public void onFinalized(Run run) {
        AuditTrailPlugin plugin = (AuditTrailPlugin) Jenkins.getInstance().getPlugin("audit-trail");
        plugin.onFinalized(run);
    }
}
