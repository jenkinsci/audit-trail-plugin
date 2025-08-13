package hudson.plugins.audit_trail;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Run;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class BasicNodeNameRetriever {
    private static final Logger LOGGER = Logger.getLogger(BasicNodeNameRetriever.class.getName());

    static final String UNKNOWN_NODE = "#unknown#";

    public String buildNodeName(Run<?, ?> run) {
        if (run instanceof AbstractBuild) {
            var abstractBuild = (AbstractBuild<?, ?>) run;
            Node node = abstractBuild.getBuiltOn();
            if (node != null) {
                return node.getDisplayName();
            }
            return abstractBuild.getBuiltOnStr() != null ? abstractBuild.getBuiltOnStr() : "built-in";
        } else {
            LOGGER.log(
                    Level.FINE,
                    "Run is not an AbstractBuild but a {0}, will log the build node as {1}.",
                    new Object[] {run.getClass().getName(), UNKNOWN_NODE});
        }
        return UNKNOWN_NODE;
    }
}
