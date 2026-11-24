package dev.opticore.client.shader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Executes a validated render graph through caller-supplied pass handlers.
 *
 * The executor is deliberately backend-agnostic. A handler may wrap OpenGL,
 * but must own and restore every graphics state it changes. Execution stops at
 * the first failure; later passes are never invoked.
 */
public final class ShaderRenderGraphExecutor {
    @FunctionalInterface
    public interface PassHandler {
        void execute(ShaderRenderGraph.Pass pass) throws Exception;
    }

    public record ExecutionResult(
            boolean success,
            List<String> completedPasses,
            String failedPass,
            List<String> diagnostics) {
        public ExecutionResult {
            completedPasses = List.copyOf(completedPasses);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    private ShaderRenderGraphExecutor() {}

    public static ExecutionResult execute(
            ShaderRenderGraph graph,
            Map<String, PassHandler> handlers) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(handlers, "handlers");

        ShaderRenderGraph.Validation validation = graph.validate();
        if (!validation.valid()) {
            return new ExecutionResult(false, List.of(), null, validation.errors());
        }

        Map<String, ShaderRenderGraph.Pass> passes = new LinkedHashMap<>();
        for (ShaderRenderGraph.Pass pass : graph.passes()) {
            passes.put(pass.id(), pass);
        }

        List<String> completed = new ArrayList<>();
        for (String passId : validation.executionOrder()) {
            PassHandler handler = handlers.get(passId);
            if (handler == null) {
                return new ExecutionResult(false, completed, passId,
                        List.of("No execution handler registered for pass '" + passId + "'."));
            }
            try {
                handler.execute(passes.get(passId));
                completed.add(passId);
            } catch (Exception exception) {
                String message = exception.getMessage();
                String detail = message == null || message.isBlank()
                        ? exception.getClass().getSimpleName() : message;
                return new ExecutionResult(false, completed, passId,
                        List.of("Pass '" + passId + "' failed: " + detail));
            }
        }
        return new ExecutionResult(true, completed, null, List.of());
    }
}
