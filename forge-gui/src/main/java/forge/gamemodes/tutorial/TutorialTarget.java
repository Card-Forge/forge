package forge.gamemodes.tutorial;

/**
 * A part of the match screen that the "How to Play" tutorial can point at while it explains it. Each GUI decides where
 * the part is on its own screen (see {@code IGuiBase#highlightGui}); a GUI that has no such part simply shows nothing.
 */
public enum TutorialTarget {
    /** The player's own avatar and life total. */
    YOUR_PLAYER,
    /** The opponent's avatar and life total. */
    OPPONENT,
    /** The player's hand. */
    HAND,
    /** The player's side of the battlefield. */
    BATTLEFIELD,
    /** The player's zone tabs or counters: library, graveyard, exile and so on, plus the mana pool. */
    ZONES,
    /** The stack display. */
    STACK,
    /** The phase indicators, where phase stops are turned on and off. */
    PHASES,
    /** The prompt message that says what the game is waiting for. */
    PROMPT,
    /** The OK / Cancel buttons (and undo where there is one). */
    BUTTONS,
    /** The game menu, log and similar controls. */
    MENU,
    /** The panel that shows a large picture or the text of the card under the pointer. */
    CARD_INFO
}
