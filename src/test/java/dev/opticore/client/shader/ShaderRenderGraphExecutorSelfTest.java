package dev.opticore.client.shader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ShaderRenderGraphExecutorSelfTest {
    public static void main(String[] args) {
        executesInDependencyOrder();
        stopsWhenHandlerIsMissing();
        stopsAfterFirstHandlerFailure();
        rejectsInvalidGraphBeforeExecuting();
        System.out.println("ShaderRenderGraphExecutorSelfTest: OK");
    }

    private static void executesInDependencyOrder() {
        ShaderRenderGraph graph = ShaderRenderGraph.builder()
                .addPass("post", Set.of("scene"), Set.of("final"), Set.of(),
                        ShaderRenderGraph.DepthMode.NONE)
                .addPass("scene", Set.of(), Set.of("scene"), Set.of(),
                        ShaderRenderGraph.DepthMode.READ_WRITE)
                .build();
        List<String> calls = new ArrayList<>();
        ShaderRenderGraphExecutor.ExecutionResult result = ShaderRenderGraphExecutor.execute(graph, Map.of(
                "scene", pass -> calls.add(pass.id()),
                "post", pass -> calls.add(pass.id())));
        check(result.success(), "execution should succeed");
        check(calls.equals(List.of("scene", "post")), "handlers must follow graph schedule");
        check(result.completedPasses().equals(calls), "completed pass list should reflect actual work");
    }

    private static void stopsWhenHandlerIsMissing() {
        ShaderRenderGraph graph = ShaderRenderGraph.builder()
                .addPass("first", Set.of(), Set.of("a"), Set.of(), ShaderRenderGraph.DepthMode.NONE)
                .addPass("second", Set.of("a"), Set.of("b"), Set.of(), ShaderRenderGraph.DepthMode.NONE)
                .build();
        boolean[] called = {false};
        ShaderRenderGraphExecutor.ExecutionResult result =
                ShaderRenderGraphExecutor.execute(graph,
                        Map.of("first", pass -> called[0] = true));
        check(!result.success(), "missing handler must fail");
        check("second".equals(result.failedPass()), "missing handler should identify pass");
        check(!called[0], "preflight must detect missing later handlers before any pass executes");
        check(result.completedPasses().isEmpty(), "nothing should be marked complete");
    }

    private static void stopsAfterFirstHandlerFailure() {
        ShaderRenderGraph graph = ShaderRenderGraph.builder()
                .addPass("first", Set.of(), Set.of("a"), Set.of(), ShaderRenderGraph.DepthMode.NONE)
                .addPass("second", Set.of("a"), Set.of("b"), Set.of(), ShaderRenderGraph.DepthMode.NONE)
                .addPass("third", Set.of("b"), Set.of("c"), Set.of(), ShaderRenderGraph.DepthMode.NONE)
                .build();
        List<String> calls = new ArrayList<>();
        ShaderRenderGraphExecutor.ExecutionResult result = ShaderRenderGraphExecutor.execute(graph, Map.of(
                "first", pass -> calls.add(pass.id()),
                "second", pass -> { calls.add(pass.id()); throw new IllegalStateException("test failure"); },
                "third", pass -> calls.add(pass.id())));
        check(!result.success(), "handler exception must fail execution");
        check("second".equals(result.failedPass()), "failed pass should be reported");
        check(calls.equals(List.of("first", "second")), "later pass must not execute");
        check(result.completedPasses().equals(List.of("first")), "only completed passes are reported");
    }

    private static void rejectsInvalidGraphBeforeExecuting() {
        ShaderRenderGraph graph = ShaderRenderGraph.builder()
                .addPass("bad", Set.of("missing"), Set.of("output"), Set.of(),
                        ShaderRenderGraph.DepthMode.NONE)
                .build();
        boolean[] called = {false};
        ShaderRenderGraphExecutor.ExecutionResult result =
                ShaderRenderGraphExecutor.execute(graph, Map.of("bad", pass -> called[0] = true));
        check(!result.success(), "invalid graph must fail");
        check(!called[0], "no handler may run for invalid graph");
        check(result.failedPass() == null, "validation failure is not an execution-pass failure");
    }

    private static ShaderRenderGraph singlePassGraph() {
        return ShaderRenderGraph.builder()
                .addPass("draw", Set.of(), Set.of("output"), Set.of(),
                        ShaderRenderGraph.DepthMode.NONE)
                .build();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
