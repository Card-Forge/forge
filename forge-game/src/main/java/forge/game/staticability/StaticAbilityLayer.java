package forge.game.staticability;

import com.google.common.collect.ImmutableList;

public enum StaticAbilityLayer {
    /** Layer 1 for control-changing effects. */
    COPY,

    /** Layer 2 for control-changing effects. */
    CONTROL,

    /** Layer 3 for text-changing effects. */
    TEXT,

    /** Layer 4 for type-changing effects. */
    TYPE,

    /** Layer 5 for color-changing effects. */
    COLOR,

    /** Layer 6 for ability effects. */
    ABILITIES,

    /** Layer 7a for characteristic-defining power/toughness effects. */
    CHARACTERISTIC,

    /** Layer 7b for power- and/or toughness-setting effects. */
    SETPT,

    /** Layer 7c for power- and/or toughness-modifying effects. */
    MODIFYPT,

    /** Layer for game rule-changing effects. */
    RULES;

    public final static ImmutableList<StaticAbilityLayer> CONTINUOUS_LAYERS =
            ImmutableList.of(COPY, CONTROL, TEXT, TYPE, COLOR, ABILITIES, CHARACTERISTIC, SETPT, MODIFYPT, RULES);
}
