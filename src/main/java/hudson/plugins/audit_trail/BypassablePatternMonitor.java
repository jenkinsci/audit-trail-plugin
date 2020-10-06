package hudson.plugins.audit_trail;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.util.FormValidation;
import hudson.util.HttpResponses;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static hudson.plugins.audit_trail.AuditTrailPlugin.getKnownKeywords;

/**
 * Manages the default old patterns used by the plugin, subjects to SECURITY-1846
 *
 * @author Pierre Beitz
 */
@Extension
public class BypassablePatternMonitor extends AdministrativeMonitor {

    private static final List<String> LEGACY_DEFAULT_PATTERNS = Arrays.asList(
          // up until 3.5
          ".*/(?:configSubmit|doDelete|postBuildResult|enable|disable|"
                + "cancelQueue|stop|toggleLogKeep|doWipeOutWorkspace|createItem|createView|toggleOffline|"
                + "cancelQuietDown|quietDown|restart|exit|safeExit)",
          // up until 2.1
          ".*/(?:configSubmit|doDelete|postBuildResult|"
                + "cancelQueue|stop|toggleLogKeep|doWipeOutWorkspace|createItem|createView|toggleOffline)",
          // up until 1.1
          ".*/(?:configSubmit|doDelete|build|toggleLogKeep|doWipeOutWorkspace|createItem|createView)"
    );

    @Inject
    private AuditTrailPlugin auditTrailPlugin;

    static boolean isLegacyBypassableDefaultPattern(String pattern) {
        return LEGACY_DEFAULT_PATTERNS.contains(pattern);
    }

    static FormValidation validatePatternAgainstKnownKeywords(String pattern) {
        Pattern p = Pattern.compile(pattern);
        return FormValidation.aggregate(
              getKnownKeywords().stream()
                    .map(keyword -> new BypassablePatternDetector(keyword, p))
                    .filter(BypassablePatternDetector::isBypassed)
                    .map(BypassablePatternDetector::buildWarningMessage)
                    .collect(Collectors.toList())
        );
    }

    public String getMessage() {
        return validatePatternAgainstKnownKeywords(auditTrailPlugin.getPattern()).renderHtml();
    }

    public HttpResponse doRedirectToConfig() {
        return HttpResponses.redirectViaContextPath("configure");
    }

    @RequirePOST
    public HttpResponse doApplyDefault() {
        auditTrailPlugin.resetPattern();
        return HttpResponses.redirectToContextRoot();
    }

    @Override
    public boolean isActivated() {
        return validatePatternAgainstKnownKeywords(auditTrailPlugin.getPattern()).kind != FormValidation.Kind.OK;
    }

    private static class BypassablePatternDetector {
        private final String keyword;
        private final List<String> messages;

        BypassablePatternDetector(String keyword, Pattern p) {
            this.keyword = keyword;
            messages = new ArrayList<>();
            if (p.matcher(createLegitUrl()).matches()) {
                if (!p.matcher(createPrefixBypassableUrl()).matches()) {
                    messages.add("crafted URLs with prefix like " + createPrefixBypassableUrl());
                }
                if (!p.matcher(createSuffixBypassableUrl()).matches()) {
                    messages.add("crafted URLs with suffix like " + createSuffixBypassableUrl());
                }
            }
        }

        private String createLegitUrl() {
            return "/" + keyword;
        }

        private String createPrefixBypassableUrl() {
            return "/static/forged/" + keyword;
        }

        private String createSuffixBypassableUrl() {
            return "/" + keyword + "/forged";
        }

        boolean isBypassed() {
            return !messages.isEmpty();
        }

        FormValidation buildWarningMessage() {
            return FormValidation.warning("Pattern seems to attempt to match " + createLegitUrl() + " but doesn't handle " + buildCraftedUrlMessage());
        }

        private String buildCraftedUrlMessage() {
            return String.join(" and ", messages);
        }
    }
}
