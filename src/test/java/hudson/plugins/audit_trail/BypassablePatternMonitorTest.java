package hudson.plugins.audit_trail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Created by Pierre Beitz
 */
@RunWith(MockitoJUnitRunner.class)
public class BypassablePatternMonitorTest {

    @Mock
    private AuditTrailPlugin plugin;
    @InjectMocks
    private BypassablePatternMonitor monitor;

    @Issue("SECURITY-1846")
    @Test
    public void aProperlyProtectedPatternShouldNotTriggerTheMonitor() {
        when(plugin.getPattern()).thenReturn(".*/configSubmit/?.*");
        assertFalse(monitor.isActivated());
    }

    @Issue("SECURITY-1846")
    @Test
    public void aSuffixVulnerablePatternShouldTriggerTheMonitor() {
        when(plugin.getPattern()).thenReturn(".*/configSubmit");
        assertTrue(monitor.isActivated());
    }

    @Issue("SECURITY-1846")
    @Test
    public void aPrefixVulnerablePatternShouldTriggerTheMonitor() {
        when(plugin.getPattern()).thenReturn("/configSubmit/?.*");
        assertTrue(monitor.isActivated());
    }

    @Issue("SECURITY-1846")
    @Test
    public void aPrefixAndSuffixVulnerablePatternShouldTriggerTheMonitor() {
        when(plugin.getPattern()).thenReturn("/configSubmit");
        assertTrue(monitor.isActivated());
    }
}
