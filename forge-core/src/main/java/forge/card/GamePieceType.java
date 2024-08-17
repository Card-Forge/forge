package forge.card;

/**
 * Defines what kind of card physically represents a game object.
 * Influences what zones the object is allowed to exist in, and
 * what card back it should have.
 */
public enum GamePieceType {
    /**
     * A traditional card with traditional rules.
     */
    CARD,
    /**
     * A token that ceases to exist outside the battlefield.
     */
    TOKEN,
    /**
     * Intangible object that exists in the command zone.
     * Includes emblems and boons.
     */
    EFFECT,
    /**
     * A copy of a spell that exists only on the stack, and becomes
     * a token if it would enter the battlefield when it resolves.
     */
    COPIED_SPELL,
    /**
     * An attraction, which starts the game in the attraction deck
     * and goes to the junkyard when it leaves play.
     */
    ATTRACTION,
    /**
     * A contraption, which starts the game in the contraption deck
     * and goes to the scrapyard when it leaves play.
     */
    CONTRAPTION,
    /**
     * A planechase plane or phenomenon, which exists in the command zone
     * or in the planar deck (which is technically in the command zone).
     */
    PLANAR,
    /**
     * A scheme card, confined to either the command zone or the archenemy's
     * scheme deck.
     */
    SCHEME,
    /**
     * A Vanguard Avatar, which starts in the command zone and never leaves.
     */
    AVATAR,
    /**
     * A Dungeon, which is created in the command zone by effects,
     * and leaves the game when completed.
     */
    DUNGEON
}
