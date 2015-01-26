package forge.game.staticability;

import com.google.common.collect.ImmutableList;

public enum StaticAbilityLayer {

    /** Layer 2 for control-changing effects. */
    CONTROL,

    /** Layer 3 for text-changing effects. */
    TEXT,

    /** Layer 4 for type-changing effects. */
    TYPE,

    /** Layer 5 for color-changing effects. */
    COLOR,

    /** Layer 6 for ability-removing and -copying effects. */
    ABILITIES1,

    /** Layer 6 for ability-granting effects. */
    ABILITIES2,

    /** Layer 7a for characteristic-defining power/toughness effects. */
    CHARACTERISTIC,

    /** Layer 7b for power- and/or toughness-setting effects. */
    SETPT,

    /** Layer 7c for power- and/or toughness-modifying effects. */
    MODIFYPT,

    /** Layer for game rule-changing effects. */
    RULES;

    public final static ImmutableList<StaticAbilityLayer> CONTINUOUS_LAYERS =
            ImmutableList.of(CONTROL, TEXT, TYPE, COLOR, ABILITIES1, ABILITIES2, CHARACTERISTIC, SETPT, MODIFYPT, RULES);
}
