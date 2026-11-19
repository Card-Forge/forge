package forge.ai;

import com.google.common.collect.Multiset;

import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;

/**
 * When bouncing a permanent of the opponent is worth a card. Returning a creature to the hand is tempo, not card
 * advantage, and it is a gift for a creature that does something when it enters the battlefield (the opponent casts it
 * again and gets the trigger twice: Vendilion Clique, Mulldrifter, Flickerwisp) or that costs next to nothing to cast again.
 * A flash creature without such a trigger is judged by its cost like any other.
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

    /** The opponent loses something by the bounce besides the mana of casting it again: a token, counters that matter, an aura of theirs. */
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
            // +1/+1 counters and counters that are a keyword (flying, trample) are built value; charge, time, fade and the like
            // run down on their own or only count for what a card does while it stays
            if (e.getCount() > 0 && (e.getElement().is(CounterEnumType.P1P1) || e.getElement().isKeywordCounter())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The attacker is why the AI is in trouble: it is unblocked and, without its damage, the AI would not be in danger
     * (or would not die). A small attacker beside the one that is lethal does not count.
     */
    private static boolean savesTheCombat(final Player ai, final Card c) {
        final Combat combat = ai.getGame().getPhaseHandler().getCombat();
        if (combat == null || !combat.isAttacking(c) || !c.getController().isOpponentOf(ai)) {
            return false;
        }
        int power = c.getNetCombatDamage();
        if (combat.isBlocked(c)) {
            // a blocked trampler still hits the AI with what its blockers cannot soak up; any other blocked attacker is held
            if (!c.hasKeyword(Keyword.TRAMPLE) || c.hasKeyword(Keyword.INFECT)) {
                return false;
            }
            power -= ComputerUtilCombat.totalShieldDamage(c, combat.getBlockers(c));
        }
        final int dmg = Math.max(0, ComputerUtilCombat.predictDamageTo(ai, Math.max(0, power), c, true));
        if (dmg == 0) {
            return false;
        }
        final int remaining = ComputerUtilCombat.lifeThatWouldRemain(ai, combat);
        if (remaining < 1 && remaining + dmg >= 1) {
            return true; // without it the AI lives
        }
        return ComputerUtilCombat.lifeInDanger(ai, combat) && !ComputerUtilCombat.lifeInDanger(ai, combat, -dmg);
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
        if (entersWithValue(c)) {
            return false; // they play it again for a second trigger (a flash creature that has one: at the end of the turn)
        }
        if (c.isCreature() && c.getCMC() <= 1) {
            return false; // the mana to cast it again is a rounding error
        }
        return true; // a plain body that costs real mana to cast again: tempo
    }
}
