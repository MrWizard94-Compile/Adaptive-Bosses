package com.adaptiveboss.gametest;

import com.adaptiveboss.AdaptiveBossMod;
import com.adaptiveboss.adaptation.AdaptationEngine;
import com.adaptiveboss.adaptation.AdaptationProfile;
import com.adaptiveboss.adaptation.BossArchetype;
import com.adaptiveboss.goal.AdaptiveControlGoal;
import com.adaptiveboss.goal.KiteBehavior;
import com.adaptiveboss.goal.RushBehavior;
import com.adaptiveboss.tracking.BossCombatTracker;
import com.adaptiveboss.tracking.DamageType;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.jetbrains.annotations.NotNull;

/**
 * End-to-end tests for the full adaptation loop:
 * {@code recordHit → aggregateAndAdapt → archetype change → behavior swap}.
 *
 * <p>These are the only tests that exercise the complete chain from raw combat
 * data through to a live behavior change on a boss entity's goal selector.
 * Each test calls {@link BossCombatTracker#aggregateAndAdapt} directly to
 * decouple from the real-time tick interval, then inspects both the
 * {@link AdaptationProfile} and the {@link AdaptiveControlGoal} on the wither.
 */
@GameTestHolder("adaptiveboss")
public final class AdaptationLoopGameTest {

    /** Spawn position inside the 9×9×9 empty-platform structure. */
    private static final BlockPos SPAWN_POS = new BlockPos(4, 2, 4);

    private AdaptationLoopGameTest() { }

    /**
     * Verifies that sustained melee combat causes the boss to converge on the
     * {@link BossArchetype#KITER} archetype and swap to a {@link KiteBehavior}.
     *
     * <p>The default hysteresis is 2 cycles, so {@code aggregateAndAdapt} is
     * called twice to guarantee commitment.
     *
     * @param helper the game-test helper
     */
    @GameTest(template = "empty_platform", timeoutTicks = 40)
    public static void meleeCombatConvergesToKiter(@NotNull final GameTestHelper helper) {
        final WitherBoss wither = helper.spawn(EntityType.WITHER, SPAWN_POS);

        // Wait one tick so BossLifecycleHandler.EntityJoinLevelEvent has fired
        helper.runAfterDelay(1, () -> {
            final UUID bossId = wither.getUUID();
            final BossCombatTracker tracker = AdaptiveBossMod.getTracker();

            if (!tracker.isTracked(bossId)) {
                helper.fail("Wither should already be tracked after spawning");
                return;
            }

            // Simulate 20 heavy melee hits from one player
            final UUID playerId = UUID.randomUUID();
            final int tick = (int) helper.getLevel().getGameTime();
            for (int i = 0; i < 20; i++) {
                tracker.recordHit(bossId, playerId, DamageType.MELEE, 8.0, 2.0, tick);
            }

            // Two cycles to satisfy default hysteresis of 2
            tracker.aggregateAndAdapt(tick);
            tracker.aggregateAndAdapt(tick);

            final AdaptationProfile profile = tracker.getAdaptationProfile(bossId);
            if (profile == null) {
                helper.fail("No adaptation profile found for the wither");
                return;
            }
            if (profile.getCurrentArchetype() != BossArchetype.KITER) {
                helper.fail("Expected KITER archetype after heavy melee combat, got: "
                        + profile.getCurrentArchetype());
                return;
            }

            final AdaptiveControlGoal goal = AdaptationEngine.findGoalOn(wither);
            if (goal == null) {
                helper.fail("No AdaptiveControlGoal found on the wither");
                return;
            }
            if (!(goal.getCurrentBehavior() instanceof KiteBehavior)) {
                helper.fail("Expected KiteBehavior on the goal, got: "
                        + goal.getCurrentBehavior());
                return;
            }

            helper.succeed();
        });
    }

    /**
     * Verifies that sustained ranged combat causes the boss to converge on the
     * {@link BossArchetype#AGGRESSOR} archetype and swap to a {@link RushBehavior}.
     *
     * @param helper the game-test helper
     */
    @GameTest(template = "empty_platform", timeoutTicks = 40)
    public static void rangedCombatConvergesToAggressor(@NotNull final GameTestHelper helper) {
        final WitherBoss wither = helper.spawn(EntityType.WITHER, SPAWN_POS);

        helper.runAfterDelay(1, () -> {
            final UUID bossId = wither.getUUID();
            final BossCombatTracker tracker = AdaptiveBossMod.getTracker();

            if (!tracker.isTracked(bossId)) {
                helper.fail("Wither should already be tracked after spawning");
                return;
            }

            final UUID playerId = UUID.randomUUID();
            final int tick = (int) helper.getLevel().getGameTime();
            for (int i = 0; i < 20; i++) {
                tracker.recordHit(bossId, playerId, DamageType.RANGED, 8.0, 12.0, tick);
            }

            tracker.aggregateAndAdapt(tick);
            tracker.aggregateAndAdapt(tick);

            final AdaptationProfile profile = tracker.getAdaptationProfile(bossId);
            if (profile == null) {
                helper.fail("No adaptation profile found for the wither");
                return;
            }
            if (profile.getCurrentArchetype() != BossArchetype.AGGRESSOR) {
                helper.fail("Expected AGGRESSOR archetype after heavy ranged combat, got: "
                        + profile.getCurrentArchetype());
                return;
            }

            final AdaptiveControlGoal goal = AdaptationEngine.findGoalOn(wither);
            if (goal == null) {
                helper.fail("No AdaptiveControlGoal found on the wither");
                return;
            }
            if (!(goal.getCurrentBehavior() instanceof RushBehavior)) {
                helper.fail("Expected RushBehavior on the goal, got: "
                        + goal.getCurrentBehavior());
                return;
            }

            helper.succeed();
        });
    }
}
