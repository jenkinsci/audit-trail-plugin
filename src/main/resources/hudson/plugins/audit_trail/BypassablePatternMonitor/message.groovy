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
            input(name: 'default', type: 'submit', value: 'Apply default pattern', class: 'submit-button primary')
        }
        form(method: 'get', name: 'redirect-to-config', action: "${rootURL}/${monitor.url}/redirectToConfig") {
            input(name: 'config', type: 'submit', value: 'Go to configuration', class: 'submit-button primary')
        }
        raw('<b>Found bypassable Audit Trail logging patterns:</b>')
        raw(monitor.message)
        raw("For more information, please refer to <a href=\"${docLink}\">this page</a>")
    }
}
