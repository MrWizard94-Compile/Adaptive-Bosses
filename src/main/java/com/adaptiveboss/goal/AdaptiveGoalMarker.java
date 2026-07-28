package com.adaptiveboss.goal;

/**
 * Marker interface for all adaptive goals added by the AdaptiveBoss system.
 *
 * <p>Goals that implement this interface can be safely identified on a boss's
 * goal selector for introspection and debugging purposes. The permanent
 * {@link AdaptiveControlGoal} is the only goal that implements this interface
 * in normal operation; it is never removed once added.
 */
public interface AdaptiveGoalMarker {
}
