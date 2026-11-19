package forge.ai;

import com.google.common.collect.Multiset;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
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
     * The attacker is a cause of the danger to the AI. Compare the normal combat forecast with the same combat after
     * removing only this attacker. Using the shared forecast keeps double strike, trample, infect, poison, damage
     * replacement/prevention and the other attackers consistent with ComputerUtilCombat.
     */
    static boolean savesTheCombat(final Player ai, final Card c) {
        final Combat combat = ai.getGame().getPhaseHandler().getCombat();
        if (combat == null || !combat.isAttacking(c) || !c.getController().isOpponentOf(ai)) {
            return false;
        }

        final Combat withoutAttacker = withoutAttacker(ai, combat, c);
        if (withoutAttacker == null) {
            return false;
        }

        // Serious danger includes lethal poison as well as lethal life damage. Check it first so the comparison is
        // deterministic for the cases where bouncing the attacker prevents the loss.
        if (ComputerUtilCombat.lifeInSeriousDanger(ai, combat)) {
            return !ComputerUtilCombat.lifeInSeriousDanger(ai, withoutAttacker);
        }
        final int poisonDangerFloor = Math.max(7, ai.getPoisonCounters());
        if (ComputerUtilCombat.resultingPoison(ai, combat) > poisonDangerFloor
                && ComputerUtilCombat.resultingPoison(ai, withoutAttacker) <= poisonDangerFloor) {
            return true;
        }
        if (!ComputerUtilCombat.lifeInDanger(ai, combat)
                || ComputerUtilCombat.resultingPoison(ai, withoutAttacker) > poisonDangerFloor) {
            return false;
        }

        // lifeInDanger randomizes its threshold between these bounds. Requiring the reduced combat to clear the upper
        // bound avoids a second random draw changing the verdict for the same attacker.
        final int safeThreshold = Math.min(Math.max(AiProfileUtil.getIntProperty(ai, AiProps.AI_IN_DANGER_THRESHOLD),
                AiProfileUtil.getIntProperty(ai, AiProps.AI_IN_DANGER_MAX_THRESHOLD)), ai.getLife());
        return ComputerUtilCombat.lifeThatWouldRemain(ai, withoutAttacker) >= safeThreshold;
    }

    /**
     * Make a lightweight combat view containing the attackers that can damage the AI's team, except one card. The
     * actual cards and blockers are reused; Combat does not update the live game view for this separate instance.
     * Return null when the live combat cannot be represented safely, in which case bounce evaluation stays conservative.
     */
    private static Combat withoutAttacker(final Player ai, final Combat combat, final Card excluded) {
        final Combat reduced = new Combat(combat.getAttackingPlayer());
        for (final Card attacker : combat.getAttackers()) {
            if (attacker == excluded) {
                continue;
            }
            final GameEntity defender = combat.getDefenderByAttacker(attacker);
            if (!(defender instanceof Player) || !ai.sharesTeamWith((Player) defender)) {
                continue; // damage to a planeswalker/battle or another player does not change this player's life/poison forecast
            }
            if (!reduced.getDefenders().contains(defender)) {
                return null;
            }
            reduced.addAttacker(attacker, defender);
            if (!reduced.isAttacking(attacker)) {
                return null;
            }
            if (combat.isBlocked(attacker)) {
                reduced.setBlocked(attacker, true);
            }
            for (final Card blocker : combat.getBlockers(attacker)) {
                reduced.addBlocker(attacker, blocker);
            }
        }
        return reduced;
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
