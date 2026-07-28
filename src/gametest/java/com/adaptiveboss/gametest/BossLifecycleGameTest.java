package com.adaptiveboss.gametest;

import com.adaptiveboss.AdaptiveBossMod;
import com.adaptiveboss.goal.AdaptiveGoalMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.jetbrains.annotations.NotNull;

/**
 * In-game tests for boss lifecycle behaviour: goal injection and tracker registration.
 *
 * <p>Each test uses the {@code empty_platform} structure (9x9x9 all-air) so that no
 * terrain geometry interferes with entity spawning or AI.
 */
@GameTestHolder("adaptiveboss")
public final class BossLifecycleGameTest {

    /** Spawn position inside the 9x9x9 structure, centred at ground level. */
    private static final BlockPos SPAWN_POS = new BlockPos(4, 2, 4);

    private BossLifecycleGameTest() { }

    /**
     * Verifies that spawning a Wither causes the adaptation engine to inject an
     * {@link AdaptiveGoalMarker} goal into its goal selector.
     *
     * @param helper the game-test helper providing world access and assertions
     */
    @GameTest(template = "empty_platform", timeoutTicks = 40)
    public static void witherSpawnAddsAdaptiveGoal(@NotNull final GameTestHelper helper) {
        final WitherBoss wither = helper.spawn(EntityType.WITHER, SPAWN_POS);
        final boolean hasMarker = wither.goalSelector.getAvailableGoals()
                .stream()
                .anyMatch(wrappedGoal -> wrappedGoal.getGoal() instanceof AdaptiveGoalMarker);
        if (hasMarker) {
            helper.succeed();
        } else {
            helper.fail("Wither goal selector does not contain an AdaptiveGoalMarker goal");
        }
    }

    /**
     * Verifies that spawning a Wither causes the combat tracker to register an
     * adaptation profile for that entity within one tick.
     *
     * @param helper the game-test helper providing world access and assertions
     */
    @GameTest(template = "empty_platform", timeoutTicks = 40)
    public static void trackerRegistersWitherOnSpawn(@NotNull final GameTestHelper helper) {
        final WitherBoss wither = helper.spawn(EntityType.WITHER, SPAWN_POS);
        helper.runAfterDelay(1, () -> {
            final var profile = AdaptiveBossMod.getTracker()
                    .getAdaptationProfile(wither.getUUID());
            if (profile != null) {
                helper.succeed();
            } else {
                helper.fail("BossCombatTracker has no adaptation profile for the spawned Wither");
            }
        });
    }

    /**
     * Verifies that spawning an Ender Dragon causes the adaptation engine to inject an
     * {@link AdaptiveGoalMarker} goal into its goal selector.
     *
     * @param helper the game-test helper providing world access and assertions
     */
    @GameTest(template = "empty_platform", timeoutTicks = 40)
    public static void dragonSpawnAddsAdaptiveGoal(@NotNull final GameTestHelper helper) {
        final EnderDragon dragon = (EnderDragon) helper.spawn(EntityType.ENDER_DRAGON, SPAWN_POS);
        final boolean hasMarker = dragon.goalSelector.getAvailableGoals()
                .stream()
                .anyMatch(wrappedGoal -> wrappedGoal.getGoal() instanceof AdaptiveGoalMarker);
        if (hasMarker) {
            helper.succeed();
        } else {
            helper.fail("EnderDragon goal selector does not contain an AdaptiveGoalMarker goal");
        }
    }

    /**
     * Verifies that spawning an Ender Dragon causes the combat tracker to register an
     * adaptation profile for that entity within one tick.
     *
     * @param helper the game-test helper providing world access and assertions
     */
    @GameTest(template = "empty_platform", timeoutTicks = 40)
    public static void trackerRegistersDragonOnSpawn(@NotNull final GameTestHelper helper) {
        final EnderDragon dragon = (EnderDragon) helper.spawn(EntityType.ENDER_DRAGON, SPAWN_POS);
        helper.runAfterDelay(1, () -> {
            final var profile = AdaptiveBossMod.getTracker()
                    .getAdaptationProfile(dragon.getUUID());
            if (profile != null) {
                helper.succeed();
            } else {
                helper.fail("BossCombatTracker has no adaptation profile for the spawned EnderDragon");
            }
        });
    }
}
