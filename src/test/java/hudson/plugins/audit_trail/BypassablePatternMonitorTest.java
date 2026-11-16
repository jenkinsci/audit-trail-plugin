package hudson.plugins.audit_trail;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.Issue;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Created by Pierre Beitz
 */
@ExtendWith(MockitoExtension.class)
class BypassablePatternMonitorTest {

    @Mock
    private AuditTrailPlugin plugin;

    @InjectMocks
    private BypassablePatternMonitor monitor;

    @Issue("SECURITY-1846")
    @Test
    void aProperlyProtectedPatternShouldNotTriggerTheMonitor() {
        when(plugin.getPattern()).thenReturn(".*/configSubmit/?.*");
        assertFalse(monitor.isActivated());
    }

    @Issue("SECURITY-1846")
    @Test
    void aSuffixVulnerablePatternShouldTriggerTheMonitor() {
        when(plugin.getPattern()).thenReturn(".*/configSubmit");
        assertTrue(monitor.isActivated());
    }

    @Issue("SECURITY-1846")
    @Test
    void aPrefixVulnerablePatternShouldTriggerTheMonitor() {
        when(plugin.getPattern()).thenReturn("/configSubmit/?.*");
        assertTrue(monitor.isActivated());
    }

    @Issue("SECURITY-1846")
    @Test
    void aPrefixAndSuffixVulnerablePatternShouldTriggerTheMonitor() {
        when(plugin.getPattern()).thenReturn("/configSubmit");
        assertTrue(monitor.isActivated());
    }
}
