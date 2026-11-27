package dev.opticore.client.shader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure, deterministic planning for bounded render-target cache eviction. */
public final class RenderTargetEvictionPlanner {
    public record Target(String key, long pixels, boolean bound) {
        public Target {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("Target key must not be blank");
            if (pixels < 1) throw new IllegalArgumentException("Target pixel count must be positive");
        }
    }

    public record Plan(boolean feasible, List<String> evictionKeys,
                       int remainingCount, long remainingPixels) {
        public Plan {
            evictionKeys = List.copyOf(evictionKeys);
        }
    }

    private RenderTargetEvictionPlanner() {}

    /**
     * Plans least-recently-used evictions in input order without mutating the
     * cache. Bound targets are never candidates. An infeasible plan is atomic:
     * callers can reject the new allocation without evicting existing entries.
     */
    public static Plan plan(List<Target> targets, long requestedPixels,
                            int maxTargets, long maxPixels) {
        Objects.requireNonNull(targets, "targets");
        if (requestedPixels < 1 || maxTargets < 1 || maxPixels < 1) {
            throw new IllegalArgumentException("Budgets and requested pixels must be positive");
        }
        int count = targets.size() + 1;
        long pixels = requestedPixels;
        for (Target target : targets) {
            Objects.requireNonNull(target, "target");
            pixels = Math.addExact(pixels, target.pixels());
        }

        List<String> evictions = new ArrayList<>();
        for (Target target : targets) {
            if (count <= maxTargets && pixels <= maxPixels) break;
            if (target.bound()) continue;
            evictions.add(target.key());
            count--;
            pixels -= target.pixels();
        }
        return new Plan(count <= maxTargets && pixels <= maxPixels,
                evictions, count, pixels);
    }
}
