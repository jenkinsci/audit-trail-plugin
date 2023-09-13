package hudson.plugins.audit_trail;

import static io.jenkins.plugins.casc.misc.Util.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

/**
 * Created by Pierre Beitz
 * on 2019-07-20.
 */
public class ConfigurationAsCodeTest {

    @ClassRule
    @ConfiguredWithCode("jcasc.yml")
    public static JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Issue("JENKINS-57232")
    @Test
    public void should_support_configuration_as_code() {
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
    public void should_support_configuration_export() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode auditTrailAttribute = getUnclassifiedRoot(context).get("audit-trail");

        String exported = toYamlString(auditTrailAttribute);

        String expected = toStringFromYamlFile(this, "expected.yml");

        assertThat(exported, is(expected));
    }
}
