package hudson.plugins.audit_trail;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsUseListener;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.model.AbstractModelObject;
import hudson.model.Item;
import hudson.model.Run;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;


@Extension
public class CredentialUsageListener extends CredentialsUseListener {
    @Inject
    AuditTrailPlugin configuration;

    @Override
    public void onUse(Credentials c, AbstractModelObject obj) {
        if (!configuration.shouldLogCredentialsUsage())
            return;

        StringBuilder builder = new StringBuilder(100);
        String objName = obj.toString();
        String objType = obj.getClass().toString();
        builder.append("'" + objName
                + "' (" + objType + ") ");
        auditlog(c, builder);
    }

    @Override
    public void onUse(Credentials c, Item item) {
        if (!configuration.shouldLogCredentialsUsage())
            return;

        StringBuilder builder = new StringBuilder(100);
        String itemName = item.getFullDisplayName();
        String itemType = item.getClass().toString();

        builder.append("'" + itemName
                + "' (" + itemType + ") ");

        auditlog(c, builder);
    }

    private void auditlog(Credentials c, StringBuilder builder) {
        String credsType = c.getClass().toString();
        if (!(c instanceof BaseStandardCredentials)) {
            builder.append("used an unsupported credentials type (" + credsType + ") that can't be audit-logged");
            Logger.getLogger(CredentialUsageListener.class.getName()).log(Level.WARNING, null,builder.toString());
            return;
        }

        String credsId = ((BaseStandardCredentials) c).getId();

        builder.append("used credentials '" + credsId + "' (" + credsType + ").");

        for (AuditLogger logger : configuration.getLoggers()) {
            logger.log(builder.toString());
        }
    }
}
