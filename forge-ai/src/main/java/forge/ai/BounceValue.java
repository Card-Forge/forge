package forge.ai;

import com.google.common.collect.Multiset;

import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;

/**
 * When bouncing a permanent of the opponent is worth a card. Returning a creature to the hand is tempo, not card
 * advantage, and it is a gift for a creature that does something when it enters the battlefield (the opponent casts it
 * again and gets the trigger twice: Vendilion Clique, Mulldrifter, Flickerwisp), that has flash (it comes back at the end
 * of the turn for the mana of the turn they lose nothing), or that costs next to nothing to cast again.
 */
public final class BounceValue {
    private BounceValue() {
    }

    /** The permanent does something when it enters the battlefield, or can be cast at instant speed again. */
    public static boolean entersWithValue(final Card c) {
        for (final Trigger t : c.getTriggers()) {
            if (t.getMode() == TriggerType.ChangesZone && "Battlefield".equals(t.getParam("Destination"))
                    && t.getParamOrDefault("ValidCard", "").contains("Self")) {
                return true;
            }
        }
        for (final ReplacementEffect re : c.getReplacementEffects()) {
            // enters as a copy, with counters, with a choice made: not the tapped drawback of a land
            if ("Moved".equals(re.getParam("Event")) && "Battlefield".equals(re.getParam("Destination"))
                    && re.getParamOrDefault("ValidCard", "").contains("Self")
                    && !"ETBTapped".equals(re.getParam("ReplaceWith"))) {
                return true;
            }
        }
        return false;
    }

    /** The opponent loses something by the bounce besides the mana of casting it again: a token, counters, an aura of theirs. */
    public static boolean losesSomething(final Player ai, final Card c) {
        if (c.isToken()) {
            return true; // ceases to exist
        }
        for (final Card attached : c.getEnchantedBy()) {
            if (attached.getController().isOpponentOf(ai)) {
                return true; // their aura goes to the graveyard
            }
        }
        for (final Multiset.Entry<CounterType> e : c.getCounters().entrySet()) {
            if (e.getCount() > 0 && !ComputerUtil.isNegativeCounter(e.getElement(), c)) {
                return true; // the counters go
            }
        }
        return false;
    }

    /** A combat in which the bounce takes an attacker that the AI cannot afford to let through. */
    private static boolean savesTheCombat(final Player ai, final Card c) {
        final Combat combat = ai.getGame().getPhaseHandler().getCombat();
        if (combat == null || !combat.isAttacking(c) || !c.getController().isOpponentOf(ai)) {
            return false;
        }
        return ComputerUtilCombat.lifeInDanger(ai, combat);
    }

    /**
     * @param c a permanent of an opponent of {@code ai}
     * @return whether sending it back to its owner's hand is worth the card (and the mana) that does it
     */
    public static boolean worthBouncing(final Player ai, final Card c) {
        if (c.getController() == null || !c.getController().isOpponentOf(ai)) {
            return true; // the AI's own permanent: judged by the rules for blinking its own stuff
        }
        if (losesSomething(ai, c) || savesTheCombat(ai, c)) {
            return true;
        }
        if (c.isLand() || c.isPlaneswalker()) {
            return true; // a land is a land drop and a turn; a planeswalker loses the loyalty it built
        }
        if (c.hasKeyword(Keyword.FLASH) || entersWithValue(c)) {
            return false; // they play it again for a second trigger, or at the end of the turn
        }
        if (c.isCreature() && c.getCMC() <= 1) {
            return false; // the mana to cast it again is a rounding error
        }
        return true; // a plain body that costs real mana to cast again: tempo
    }
}
