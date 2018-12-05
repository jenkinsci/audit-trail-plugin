package hudson.plugins.audit_trail;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.io.IOException;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class AuditLogger implements Describable<AuditLogger>, ExtensionPoint {

    public abstract void configure();

    public abstract void log(String event);

    public Descriptor getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Clean up any resource used by this logger.
     * For instance if your logger use a InputStream, this is were you should close it.
     *
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have <tt>LoggingPermission("control")</tt>.
     */
    public void cleanUp() throws SecurityException {
        // default does nothing
    }

}
