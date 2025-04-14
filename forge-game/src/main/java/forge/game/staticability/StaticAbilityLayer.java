package forge.game.staticability;

import com.google.common.collect.ImmutableList;

public enum StaticAbilityLayer {
    /** Layer 1 for copiable values. */
    COPY("1"),

    /** Layer 2 for control-changing effects. */
    CONTROL("2"),

    /** Layer 3 for text-changing effects. */
    TEXT("3"),

    /** Layer 4 for type-changing effects. */
    TYPE("4"),

    /** Layer 5 for color-changing effects. */
    COLOR("5"),

    /** Layer 6 for ability effects. */
    ABILITIES("6"),

    /** Layer 7a for characteristic-defining power/toughness effects. */
    CHARACTERISTIC("7a"),

    /** Layer 7b for power- and/or toughness-setting effects. */
    SETPT("7b"),

    /** Layer 7c for power- and/or toughness-modifying effects. */
    MODIFYPT("7c"),

    /** Layer 7d for power- and/or toughness-switching effects. */
    //SWITCHPT("7d"),

    /** Layer for game rule-changing effects. */
    RULES("8");

    public final String num;

    StaticAbilityLayer(String n) {
        num = n;
    }

    public final static ImmutableList<StaticAbilityLayer> CONTINUOUS_LAYERS =
            ImmutableList.of(COPY, CONTROL, TEXT, TYPE, COLOR, ABILITIES, CHARACTERISTIC, SETPT, MODIFYPT, RULES);
    public final static ImmutableList<StaticAbilityLayer> CONTINUOUS_LAYERS_WITH_DEPENDENCY =
            ImmutableList.of(COPY, CONTROL, TEXT, TYPE, ABILITIES, CHARACTERISTIC, SETPT);
}
