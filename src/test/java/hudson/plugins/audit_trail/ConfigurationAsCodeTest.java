package hudson.plugins.audit_trail;

import static io.jenkins.plugins.casc.misc.Util.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

/**
 * @author Pierre Beitz
 */
@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeTest {

    @Issue("JENKINS-57232")
    @Test
    @ConfiguredWithCode("jcasc.yml")
    void shouldSupportConfigurationAsCode(JenkinsConfiguredWithCodeRule r) {
        ExtensionList<AuditTrailPlugin> extensionList = r.jenkins.getExtensionList(AuditTrailPlugin.class);
        AuditTrailPlugin plugin = extensionList.get(0);
        assertEquals(
                ".*/(?:configSubmit|doUninstall|doDelete|postBuildResult|enable|disable|cancelQueue|stop|toggleLogKeep|doWipeOutWorkspace|createItem|createView|toggleOffline|cancelQuietDown|quietDown|restart|exit|safeExit)",
                plugin.getPattern());
        assertTrue(plugin.getLogBuildCause());
        assertTrue(plugin.shouldLogCredentialsUsage());
        assertEquals(3, plugin.getLoggers().size());

        // first logger
        AuditLogger logger = plugin.getLoggers().get(0);
        assertTrue(logger instanceof ConsoleAuditLogger);
        assertEquals("test", ((ConsoleAuditLogger) logger).getLogPrefix());
        assertEquals(ConsoleAuditLogger.Output.STD_OUT, ((ConsoleAuditLogger) logger).getOutput());
        assertEquals("yyyy-MM-dd HH:mm:ss:SSS", ((ConsoleAuditLogger) logger).getDateFormat());

        // second logger
        logger = plugin.getLoggers().get(1);
        assertTrue(logger instanceof LogFileAuditLogger);
        assertEquals(10, ((LogFileAuditLogger) logger).getCount());
        assertEquals(666, ((LogFileAuditLogger) logger).getLimit());
        assertEquals("/log/location", ((LogFileAuditLogger) logger).getLog());
        assertEquals(";", ((LogFileAuditLogger) logger).getLogSeparator());

        // third logger
        logger = plugin.getLoggers().get(2);
        assertEquals("jenkins", ((SyslogAuditLogger) logger).getAppName());
        assertEquals("DAEMON", ((SyslogAuditLogger) logger).getFacility());
        assertEquals("RFC_5424", ((SyslogAuditLogger) logger).getMessageFormat());
        assertEquals("hostname", ((SyslogAuditLogger) logger).getMessageHostname());
        assertEquals("syslog-server", ((SyslogAuditLogger) logger).getSyslogServerHostname());
        assertEquals(514, ((SyslogAuditLogger) logger).getSyslogServerPort());
    }

    @Issue("JENKINS-57232")
    @Test
    @ConfiguredWithCode("jcasc.yml")
    public void should_support_configuration_export(JenkinsConfiguredWithCodeRule r) throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode auditTrailAttribute = getUnclassifiedRoot(context).get("audit-trail");

        String exported = toYamlString(auditTrailAttribute);

        String expected = toStringFromYamlFile(this, "expected.yml");

        assertThat(exported, is(expected));
    }
}
