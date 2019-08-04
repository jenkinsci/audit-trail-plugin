package hudson.plugins.audit_trail;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        assertEquals(".*/(?:configSubmit|doUninstall|doDelete|postBuildResult|enable|disable|cancelQueue|stop|toggleLogKeep|doWipeOutWorkspace|createItem|createView|toggleOffline|cancelQuietDown|quietDown|restart|exit|safeExit)", plugin.getPattern());
        assertTrue(plugin.getLogBuildCause());
        assertEquals(3, plugin.getLoggers().size());

        //first logger
        AuditLogger logger = plugin.getLoggers().get(0);
        assertTrue(logger instanceof ConsoleAuditLogger);
        assertEquals("test", ((ConsoleAuditLogger) logger).getLogPrefix());
        assertEquals(ConsoleAuditLogger.Output.STD_OUT, ((ConsoleAuditLogger) logger).getOutput());
        assertEquals("yyyy-MM-dd HH:mm:ss:SSS", ((ConsoleAuditLogger)logger).getDateFormat());

        //second logger
        logger = plugin.getLoggers().get(1);
        assertTrue(logger instanceof LogFileAuditLogger);
        assertEquals(10, ((LogFileAuditLogger)logger).getCount());
        assertEquals(666, ((LogFileAuditLogger)logger).getLimit());
        assertEquals("/log/location", ((LogFileAuditLogger)logger).getLog());

        //third logger
        logger = plugin.getLoggers().get(2);
        assertEquals("jenkins", ((SyslogAuditLogger)logger).getAppName());
        assertEquals("DAEMON", ((SyslogAuditLogger)logger).getFacility());
        assertEquals("RFC_5424", ((SyslogAuditLogger)logger).getMessageFormat());
        assertEquals("hostname", ((SyslogAuditLogger)logger).getMessageHostname());
        assertEquals("syslog-server", ((SyslogAuditLogger)logger).getSyslogServerHostname());
        assertEquals(514, ((SyslogAuditLogger)logger).getSyslogServerPort());
    }
}
