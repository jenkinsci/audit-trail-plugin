package hudson.plugins.audit_trail;

import hudson.model.Hudson;
import hudson.model.User;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Servlet filter to watch requests and log those we are interested in.
 */
public class AuditTrailFilter implements Filter {

    static Pattern uriPattern = null;
    static Logger LOG = Logger.getLogger(AuditTrailFilter.class.getName());

    public void init(FilterConfig fc) {
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String uri = ((HttpServletRequest)req).getRequestURI();
        if (uriPattern != null && uriPattern.matcher(uri).matches()) {
            User user = User.current();
            String username = user != null ? user.getId() : "?";
            LOG.config(uri + " by " + username);
        }
        chain.doFilter(req, res);
    }

    public void destroy() {
    }
}
