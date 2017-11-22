package hudson.plugins.audit_trail;

/**
 * Created by home on 21/11/2017.
 */
public interface CdpLogger {

    void handle(String event) throws Exception;
}
