package forge.ai;

import java.util.Map;

import com.google.common.collect.MapMaker;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.token.TokenInfo;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostSacrifice;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * Sacrifice-for-mana loop (Ashnod's Altar + Funeral Room + Trudge Garden). A free mana outlet "Sacrifice a creature: add N mana", a drain "whenever a creature you control dies, each
 * opponent loses life" and a refund that makes a creature token for at most N generic mana (on the death itself, or on the
 * life the drain gains) is endless drain. Forge's AI never activates a mana ability without a spell to pay: it sat there.
 * The AI now sacrifices while the opponents' life keeps dropping, up to {@link #MAX_PER_TURN} a turn. Read on the scripts'
 * shape, not on card names.
 */
public final class SacManaLoopAi {
    private SacManaLoopAi() { }

    static final int MAX_PER_TURN = 200;

    /** per player: turn, activations this turn, opponents' total life at the last activation, board fingerprint then */
    private static final Map<Player, int[]> STATE = new MapMaker().weakKeys().makeMap(); // identity keys

    /** Mana produced by "Sacrifice a creature (not this): add N", no other cost, or 0. */
    static int outletMana(final SpellAbility sa) {
        if (sa == null || !sa.isManaAbility() || !sa.isActivatedAbility()) {
            return 0;
        }
        final Cost cost = sa.getPayCosts();
        if (cost == null || cost.getCostParts().size() != 1
                || !(cost.getCostParts().get(0) instanceof CostSacrifice cs) || cs.payCostFromSource()
                || !"1".equals(cs.getAmount()) || !cs.getType().startsWith("Creature")) {
            return 0;
        }
        final AbilityManaPart part = sa.getManaPart();
        if (part == null) {
            return 0;
        }
        int n;
        try {
            n = Integer.parseInt(sa.getParamOrDefault("Amount", "1"));
        } catch (NumberFormatException e) {
            return 0;
        }
        return n * Math.max(1, part.getOrigProduced().split(" ").length);
    }

    static boolean diesTrigger(final Trigger t) {
        if (t.getMode() != TriggerType.ChangesZone || !"Battlefield".equals(t.getParam("Origin"))
                || !"Graveyard".equals(t.getParam("Destination"))) {
            return false;
        }
        final String valid = t.getParam("ValidCard");
        return valid != null && valid.startsWith("Creature") && valid.contains("YouCtrl");
    }

    /** 0 = no drain, 1 = drain, 2 = drain that also gains life. */
    static int drain(final Player ai) {
        int best = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            for (final Trigger t : c.getTriggers()) {
                if (!diesTrigger(t) || t.hasParam("OptionalDecider")) {
                    continue;
                }
                SpellAbility ab = t.ensureAbility();
                boolean hurts = false;
                boolean gains = false;
                for (; ab != null; ab = ab.getSubAbility()) {
                    final String def = ab.getParamOrDefault("Defined", "");
                    if ((ab.getApi() == ApiType.LoseLife || ab.getApi() == ApiType.DealDamage) && !ab.usesTargeting()
                            && def.contains("Opponent")) {
                        hurts = true;
                    } else if (ab.getApi() == ApiType.GainLife && !ab.usesTargeting()
                            && (def.isEmpty() || "You".equals(def))) {
                        gains = true;
                    }
                }
                if (hurts) {
                    best = Math.max(best, gains ? 2 : 1);
                }
            }
        }
        return best;
    }

    /** A trigger (death of our creature, or our life gain) that makes a creature token for at most {@code mana} generic. */
    static boolean refund(final Player ai, final int mana, final boolean gains) {
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            for (final Trigger t : c.getTriggers()) {
                final boolean onDeath = diesTrigger(t);
                final boolean onGain = gains && t.getMode() == TriggerType.LifeGained
                        && "You".equals(t.getParamOrDefault("ValidPlayer", ""));
                if (!onDeath && !onGain) {
                    continue;
                }
                final SpellAbility ab = t.ensureAbility();
                if (ab == null || ab.getApi() != ApiType.Token || !ab.hasParam("TokenScript")) {
                    continue;
                }
                final Card proto = TokenInfo.getProtoType(ab.getParam("TokenScript"), ab, ai);
                if (proto == null || !proto.isCreature()) {
                    continue;
                }
                final Cost cost = ab.getPayCosts();
                if (cost == null || cost.hasNoManaCost()) {
                    return true;
                }
                boolean ok = true;
                for (final CostPart p : cost.getCostParts()) {
                    if (!(p instanceof forge.game.cost.CostPartMana pm) || !pm.getMana().isPureGeneric()
                            || pm.getMana().getGenericCost() > mana) {
                        ok = false;
                    }
                }
                if (ok) {
                    return true;
                }
            }
        }
        return false;
    }

    static int oppLife(final Player ai) {
        int life = 0;
        for (final Player o : ai.getOpponents()) {
            life += Math.max(0, o.getLife());
        }
        return life;
    }

    static boolean loopReady(final Player ai, final SpellAbility sa) {
        final int mana = outletMana(sa);
        if (mana == 0 || sa.getHostCard().getController() != ai) {
            return false;
        }
        final Player opp = ai.getWeakestOpponent();
        if (opp == null || !opp.canLoseLife() || opp.cantLoseForZeroOrLessLife()) {
            return false;
        }
        final int d = drain(ai);
        return d > 0 && refund(ai, mana, d == 2);
    }

    /** Called by AiController: sacrifice now, the drain hits, the mana pays the next creature. */
    public static boolean shouldLoop(final Player ai, final SpellAbility sa) {
        if (sa == null || !ai.getGame().getStack().isEmpty() || !ai.getGame().getPhaseHandler().isPlayerTurn(ai)
                || !ai.getGame().getPhaseHandler().getPhase().isMain() || !loopReady(ai, sa)) {
            return false;
        }
        final int turn = ai.getGame().getPhaseHandler().getTurn();
        final int life = oppLife(ai);
        int board = 0; // our creatures' timestamps: the same board is the same decision, judged again
        for (final Card c : ai.getCreaturesInPlay()) {
            board = 31 * board + (int) c.getGameTimestamp();
        }
        int[] st = STATE.get(ai);
        if (st == null || st[0] != turn) {
            st = new int[] {turn, 0, Integer.MAX_VALUE, board};
            STATE.put(ai, st);
        } else if (st[3] == board) {
            return st[1] < MAX_PER_TURN;
        } else if (st[1] >= MAX_PER_TURN || life >= st[2]) {
            return false; // the last sacrifice did not drain: the loop is broken (no refund paid, life gained back)
        }
        st[1]++;
        st[2] = life;
        st[3] = board;
        return true;
    }
}
