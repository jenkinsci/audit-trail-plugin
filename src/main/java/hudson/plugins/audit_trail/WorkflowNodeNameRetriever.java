package hudson.plugins.audit_trail;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.model.Messages;
import hudson.model.Run;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.actions.WorkspaceActionImpl;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStep;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;

@Extension(optional = true)
public class WorkflowNodeNameRetriever extends BasicNodeNameRetriever {

    @Override
    public String buildNodeName(Run<?, ?> run) {
        if (run instanceof WorkflowRun workflowRun) {
            return printNodes((workflowRun));
        }
        return super.buildNodeName(run);
    }

    public String printNodes(WorkflowRun run) {
        var exec = run.getExecution();
        if (exec == null) {
            return "N/A";
        }
        var nodes = StreamSupport.stream(new FlowGraphWalker(exec).spliterator(), false)
                .filter(n -> n instanceof StepStartNode)
                .flatMap(n -> extractNodeNames((StepStartNode) n))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(";"));
        if (nodes.isEmpty()) {
            // it means we didn't find any start node, meaning agent none
            return "no agent";
        }
        return nodes;
    }

    private static Stream<String> extractNodeNames(StepStartNode node) {
        var stepDescriptor = node.getDescriptor();
        if (stepDescriptor instanceof ExecutorStep.DescriptorImpl) {
            return node.getActions(WorkspaceActionImpl.class).stream()
                    .map(WorkspaceActionImpl::getNode)
                    .map(WorkflowNodeNameRetriever::normalizeNodeName);
        }
        return Stream.empty();
    }

    @SuppressRestrictedWarnings(Messages.class)
    private static String normalizeNodeName(String nodeName) {
        if (Strings.isNullOrEmpty(nodeName)) {
            return Messages.Hudson_Computer_DisplayName();
        }
        return nodeName;
    }
}
