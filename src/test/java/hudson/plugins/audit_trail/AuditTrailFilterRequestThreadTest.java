package hudson.plugins.audit_trail;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

public class AuditTrailFilterRequestThreadTest {

    @After
    public void resetPattern() {
        AuditTrailFilter.setPattern(AuditTrailPlugin.DEFAULT_PATTERN);
    }

    @Issue("JENKINS-76557")
    @Test
    @SuppressWarnings("deprecation")
    public void requestIsOnlyReadOnTheRequestThread() throws Exception {
        // Request objects are not thread-safe and Jetty recycles them, so the filter must only read the request on
        // the request thread. Record the thread of every call made on the request.
        Set<Thread> callingThreads = ConcurrentHashMap.newKeySet();
        HttpServletRequest request = mock(
                HttpServletRequest.class,
                withSettings().invocationListeners(report -> callingThreads.add(Thread.currentThread())));
        when(request.getRequestURI()).thenReturn("/jenkins/createItem");
        when(request.getContextPath()).thenReturn("/jenkins");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getParameter("name")).thenReturn("created-from-api");
        callingThreads.clear();

        AuditLogger logger = mock(AuditLogger.class);
        AuditTrailPlugin configuration = mock(AuditTrailPlugin.class);
        when(configuration.getLoggers()).thenReturn(List.of(logger));
        AuditTrailFilter.setPattern(AuditTrailPlugin.DEFAULT_PATTERN);

        new AuditTrailFilter(configuration).handle(request, null);

        // The filter logs on its own executor, so wait for the audit line. Avoiding this timeout would need a change
        // in AuditTrailFilter: accept the ExecutorService in a package-private constructor, so the test can capture
        // the submitted task and run it itself once handle() has returned.
        // No user is logged in, so the line names NA.
        verify(logger, timeout(10_000)).log("/createItem (created-from-api) by NA from 127.0.0.1");
        assertEquals(Set.of(Thread.currentThread()), callingThreads);
    }
}
