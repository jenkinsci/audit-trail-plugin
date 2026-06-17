package hudson.plugins.audit_trail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author <a href="mailto:alexander.russell@sap.com">Alex Russell</a>
 * @author Pierre Beitz
 */
@WithJenkins
class ElasticSearchAuditLoggerTest {

    private static final String ES_URL = "https://localhost/myindex/jenkins";

    @Test
    void shouldConfigureElasticSearchAuditLogger(JenkinsRule jenkinsRule) throws Exception {
        JenkinsRule.WebClient jenkinsWebClient = jenkinsRule.createWebClient();
        HtmlPage configure = jenkinsWebClient.goTo("configure");
        HtmlForm form = configure.getFormByName("config");
        jenkinsRule.getButtonByCaption(form, "Add Logger").click();
        jenkinsRule.getButtonByCaption(form, "Elastic Search server").click();
        jenkinsWebClient.waitForBackgroundJavaScript(2000);

        // When
        jenkinsRule.submit(form);

        // Then
        // submit configuration page without any errors
        AuditTrailPlugin plugin = GlobalConfiguration.all().get(AuditTrailPlugin.class);
        assertEquals(1, plugin.getLoggers().size(), "amount of loggers");
        AuditLogger logger = plugin.getLoggers().get(0);
        assertInstanceOf(ElasticSearchAuditLogger.class, logger, "ConsoleAuditLogger should be configured");
    }

    @Test
    void testElasticSearchAuditLogger() {
        ElasticSearchAuditLogger auditLogger = new ElasticSearchAuditLogger(ES_URL, true);
        auditLogger.configure();
        assertNotNull(auditLogger.getElasticSearchSender());
        assertEquals(ES_URL, auditLogger.getElasticSearchSender().getUrl());
        assertTrue(auditLogger.getElasticSearchSender().getSkipCertificateValidation());
    }

    @Test
    public void testGetHttpPostContainsExpectedKeys() throws Exception {
        ElasticSearchAuditLogger auditLogger = new ElasticSearchAuditLogger(esUrl, true);
        auditLogger.configure();
        assertTrue(auditLogger.getElasticSearchSender() != null);
        HttpPost post = auditLogger.getElasticSearchSender().getHttpPost("test-event");
        String body = EntityUtils.toString(post.getEntity());
        JSONObject json = JSONObject.fromObject(body);
        assertTrue("message key should be present", json.containsKey("message"));
        assertTrue("@timestamp key should be present", json.containsKey("@timestamp"));
        assertTrue("jenkins.version key should be present", json.containsKey("jenkins.version"));
        assertTrue("jenkins.url key should be present", json.containsKey("jenkins.url"));
        assertTrue(
                "jenkins.audittrail.plugin.version key should be present",
                json.containsKey("jenkins.audittrail.plugin.version"));
        assertTrue(
                "jenkins.controller.computer.name key should be present",
                json.containsKey("jenkins.controller.computer.name"));
        assertTrue(
                "jenkins.controller.computer.address key should be present",
                json.containsKey("jenkins.controller.computer.address"));
    }
}
