package hudson.plugins.audit_trail;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @author Pierre Beitz
 */
public abstract class AuditLogger implements Describable<AuditLogger>, ExtensionPoint {

    public abstract void log(String event);

    public Descriptor<AuditLogger> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Clean up any resource used by this logger.
     * For instance if your logger use a InputStream, this is were you should close it.
     *
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have <code>LoggingPermission("control")</code>.
     */
    public void cleanUp() throws SecurityException {
        // default does nothing
    }

    /**
     * Returns all the registered {@link AuditLogger} descriptors.
     */
    public static DescriptorExtensionList<AuditLogger, Descriptor<AuditLogger>> all() {
        return Jenkins.getInstance().getDescriptorList(AuditLogger.class);
    }
}
