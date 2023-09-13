package hudson.plugins.audit_trail;

import static hudson.plugins.audit_trail.AuditTrailPlugin.DEFAULT_PATTERN;
import static org.junit.Assert.assertEquals;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

/**
 * Created by Pierre Beitz
 */
public class AuditTrailPluginTest {

    @ClassRule
    public static JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @ConfiguredWithCode("security-1846")
    @Issue("SECURITY-1846")
    @Test
    public void aLegacyDefaultPatternGetsReplacedByTheDefaultPattern() {
        ExtensionList<AuditTrailPlugin> extensionList = r.jenkins.getExtensionList(AuditTrailPlugin.class);
        AuditTrailPlugin plugin = extensionList.get(0);
        assertEquals(DEFAULT_PATTERN, plugin.getPattern());
    }
}
