package dev.opticore.client.shader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, Minecraft-independent description of a frame's render-pass graph.
 *
 * This class validates scheduling and resource declarations only. It deliberately
 * performs no OpenGL calls and does not imply that any pass is connected to the
 * Minecraft world renderer.
 */
public final class ShaderRenderGraph {
    public enum DepthMode { NONE, READ_ONLY, READ_WRITE, CLEAR }

    public record Pass(
            String id,
            Set<String> reads,
            Set<String> writes,
            Set<String> dependsOn,
            DepthMode depthMode) {
        public Pass {
            id = requireIdentifier(id, "pass id");
            reads = immutableIdentifiers(reads, "read resource");
            writes = immutableIdentifiers(writes, "written resource");
            dependsOn = immutableIdentifiers(dependsOn, "dependency");
            depthMode = Objects.requireNonNull(depthMode, "depthMode");
            if (writes.isEmpty()) {
                throw new IllegalArgumentException("A render pass must declare at least one output: " + id);
            }
            Set<String> overlap = new HashSet<>(reads);
            overlap.retainAll(writes);
            if (!overlap.isEmpty()) {
                throw new IllegalArgumentException("A pass cannot read and write the same resource: " + overlap);
            }
        }
    }

    public record Validation(boolean valid, List<String> errors, List<String> executionOrder) {
        public Validation {
            errors = List.copyOf(errors);
            executionOrder = List.copyOf(executionOrder);
        }
    }

    private final Map<String, Pass> passes;
    private final Set<String> externalResources;

    private ShaderRenderGraph(Map<String, Pass> passes, Set<String> externalResources) {
        this.passes = Collections.unmodifiableMap(new LinkedHashMap<>(passes));
        this.externalResources = Set.copyOf(externalResources);
    }

    public static Builder builder() { return new Builder(); }

    public List<Pass> passes() { return List.copyOf(passes.values()); }

    public Set<String> externalResources() { return externalResources; }

    public Validation validate() {
        List<String> errors = new ArrayList<>();
        Map<String, String> writers = new HashMap<>();
        for (Pass pass : passes.values()) {
            for (String dependency : pass.dependsOn()) {
                if (!passes.containsKey(dependency)) {
                    errors.add("Pass '" + pass.id() + "' depends on missing pass '" + dependency + "'.");
                }
            }
            for (String resource : pass.writes()) {
                String previous = writers.putIfAbsent(resource, pass.id());
                if (previous != null) {
                    errors.add("Resource '" + resource + "' has multiple writers: '" + previous
                            + "' and '" + pass.id() + "'.");
                }
            }
        }

        for (Pass pass : passes.values()) {
            for (String resource : pass.reads()) {
                if (!externalResources.contains(resource) && !writers.containsKey(resource)) {
                    errors.add("Pass '" + pass.id() + "' reads resource '" + resource
                            + "' with no declared producer or external input.");
                }
            }
        }
        if (!errors.isEmpty()) {
            return new Validation(false, errors, List.of());
        }

        Map<String, Set<String>> predecessors = new LinkedHashMap<>();
        Map<String, Set<String>> successors = new LinkedHashMap<>();
        for (String id : passes.keySet()) {
            predecessors.put(id, new LinkedHashSet<>());
            successors.put(id, new LinkedHashSet<>());
        }
        for (Pass pass : passes.values()) {
            for (String dependency : pass.dependsOn()) {
                addEdge(dependency, pass.id(), predecessors, successors);
            }
            for (String resource : pass.reads()) {
                String producer = writers.get(resource);
                if (producer != null && !producer.equals(pass.id())) {
                    addEdge(producer, pass.id(), predecessors, successors);
                }
            }
        }

        ArrayDeque<String> ready = new ArrayDeque<>();
        for (Map.Entry<String, Set<String>> entry : predecessors.entrySet()) {
            if (entry.getValue().isEmpty()) ready.addLast(entry.getKey());
        }
        List<String> order = new ArrayList<>(passes.size());
        while (!ready.isEmpty()) {
            String id = ready.removeFirst();
            order.add(id);
            for (String next : successors.get(id)) {
                Set<String> remaining = predecessors.get(next);
                remaining.remove(id);
                if (remaining.isEmpty()) ready.addLast(next);
            }
        }
        if (order.size() != passes.size()) {
            Set<String> cyclic = new LinkedHashSet<>(passes.keySet());
            cyclic.removeAll(order);
            errors.add("Render-pass dependency cycle detected: " + cyclic);
            return new Validation(false, errors, List.of());
        }
        return new Validation(true, List.of(), order);
    }

    private static void addEdge(String from, String to,
            Map<String, Set<String>> predecessors, Map<String, Set<String>> successors) {
        if (from.equals(to)) {
            predecessors.get(to).add(from);
            successors.get(from).add(to);
            return;
        }
        predecessors.get(to).add(from);
        successors.get(from).add(to);
    }

    private static Set<String> immutableIdentifiers(Set<String> values, String label) {
        Objects.requireNonNull(values, label + "s");
        Set<String> copy = new LinkedHashSet<>();
        for (String value : values) copy.add(requireIdentifier(value, label));
        return Collections.unmodifiableSet(copy);
    }

    private static String requireIdentifier(String value, String label) {
        Objects.requireNonNull(value, label);
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 128
                || !trimmed.matches("[A-Za-z0-9_.:/-]+")) {
            throw new IllegalArgumentException("Invalid " + label + ": " + value);
        }
        return trimmed;
    }

    public static final class Builder {
        private final Map<String, Pass> passes = new LinkedHashMap<>();
        private final Set<String> externalResources = new LinkedHashSet<>();

        public Builder externalResource(String resource) {
            externalResources.add(requireIdentifier(resource, "external resource"));
            return this;
        }

        public Builder addPass(Pass pass) {
            Objects.requireNonNull(pass, "pass");
            if (passes.putIfAbsent(pass.id(), pass) != null) {
                throw new IllegalArgumentException("Duplicate render pass id: " + pass.id());
            }
            return this;
        }

        public Builder addPass(String id, Set<String> reads, Set<String> writes,
                Set<String> dependsOn, DepthMode depthMode) {
            return addPass(new Pass(id, reads, writes, dependsOn, depthMode));
        }

        public ShaderRenderGraph build() {
            return new ShaderRenderGraph(passes, externalResources);
        }
    }
}
