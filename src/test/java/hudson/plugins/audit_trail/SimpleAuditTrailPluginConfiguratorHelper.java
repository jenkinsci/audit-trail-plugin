package hudson.plugins.audit_trail;

import static hudson.plugins.audit_trail.LogFileAuditLogger.DEFAULT_LOG_SEPARATOR;

import java.io.File;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Created by Pierre Beitz
 */
public class SimpleAuditTrailPluginConfiguratorHelper {
    private static final int TIMEOUT = 2000;
    private static final String LOG_LOCATION_INPUT_NAME = "_.log";
    private static final String LOG_FILE_SIZE_INPUT_NAME = "_.limit";
    private static final String LOG_FILE_COUNT_INPUT_NAME = "_.count";
    private static final String LOG_FILE_LOG_SEPARATOR_INPUT_NAME = "_.logSeparator";
    private static final String PATTERN_INPUT_NAME = "pattern";
    private static final String LOG_BUILD_CAUSE_INPUT_NAME = "logBuildCause";
    private static final String LOG_CREDENTIALS_USAGE_INPUT_NAME = "logCredentialsUsage";
    private static final String ADD_LOGGER_BUTTON_TEXT = "Add Logger";
    private static final String LOG_FILE_COMBO_TEXT = new LogFileAuditLogger.DescriptorImpl().getDisplayName();

    private final File logFile;

    private boolean logBuildCause = true;
    private boolean logCredentialsUsage = true;
    private String pattern = ".*/(?:enable|cancelItem|quietDown|createItem)/?.*";

    public SimpleAuditTrailPluginConfiguratorHelper(File logFile) {
        this.logFile = logFile;
    }

    public SimpleAuditTrailPluginConfiguratorHelper withLogBuildCause(boolean logBuildCause) {
        this.logBuildCause = logBuildCause;
        return this;
    }

    public SimpleAuditTrailPluginConfiguratorHelper withLogCredentialsUsage(boolean logCredentialsUsage) {
        this.logCredentialsUsage = logCredentialsUsage;
        return this;
    }

    public SimpleAuditTrailPluginConfiguratorHelper withPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public void sendConfiguration(JenkinsRule j, JenkinsRule.WebClient wc) throws Exception {
        HtmlPage configure = wc.goTo("configure");
        HtmlForm form = configure.getFormByName("config");
        j.getButtonByCaption(form, ADD_LOGGER_BUTTON_TEXT).click();
        configure.getAnchorByText(LOG_FILE_COMBO_TEXT).click();
        wc.waitForBackgroundJavaScript(TIMEOUT);
        form.getInputByName(LOG_LOCATION_INPUT_NAME).setValue(logFile.getPath());
        form.getInputByName(LOG_FILE_SIZE_INPUT_NAME).setValue("1");
        form.getInputByName(LOG_FILE_COUNT_INPUT_NAME).setValue("2");
        form.getInputByName(LOG_FILE_LOG_SEPARATOR_INPUT_NAME).setValue(DEFAULT_LOG_SEPARATOR);
        form.getInputByName(PATTERN_INPUT_NAME).setValue(pattern);
        form.getInputByName(LOG_BUILD_CAUSE_INPUT_NAME).setChecked(logBuildCause);
        form.getInputByName(LOG_CREDENTIALS_USAGE_INPUT_NAME).setChecked(logCredentialsUsage);
        j.submit(form);
    }
}
