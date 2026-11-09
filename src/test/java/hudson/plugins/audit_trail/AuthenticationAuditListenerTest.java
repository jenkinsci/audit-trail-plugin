package hudson.plugins.audit_trail;

import static org.junit.jupiter.api.Assertions.*;

import hudson.Util;
import hudson.security.HudsonPrivateSecurityRealm;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirDeletionStrategy.IgnoreFailures;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

@WithJenkins
class AuthenticationAuditListenerTest {

    private JenkinsRule r;

    @TempDir(deletionStrategy = IgnoreFailures.class)
    private File tmpDir;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        r = rule;
    }

    private HudsonPrivateSecurityRealm setUpRealmWithUser(String username, String password) throws Exception {
        HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false);
        r.jenkins.setSecurityRealm(realm);
        realm.createAccount(username, password);
        return realm;
    }

    /**
     * Authenticates through the realm's {@link AuthenticationManager}, i.e. the same code path used by
     * real interactive/API authentication so that {@code jenkins.security.SecurityListener} callbacks fire
     */
    private void authenticateThroughManager(HudsonPrivateSecurityRealm realm, String username, String password) {
        AuthenticationManager manager = realm.getSecurityComponents().manager2;
        manager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }

    @Test
    void successfulLoginIsLogged() throws Exception {
        String logFileName = "successfulLoginIsLogged.log";
        File logFile = new File(tmpDir, logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        setUpRealmWithUser("alice", "s3cr3t");
        wc.login("alice", "s3cr3t");

        String log = Util.loadFile(new File(tmpDir, logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(".*Successful login for user 'alice'.*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                "Successful login was not logged. Logged actions: " + log);
    }

    @Test
    void failedLoginIsLogged() throws Exception {
        String logFileName = "failedLoginIsLogged.log";
        File logFile = new File(tmpDir, logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        HudsonPrivateSecurityRealm realm = setUpRealmWithUser("bob", "correct-password");
        assertThrows(AuthenticationException.class, () -> authenticateThroughManager(realm, "bob", "wrong-password"));

        String log = Util.loadFile(new File(tmpDir, logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(".*Failed login attempt for user 'bob'.*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                "Failed login was not logged. Logged actions: " + log);
    }

    @Test
    void repeatedFailedLoginWithinDedupWindowIsLoggedOnce() throws Exception {
        String logFileName = "repeatedFailedLoginIsDeduped.log";
        File logFile = new File(tmpDir, logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        HudsonPrivateSecurityRealm realm = setUpRealmWithUser("eve", "correct-password");
        for (int i = 0; i < 2; i++) {
            assertThrows(
                    AuthenticationException.class, () -> authenticateThroughManager(realm, "eve", "wrong-password"));
        }

        String log = Util.loadFile(new File(tmpDir, logFileName + ".0"), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("Failed login attempt for user 'eve'").matcher(log);
        int occurrences = 0;
        while (matcher.find()) {
            occurrences++;
        }
        assertEquals(1, occurrences, "Repeated failed login was not deduped. Logged actions: " + log);
    }

    @Test
    void logoutIsLogged() throws Exception {
        String logFileName = "logoutIsLogged.log";
        File logFile = new File(tmpDir, logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        setUpRealmWithUser("carol", "s3cr3t");
        wc.login("carol", "s3cr3t");
        wc.goTo("logout");

        String log = Util.loadFile(new File(tmpDir, logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                Pattern.compile(".*Logout for user 'carol'.*", Pattern.DOTALL)
                        .matcher(log)
                        .matches(),
                "Logout was not logged. Logged actions: " + log);
    }

    @Test
    void disabledLoggingOptionIsRespected() throws Exception {
        String logFileName = "disabledAuthEventsLoggingIsRespected.log";
        File logFile = new File(tmpDir, logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile)
                .withLogAuthEvents(false)
                .sendConfiguration(r, wc);

        HudsonPrivateSecurityRealm realm = setUpRealmWithUser("dave", "s3cr3t");
        authenticateThroughManager(realm, "dave", "s3cr3t");
        assertThrows(AuthenticationException.class, () -> authenticateThroughManager(realm, "dave", "wrong-password"));

        String log = Util.loadFile(new File(tmpDir, logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(log.isEmpty(), "Unexpected audit logs: " + log);
    }
}
