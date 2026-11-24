package dev.opticore.client.shader;

import java.util.Set;

public final class ShaderRenderGraphSelfTest {
    public static void main(String[] args) {
        validatesProducerConsumerOrder();
        acceptsExternalInputs();
        rejectsMissingResourcesAndDependencies();
        rejectsMultipleWriters();
        rejectsCycles();
        rejectsInvalidPassDeclarations();
        System.out.println("ShaderRenderGraphSelfTest: OK");
    }

    private static void validatesProducerConsumerOrder() {
        ShaderRenderGraph graph = ShaderRenderGraph.builder()
                .addPass("composite", Set.of("scene"), Set.of("final"), Set.of(),
                        ShaderRenderGraph.DepthMode.NONE)
                .addPass("world", Set.of(), Set.of("scene"), Set.of(),
                        ShaderRenderGraph.DepthMode.READ_WRITE)
                .build();
        ShaderRenderGraph.Validation result = graph.validate();
        check(result.valid(), "valid producer/consumer graph");
        check(result.executionOrder().equals(java.util.List.of("world", "composite")),
                "producer must run before consumer regardless of insertion order");
    }

    private static void acceptsExternalInputs() {
        ShaderRenderGraph graph = ShaderRenderGraph.builder()
                .externalResource("minecraft:main_color")
                .addPass("tonemap", Set.of("minecraft:main_color"), Set.of("display"),
                        Set.of(), ShaderRenderGraph.DepthMode.NONE)
                .build();
        check(graph.validate().valid(), "external input should be accepted");
    }

    private static void rejectsMissingResourcesAndDependencies() {
        ShaderRenderGraph graph = ShaderRenderGraph.builder()
                .addPass("post", Set.of("missing_texture"), Set.of("output"),
                        Set.of("unknown_pass"), ShaderRenderGraph.DepthMode.NONE)
                .build();
        ShaderRenderGraph.Validation result = graph.validate();
        check(!result.valid(), "missing declarations must fail validation");
        check(result.errors().size() == 2, "both missing resource and dependency should be reported");
    }

    private static void rejectsMultipleWriters() {
        ShaderRenderGraph graph = ShaderRenderGraph.builder()
                .addPass("first", Set.of(), Set.of("color"), Set.of(), ShaderRenderGraph.DepthMode.NONE)
                .addPass("second", Set.of(), Set.of("color"), Set.of(), ShaderRenderGraph.DepthMode.NONE)
                .build();
        check(!graph.validate().valid(), "multiple writers must be rejected");
    }

    private static void rejectsCycles() {
        ShaderRenderGraph graph = ShaderRenderGraph.builder()
                .addPass("a", Set.of(), Set.of("a_out"), Set.of("b"), ShaderRenderGraph.DepthMode.NONE)
                .addPass("b", Set.of(), Set.of("b_out"), Set.of("a"), ShaderRenderGraph.DepthMode.NONE)
                .build();
        check(!graph.validate().valid(), "dependency cycles must be rejected");
    }

    private static void rejectsInvalidPassDeclarations() {
        expectFailure(() -> new ShaderRenderGraph.Pass("bad id", Set.of(), Set.of("out"),
                Set.of(), ShaderRenderGraph.DepthMode.NONE), "invalid pass id");
        expectFailure(() -> new ShaderRenderGraph.Pass("empty", Set.of(), Set.of(),
                Set.of(), ShaderRenderGraph.DepthMode.NONE), "pass without outputs");
        expectFailure(() -> new ShaderRenderGraph.Pass("feedback", Set.of("image"), Set.of("image"),
                Set.of(), ShaderRenderGraph.DepthMode.NONE), "same-pass read/write feedback");
        expectFailure(() -> ShaderRenderGraph.builder()
                .addPass("same", Set.of(), Set.of("out"), Set.of(), ShaderRenderGraph.DepthMode.NONE)
                .addPass("same", Set.of(), Set.of("other"), Set.of(), ShaderRenderGraph.DepthMode.NONE),
                "duplicate pass id");
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message + " should fail");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
