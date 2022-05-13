package hudson.plugins.audit_trail;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsUseListener;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.Run;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Log when credentials are used. Only works if the job decides to access the credentials via the
 * {@link com.cloudbees.plugins.credentials.CredentialsProvider}. Credential-types that do not extend
 * {@link com.cloudbees.plugins.credentials.Credentials}
 *
 * @author Jan Meiswinkel
 */
@Extension
public class CredentialUsageListener implements CredentialsUseListener {
    private static final Logger LOGGER = Logger.getLogger(CredentialUsageListener.class.getName());

    @Inject
    AuditTrailPlugin configuration;

    /**
     * Triggered when the {@link com.cloudbees.plugins.credentials.CredentialsProvider} accesses
     * {@link com.cloudbees.plugins.credentials.Credentials}.
     *
     * @param c   The used Credentials.
     * @param run The object using the credentials.
     * @see CredentialsProvider#trackAll(Run, java.util.List)
     */
    @Override
    public void onUse(Credentials c, Run run) {
        if (!configuration.shouldLogCredentialsUsage())
            return;

        StringBuilder builder = new StringBuilder(100);

        String runName = run.getExternalizableId();
        String runType = run.getClass().toString();
        builder.append(String.format("'%s' (%s) ", runName, runType));
        auditLog(c, builder);
    }

    /**
     * Triggered when the {@link com.cloudbees.plugins.credentials.CredentialsProvider} accesses
     * {@link com.cloudbees.plugins.credentials.Credentials}.
     *
     * @param c    The used Credentials.
     * @param node The object using the credentials.
     * @see CredentialsProvider#trackAll(Node, java.util.List)
     */
    @Override
    public void onUse(Credentials c, Node node) {
        if (!configuration.shouldLogCredentialsUsage())
            return;

        StringBuilder builder = new StringBuilder(100);

        String nodeName = node.getNodeName();
        String nodeType = node.getClass().toString();
        builder.append(String.format("'%s' (%s) ", nodeName, nodeType));
        auditLog(c, builder);
    }

    /**
     * Triggered when the {@link com.cloudbees.plugins.credentials.CredentialsProvider} accesses
     * {@link com.cloudbees.plugins.credentials.Credentials}.
     *
     * @param c    The used Credentials.
     * @param item The object using the credentials.
     * @see CredentialsProvider#trackAll(Item, java.util.List)
     */
    @Override
    public void onUse(Credentials c, Item item) {
        if (!configuration.shouldLogCredentialsUsage())
            return;

        StringBuilder builder = new StringBuilder(100);

        String runName = item.getFullName();
        String itemType = item.getClass().toString();
        builder.append(String.format("'%s' (%s) ", runName, itemType));
        auditLog(c, builder);
    }

    private void auditLog(Credentials c, StringBuilder builder) {
        String credsType = c.getClass().toString();
        if (c instanceof BaseStandardCredentials) {
            String credsId = ((BaseStandardCredentials) c).getId();
            builder.append(String.format("used credentials '%s' (%s).", credsId, credsType));
        } else if (c instanceof IdCredentials) {
            String credsId = ((IdCredentials) c).getId();
            builder.append(String.format("used credentials '%s' (%s).", credsId, credsType));
        } else {
            String noIdAvailableWarning = builder + ("used an unsupported credentials type (" + credsType +
                    ") whose ID cannot be audit-logged. Consider opening an issue.");
            Logger.getLogger(CredentialUsageListener.class.getName()).log(Level.WARNING, null, noIdAvailableWarning);

            builder.append("used credentials of type " + credsType + " (Note: Used fallback method for log as " +
                    "credentials type is not supported. See INFO log for more information).");
        }

        String log = builder.toString();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Detected credential usage, details: {0}", new Object[]{log});
        }

        for (AuditLogger logger : configuration.getLoggers()) {
            logger.log(log);
        }
    }
}
