package com.adaptiveboss.gametest;

import com.adaptiveboss.AdaptiveBossMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all Adaptive Boss game-test classes with NeoForge's test runner.
 *
 * <p>Wired via the mod event bus so tests are picked up by
 * {@code ./gradlew runGameTestServer}.
 */
@EventBusSubscriber(modid = AdaptiveBossMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class AdaptiveBossGameTests {

    private AdaptiveBossGameTests() { }

    /**
     * Registers game-test holder classes.
     *
     * @param event the registration event
     */
    @SubscribeEvent
    public static void register(@NotNull final RegisterGameTestsEvent event) {
        event.register(BossLifecycleGameTest.class);
        event.register(AdaptationLoopGameTest.class);
    }
}
