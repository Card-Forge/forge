package forge.web.deck;

import forge.deck.DeckFormat;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.util.Localizer;

/** What a deck is checked against: a format, plus an optional card pool for Constructed. Unrestricted checks only Constructed's copy limit, and only when deck legality is enforced. */
public record LegalityScope(GameType format, GameFormat pool, boolean unrestricted) {
    public static LegalityScope of(final GameType format, final GameFormat pool) {
        return new LegalityScope(format, pool, false);
    }

    static LegalityScope none() {
        return new LegalityScope(GameType.Constructed, null, true);
    }

    DeckFormat deckFormat() {
        return unrestricted ? DeckFormat.Constructed : format.getDeckFormat();
    }

    String label() {
        if (unrestricted) {
            return Localizer.getInstance().getMessage("lblWebEditorNoRestriction");
        }
        return pool == null ? format.toString() : format + " · " + pool.getName();
    }
}
