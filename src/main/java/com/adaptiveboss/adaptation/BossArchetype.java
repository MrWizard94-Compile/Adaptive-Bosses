package com.adaptiveboss.adaptation;

/** Tactical archetype a boss can adopt based on the party's combat style. */
public enum BossArchetype {

    /**
     * Party is predominantly melee — boss maintains distance and uses AoE attacks
     * to punish close-range fighters.
     */
    KITER,

    /**
     * Party is predominantly ranged — boss closes the gap aggressively and
     * suppresses its own ranged attacks to force melee engagement.
     */
    AGGRESSOR,

    /**
     * Party uses a balanced mix of combat styles — boss uses unpredictable,
     * randomised patterns to prevent the party from adapting back.
     */
    UNPREDICTABLE,

    /**
     * No combat data has been collected yet — boss uses its default vanilla AI
     * unchanged.
     */
    BALANCED
}
