/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Alan Harder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.audit_trail;

import hudson.model.User;
import jenkins.model.Jenkins;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * Servlet filter to watch requests and log those we are interested in.
 * @author Alan Harder
 */
public class AuditTrailFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(AuditTrailFilter.class.getName());

    private static Pattern uriPattern = null;

    private final AuditTrailPlugin plugin;

    public AuditTrailFilter(AuditTrailPlugin plugin) {
        this.plugin = plugin;
    }

    public void init(FilterConfig fc) {
    }

    static void setPattern(String pattern) throws PatternSyntaxException {
        uriPattern = Pattern.compile(pattern);
        LOGGER.log(Level.FINE, "set pattern to {0}", pattern);
    }

    public void doFilter(ServletRequest request, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String uri;
        if (req.getPathInfo() == null) {
            // workaround: on some containers such as CloudBees DEV@cloud, req.getPathInfo() is unexpectedly null,
            // construct pathInfo based on contextPath and requestUri
            uri = req.getRequestURI().substring(req.getContextPath().length());
        } else {
            uri = req.getPathInfo();
        }
        if (uriPattern != null && uriPattern.matcher(uri).matches()) {
            User user = User.current();
            String username = user != null ? user.getId() : req.getRemoteAddr(),
                   extra = "";
            // For queue items, show what task is in the queue:
            if (uri.startsWith("/queue/item/")) try {
                extra = " (" + Jenkins.getInstance().getQueue().getItem(Integer.parseInt(
                        uri.substring(12, uri.indexOf('/', 13)))).task.getUrl() + ')';
            } catch (Exception ignore) { }

            if(LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Audit request {0} by user {1}", new Object[]{uri, username});

            plugin.onRequest(uri, extra, username);
        } else {
            LOGGER.log(Level.FINEST, "Skip audit for request {0}", uri);
        }
        chain.doFilter(req, res);
    }

    public void destroy() {
    }
}
