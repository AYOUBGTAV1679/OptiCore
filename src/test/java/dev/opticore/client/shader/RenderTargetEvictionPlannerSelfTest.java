package dev.opticore.client.shader;

import java.util.List;

public final class RenderTargetEvictionPlannerSelfTest {
    public static void main(String[] args) {
        evictsLeastRecentlyUsedUnboundTargets();
        skipsBoundTargets();
        rejectsImpossiblePlanWithoutEvictions();
        rejectsInvalidBudgets();
        System.out.println("RenderTargetEvictionPlannerSelfTest: OK");
    }

    private static void evictsLeastRecentlyUsedUnboundTargets() {
        var targets = List.of(
                new RenderTargetEvictionPlanner.Target("old", 40, false),
                new RenderTargetEvictionPlanner.Target("newer", 30, false));
        var plan = RenderTargetEvictionPlanner.plan(targets, 50, 2, 100);
        check(plan.feasible(), "a feasible eviction plan should succeed");
        check(plan.evictionKeys().equals(List.of("old")),
                "the least-recently-used target should be evicted first");
        check(plan.remainingCount() == 2 && plan.remainingPixels() == 80,
                "plan should account for the requested target");
    }

    private static void skipsBoundTargets() {
        var targets = List.of(
                new RenderTargetEvictionPlanner.Target("bound", 70, true),
                new RenderTargetEvictionPlanner.Target("free", 40, false));
        var plan = RenderTargetEvictionPlanner.plan(targets, 30, 2, 110);
        check(plan.feasible(), "an unbound target should make the budget feasible");
        check(plan.evictionKeys().equals(List.of("free")),
                "a currently bound target must never be selected for eviction");
    }

    private static void rejectsImpossiblePlanWithoutEvictions() {
        var targets = List.of(
                new RenderTargetEvictionPlanner.Target("bound-a", 70, true),
                new RenderTargetEvictionPlanner.Target("bound-b", 40, true));
        var plan = RenderTargetEvictionPlanner.plan(targets, 50, 2, 100);
        check(!plan.feasible(), "all-bound cache should reject an impossible request");
        check(plan.evictionKeys().isEmpty(),
                "an infeasible plan must not request partial evictions");
    }

    private static void rejectsInvalidBudgets() {
        expectFailure(() -> RenderTargetEvictionPlanner.plan(List.of(), 0, 2, 100));
        expectFailure(() -> RenderTargetEvictionPlanner.plan(List.of(), 10, 0, 100));
        expectFailure(() -> RenderTargetEvictionPlanner.plan(List.of(), 10, 2, 0));
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("invalid budget should fail");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
