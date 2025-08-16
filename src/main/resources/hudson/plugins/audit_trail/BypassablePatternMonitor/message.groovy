package hudson.plugins.audit_trail.BypassablePatternMonitor

/**
 * Administrative monitor for SECURITY-1846
 * @author Pierre Beitz
 */
def monitor = my
def docLink = 'https://github.com/jenkinsci/audit-trail-plugin/tree/master/docs/bypassable-patterns.adoc'

dl {
    div(class: 'alert alert-warning') {
        form(method: 'post', name: 'default', action: "${rootURL}/${monitor.url}/applyDefault") {
            button(name: 'default', type: 'submit', class: 'jenkins-button jenkins-button--primary') {
                text("Apply default pattern")
            }
        }
        form(method: 'get', name: 'redirect-to-config', action: "${rootURL}/${monitor.url}/redirectToConfig") {
            button(name: 'config', type: 'submit', class: 'jenkins-button jenkins-button--primary jenkins-!-margin-right-1') {
                text("Go to configuration")
            }
        }
        raw('<b>Found bypassable Audit Trail logging patterns:</b>')
        raw(monitor.message)
        raw("For more information, please refer to <a href=\"${docLink}\">this page</a>")
    }
}
