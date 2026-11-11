package forge.gamemodes.match;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.spellability.SpellAbility;

/**
 * What the game's next question is about, for a user interface that titles its dialogs with it (see
 * {@link forge.gui.interfaces.IGuiGame#setPromptContext}): the card whose effect asks ({@code source}, may be null), a short
 * heading saying why it is asked when no card does ({@code reason}, may be null, e.g. a discard to hand size) and what
 * answering does to the cards picked ({@code verb}), so the dialog's button can say it ("Discard 2"). Immutable; the
 * controller puts back the one before it when a question is answered, so nested questions keep their own.
 */
public record PromptContext(CardView source, String reason, Verb verb) {
    /** What the cards a dialog picks are for. */
    public enum Verb { CHOOSE, DISCARD, SACRIFICE, EXILE, DESTROY, PUT_ON_BOTTOM, PUT_INTO_GRAVEYARD }

    /** No context: a plain question. */
    public static final PromptContext NONE = new PromptContext(null, null, Verb.CHOOSE);

    public PromptContext {
        verb = verb == null ? Verb.CHOOSE : verb;
    }

    /** The question comes from this ability's card (none if it has no card). */
    public static PromptContext of(final SpellAbility sa) {
        return of(sa, Verb.CHOOSE);
    }

    public static PromptContext of(final SpellAbility sa, final Verb verb) {
        final Card host = sa == null ? null : sa.getHostCard();
        return new PromptContext(host == null ? null : CardView.get(host), null, verb);
    }

    public static PromptContext of(final Card card) {
        return new PromptContext(card == null ? null : CardView.get(card), null, Verb.CHOOSE);
    }

    /** A question no card asks, with a heading saying why. */
    public static PromptContext because(final String reason, final Verb verb) {
        return new PromptContext(null, reason, verb);
    }

    public boolean isNone() {
        return source == null && (reason == null || reason.isEmpty()) && verb == Verb.CHOOSE;
    }
}
