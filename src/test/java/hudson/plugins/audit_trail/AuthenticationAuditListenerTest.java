package hudson.plugins.audit_trail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import hudson.Util;
import hudson.security.HudsonPrivateSecurityRealm;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

public class AuthenticationAuditListenerTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

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
    public void successfulLoginIsLogged() throws Exception {
        String logFileName = "successfulLoginIsLogged.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        setUpRealmWithUser("alice", "s3cr3t");
        wc.login("alice", "s3cr3t");

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                "Successful login was not logged. Logged actions: " + log,
                Pattern.compile(".*Successful login for user 'alice'.*", Pattern.DOTALL)
                        .matcher(log)
                        .matches());
    }

    @Test
    public void failedLoginIsLogged() throws Exception {
        String logFileName = "failedLoginIsLogged.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        HudsonPrivateSecurityRealm realm = setUpRealmWithUser("bob", "correct-password");
        try {
            authenticateThroughManager(realm, "bob", "wrong-password");
            fail("expected authentication failure");
        } catch (AuthenticationException expected) {
            // expected: wrong password
        }

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                "Failed login was not logged. Logged actions: " + log,
                Pattern.compile(".*Failed login attempt for user 'bob'.*", Pattern.DOTALL)
                        .matcher(log)
                        .matches());
    }

    @Test
    public void repeatedFailedLoginWithinDedupWindowIsLoggedOnce() throws Exception {
        String logFileName = "repeatedFailedLoginIsDeduped.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        HudsonPrivateSecurityRealm realm = setUpRealmWithUser("eve", "correct-password");
        for (int i = 0; i < 2; i++) {
            try {
                authenticateThroughManager(realm, "eve", "wrong-password");
                fail("expected authentication failure");
            } catch (AuthenticationException expected) {
                // expected: wrong password
            }
        }

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("Failed login attempt for user 'eve'").matcher(log);
        int occurrences = 0;
        while (matcher.find()) {
            occurrences++;
        }
        assertEquals("Repeated failed login was not deduped. Logged actions: " + log, 1, occurrences);
    }

    @Test
    public void logoutIsLogged() throws Exception {
        String logFileName = "logoutIsLogged.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile).sendConfiguration(r, wc);

        setUpRealmWithUser("carol", "s3cr3t");
        wc.login("carol", "s3cr3t");
        wc.goTo("logout");

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue(
                "Logout was not logged. Logged actions: " + log,
                Pattern.compile(".*Logout for user 'carol'.*", Pattern.DOTALL)
                        .matcher(log)
                        .matches());
    }

    @Test
    public void disabledLoggingOptionIsRespected() throws Exception {
        String logFileName = "disabledAuthEventsLoggingIsRespected.log";
        File logFile = new File(tmpDir.getRoot(), logFileName);
        JenkinsRule.WebClient wc = r.createWebClient();
        new SimpleAuditTrailPluginConfiguratorHelper(logFile)
                .withLogAuthEvents(false)
                .sendConfiguration(r, wc);

        HudsonPrivateSecurityRealm realm = setUpRealmWithUser("dave", "s3cr3t");
        authenticateThroughManager(realm, "dave", "s3cr3t");
        try {
            authenticateThroughManager(realm, "dave", "wrong-password");
            fail("expected authentication failure");
        } catch (AuthenticationException expected) {
            // expected: wrong password
        }

        String log = Util.loadFile(new File(tmpDir.getRoot(), logFileName + ".0"), StandardCharsets.UTF_8);
        assertTrue("Unexpected audit logs: " + log, log.isEmpty());
    }
}
