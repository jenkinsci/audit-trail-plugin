package hudson.plugins.audit_trail;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class AuditLogger implements Describable<AuditLogger>, ExtensionPoint {

    public abstract void configure();

    public abstract void log(String event);

    public Descriptor getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

}
