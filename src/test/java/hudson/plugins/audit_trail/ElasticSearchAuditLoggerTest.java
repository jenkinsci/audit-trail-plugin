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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author <a href="mailto:alexander.russell@sap.com">Alex Russell</a>
 */
@WithJenkins
class ElasticSearchAuditLoggerTest {

    private static final String ES_URL = "https://localhost/myindex/jenkins";

    private JenkinsRule jenkinsRule;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        jenkinsRule = rule;
    }

    @Test
    void shouldConfigureElasticSearchAuditLogger() throws Exception {
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
    void testGetHttpPostContainsExpectedKeys() throws Exception {
        ElasticSearchAuditLogger auditLogger = new ElasticSearchAuditLogger(ES_URL, true);
        auditLogger.configure();
        assertNotNull(auditLogger.getElasticSearchSender());
        HttpPost post = auditLogger.getElasticSearchSender().getHttpPost("test-event");
        String body = EntityUtils.toString(post.getEntity());
        JSONObject json = JSONObject.fromObject(body);
        assertTrue(json.containsKey("message"), "message key should be present");
        assertTrue(json.containsKey("@timestamp"), "@timestamp key should be present");
        assertTrue(json.containsKey("jenkins.version"), "jenkins.version key should be present");
        assertTrue(json.containsKey("jenkins.url"), "jenkins.url key should be present");
        assertTrue(
                json.containsKey("jenkins.audittrail.plugin.version"),
                "jenkins.audittrail.plugin.version key should be present");
        assertTrue(
                json.containsKey("jenkins.controller.computer.name"),
                "jenkins.controller.computer.name key should be present");
        assertTrue(
                json.containsKey("jenkins.controller.computer.address"),
                "jenkins.controller.computer.address key should be present");
    }
}
