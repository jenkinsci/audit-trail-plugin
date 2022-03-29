package hudson.plugins.audit_trail;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsUseListener;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.model.ModelObject;

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
public class CredentialUsageListener extends CredentialsUseListener {
    @Inject
    AuditTrailPlugin configuration;

    /**
     * Triggered when the {@link com.cloudbees.plugins.credentials.CredentialsProvider} accesses
     * {@link com.cloudbees.plugins.credentials.Credentials}
     *
     * @param c     The used Credentials.
     * @param obj   The object using the credentials.
     */
    @Override
    public void onUse(Credentials c, ModelObject obj) {
        if (!configuration.shouldLogCredentialsUsage()) {
            return;
        }

        StringBuilder builder = new StringBuilder(100);

        String objName = obj.toString();
        String objType = obj.getClass().toString();

        builder.append("'" + objName
                + "' (" + objType + ") ");

        String credsType = c.getClass().toString();

        if (c instanceof BaseStandardCredentials) {
            String credsId = ((BaseStandardCredentials) c).getId();
            builder.append("used credentials '" + credsId + "' (" + credsType + ").");
        } else {
            String nonDefaultWarning = builder + ("used an unsupported credentials type (" + credsType +
                    ") that may potentially not be audit-logged correctly.");
            Logger.getLogger(CredentialUsageListener.class.getName()).log(Level.INFO, null, nonDefaultWarning);

            builder.append("used credentials '" + c + "' (" + credsType + ") (Note: Used fallback method for log as " +
                    "credentials type is not supported. See INFO log for more information.");
        }

        for (AuditLogger logger : configuration.getLoggers()) {
            logger.log(builder.toString());
        }
    }

}
