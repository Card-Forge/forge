package forge.game.mulligan;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility; // Keep this import for the final GameAction call
import forge.game.zone.ZoneType;
import java.util.List;
import com.google.common.collect.Lists;

public class HoustonMulligan extends AbstractMulligan {

    private static final int STARTING_DRAW_SIZE = 10;
    private static final int TUCK_COUNT = 3;

    public HoustonMulligan(Player p, boolean firstMullFree) {
        super(p, false);
    }

    // IMPORTANT: The initial draw of 10 cards happens before this service runs (due to Match.java fix).
    // The player starts the service with 10 cards.

    @Override
    public int handSizeAfterNextMulligan() {
        // This is only used for a *redrawing* mulligan. Since Houston forces a keep,
        // this is largely irrelevant, but we return 10 to be safe.
        return STARTING_DRAW_SIZE;
    }

    @Override
    public void keep() {
        // 🚨 FIX: If the ritual hasn't been performed yet, call the mulligan() method
        // to execute the draw/tuck ritual, then mark as kept.
        if (timesMulliganed == 0) {
            // Since the mulligan() method contains the full tuck logic (draws 10, tucks 3, and sets timesMulliganed=1/kept=true),
            // calling it here executes the ritual.
            mulligan();

            // Exit, as mulligan() already sets kept=true and timesMulliganed=1
            return;
        }

        // For subsequent calls (after the ritual is complete), just mark as kept.
        kept = true;
        // Set to 1 to prevent further calls to mulligan() if it didn't run earlier
        timesMulliganed = 1;
    }

    // 🚨 Critical Override: This executes the draw, tuck, and finalizes the hand size.
    @Override
    public void mulligan() {
        // 1. Mark that the ritual has been performed. This is crucial.
        timesMulliganed++;

        // 2. The Houston "ritual" is a forced keep after a forced tuck of 3.

        // The game should have already drawn 10 cards. We just need to ensure the tuck happens.
        // If, for some reason, the game engine shuffles the hand back before calling mulliganDraw/mulligan,
        // we might need to include a drawCards(10) here. Assuming the 10 cards are currently in hand.

        // 3. Force the tuck using the known working method signature.
        // This method handles the UI prompt internally, asking the player to select TUCK_COUNT (3) cards.
        List<Card> cardsToTuckDown = Lists.newArrayList(
                player.getController().londonMulliganReturnCards(player, TUCK_COUNT)
        );

        // 4. Execute the tuck action. This uses the simple 3-argument signature
        // that accepts null for SpellAbility, which previously caused no crash.
        for (final Card c : cardsToTuckDown) {
            player.getGame().getAction().moveToLibrary(
                    c,              // Card to move
                    -1,             // Position: -1 (bottom of library)
                    (SpellAbility)null // SpellAbility: Null, should be safe in this context
            );
        }

        // 5. Finalize the state: Mark the hand as kept, as the ritual is complete.
        kept = true;
    }

    @Override
    public boolean canMulligan() {
        // Only allow the ritual to run once.
        return !kept && timesMulliganed == 0;
    }

    @Override
    public int tuckCardsAfterKeepHand() {
        // This is no longer necessary, as the tuck happens inside mulligan(), but
        // we can set it to 0 or TUCK_COUNT. Let's return 0 to simplify the engine's flow.
        return 0;
    }

    // Ensure mulliganDraw is not overridden unless necessary, as mulligan() handles the whole ritual.
    // If you HAD an override for mulliganDraw, remove it.
}