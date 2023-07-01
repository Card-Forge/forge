package forge.gamemodes.quest.data;

/**
 * Used to clarify which subformat a quest is using e.g. Commander.
 * Auth. Imakuni
 */
public enum DeckConstructionRules {
    /**
     * Typically has no effect on Quest gameplay.
     */
    Default,

    /**
     * Commander ruleset. 99 card deck, no copies other than basic lands, commander(s) in Command zone
     */
    Commander
}
