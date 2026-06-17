package hudson.plugins.audit_trail;

import static hudson.plugins.audit_trail.AuditTrailPlugin.DEFAULT_PATTERN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

/**
 * @author Pierre Beitz
 */
@WithJenkinsConfiguredWithCode
class AuditTrailPluginTest {

    @ConfiguredWithCode("security-1846.yaml")
    @Issue("SECURITY-1846")
    @Test
    void aLegacyDefaultPatternGetsReplacedByTheDefaultPattern(JenkinsConfiguredWithCodeRule r) {
        ExtensionList<AuditTrailPlugin> extensionList = r.jenkins.getExtensionList(AuditTrailPlugin.class);
        AuditTrailPlugin plugin = extensionList.get(0);
        assertEquals(DEFAULT_PATTERN, plugin.getPattern());
    }
}
