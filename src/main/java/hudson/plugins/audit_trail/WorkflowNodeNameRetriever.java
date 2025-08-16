package hudson.plugins.audit_trail;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.model.Run;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.actions.WorkspaceActionImpl;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStep;

/**
 * Created by Pierre Beitz
 * on 16/08/2025.
 */
@Extension(optional = true)
public class WorkflowNodeNameRetriever extends BasicNodeNameRetriever {
    private static final String N_A = "N/A";

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
            return N_A;
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

    private static String normalizeNodeName(String nodeName) {
        if (Strings.isNullOrEmpty(nodeName)) {
            var computer = Jenkins.get().toComputer();
            if (computer == null) {
                // shouldn't happen, otherwise how would Jenkins build on it ?
                return N_A;
            }
            return computer.getDisplayName();
        }
        return nodeName;
    }
}
