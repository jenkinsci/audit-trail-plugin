package hudson.plugins.audit_trail;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import hudson.Extension;
import hudson.model.User;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import jenkins.security.SecurityListener;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Emit authentication events (successful logins, failed login attempts and logouts) to each configured AuditLogger.
 *
 * This hooks directly into Jenkins core's {@link SecurityListener} extension point to catch
 * all auth paths (and get the username) in a way that URL pattern matching cannot achieve.
 */
@Extension
public class AuthenticationAuditListener extends SecurityListener {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationAuditListener.class.getName());

    /**
     * Depending on the configured {@link hudson.security.SecurityRealm}, both
     * {@link #failedToAuthenticate(String)} and {@link #failedToLogIn(String)} can fire for the
     * same failed attempt, in quick succession. This window collapses that duplicate callback
     * into a single log entry. However, a sustained brute-force attack will not be ignored
     * as there would still be a log entry at least once per window.
     */
    private static final long FAILED_LOGIN_DEDUP_WINDOW_MS = 250;

    /**
     * Caps the number of distinct usernames tracked for deduplication to avoid memory DoS
     */
    private static final long MAX_TRACKED_FAILED_LOGIN_USERNAMES = 10_000;

    private final Cache<String, Boolean> recentlyLoggedFailedLogins = CacheBuilder.newBuilder()
            .expireAfterWrite(FAILED_LOGIN_DEDUP_WINDOW_MS, TimeUnit.MILLISECONDS)
            .maximumSize(MAX_TRACKED_FAILED_LOGIN_USERNAMES)
            .build();

    @Inject
    AuditTrailPlugin configuration;

    @Override
    protected void authenticated2(UserDetails details) {
        if (!configuration.shouldLogAuthEvents()) return;

        String username = details.getUsername();
        log(String.format("Successful login for user '%s'", resolveDisplayName(username)));
    }

    @Override
    protected void failedToAuthenticate(String username) {
        recordFailedLogin(username);
    }

    @Override
    protected void failedToLogIn(String username) {
        recordFailedLogin(username);
    }

    @Override
    protected void loggedOut(String username) {
        if (!configuration.shouldLogAuthEvents()) return;

        log(String.format("Logout for user '%s'", resolveDisplayName(username)));
    }

    private void recordFailedLogin(String username) {
        if (!configuration.shouldLogAuthEvents()) return;

        String key = username != null ? username : "<unknown>";
        if (recentlyLoggedFailedLogins.getIfPresent(key) != null) {
            // Skip duplicate callback for the same failed attempt - see comment beside FAILED_LOGIN_DEDUP_WINDOW_MS
            return;
        }
        recentlyLoggedFailedLogins.put(key, Boolean.TRUE);

        log(String.format("Failed login attempt for user '%s'", key));
    }

    private String resolveDisplayName(String username) {
        // shouldDisplayUserName is confusingly named, but if true it means use the *display* name
        // rather than the user ID. The `username` passed in to these callbacks is the user ID.
        if (!configuration.shouldDisplayUserName()) {
            // Use user ID
            return username;
        }
        // Resolve display name if possible
        User user = User.getById(username, false);
        return user != null ? user.getDisplayName() : username;
    }

    private void log(String message) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Detected authentication event, details: {0}", new Object[] {message});
        }
        for (AuditLogger logger : configuration.getLoggers()) {
            logger.log(message);
        }
    }
}
