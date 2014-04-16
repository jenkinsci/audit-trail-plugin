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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


/**
 * Servlet filter to watch requests and log those we are interested in.
 * @author Alan Harder
 */
public class AuditTrailFilter implements Filter {

    protected final Logger LOGGER = Logger.getLogger(getClass().getName());

    static Pattern uriPattern = null;

    private final AuditTrailPlugin plugin;

    public AuditTrailFilter(AuditTrailPlugin plugin) {
        this.plugin = plugin;
    }

    public void init(FilterConfig fc) {
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String uri = ((HttpServletRequest)req).getPathInfo();
        uri = (uri == null) ? "/" : uri;
        if (uriPattern != null && uriPattern.matcher(uri).matches()) {
            User user = User.current();
            String username = user != null ? user.getId() : req.getRemoteAddr(),
                   extra = "";
            // For queue items, show what task is in the queue:
            if (uri.startsWith("/queue/item/")) try {
                extra = " (" + Hudson.getInstance().getQueue().getItem(Integer.parseInt(
                        uri.substring(12, uri.indexOf('/', 13)))).task.getUrl() + ')';
            } catch (Exception ignore) { }

            if(LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINER, "Audit request {0} by user {1}", new Object[]{uri, username});

            plugin.onRequest(uri, extra, username);
        } else {
            if(LOGGER.isLoggable(Level.FINEST))
                LOGGER.log(Level.FINEST, "Skip audit for request {0}", uri);
        }
        chain.doFilter(req, res);
    }

    public void destroy() {
    }
}
