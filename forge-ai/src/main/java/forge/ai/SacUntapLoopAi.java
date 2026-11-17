package forge.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.token.TokenInfo;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostSacrifice;
import forge.game.cost.CostTap;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * sacrifice-untap loop (Blasting Station + Pitiless Plunderer + Stimulus Package).
 * An outlet "{T}, Sacrifice a creature: N damage to any target" that untaps whenever a creature enters, plus a refund
 * "whenever a creature you control dies, create a token" whose token is a creature, or is turned into a creature token by a
 * free ability "Sacrifice a <token>: create a creature token", is endless damage without mana. Forge's AI never sacrificed
 * a creature for 1 damage (and Blasting Station is AI:RemoveDeck:All): it attacked into walls. The AI now runs the loop at
 * the weakest opponent's face, and feeds it with the converter while the outlet waits tapped. Generic: any outlet, refund
 * and converter of that shape, read from the scripts.
 */
public final class SacUntapLoopAi {
    private SacUntapLoopAi() { }

    /** At most this many outlet activations per turn: the loop is endless, the game must stay finite. */
    static final int MAX_PER_TURN = 120;

    private static final Map<Game, Map<Integer, int[]>> COUNT = new WeakHashMap<>();

    /** Sacrifice part of an outlet "{T}, Sacrifice a creature (not this)", no mana, or null. */
    static CostSacrifice outletSac(final SpellAbility sa) {
        if (sa == null || !sa.isActivatedAbility() || sa.getApi() != ApiType.DealDamage || !sa.usesTargeting()
                || !sa.getTargetRestrictions().canTgtPlayer()) {
            return null;
        }
        final Cost cost = sa.getPayCosts();
        if (cost == null) {
            return null;
        }
        boolean tap = false;
        CostSacrifice sac = null;
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostTap) {
                tap = true;
            } else if (part instanceof CostSacrifice cs && !cs.payCostFromSource() && "1".equals(cs.getAmount())
                    && cs.getType().startsWith("Creature")) {
                sac = cs;
            } else {
                return null;
            }
        }
        return tap ? sac : null;
    }

    /** The card untaps itself whenever a creature enters (Blasting Station). */
    static boolean untapsOnCreatureEnter(final Card c) {
        for (final Trigger t : c.getTriggers()) {
            if (t.getMode() != TriggerType.ChangesZone || !"Battlefield".equals(t.getParam("Destination"))) {
                continue;
            }
            final String valid = t.getParam("ValidCard");
            if (valid == null || !valid.startsWith("Creature")) {
                continue;
            }
            final SpellAbility ab = t.ensureAbility();
            if (ab != null && ab.getApi() == ApiType.Untap && "Self".equals(ab.getParam("Defined"))) {
                return true;
            }
        }
        return false;
    }

    /** Creature token of a free "Sacrifice a <type>: create a token" ability, or null. */
    static Card converterToken(final Player ai, final SpellAbility sa, final Card fed) {
        if (sa == null || !sa.isActivatedAbility() || sa.getApi() != ApiType.Token || !sa.hasParam("TokenScript")
                || sa.usesTargeting()) {
            return null;
        }
        final Cost cost = sa.getPayCosts();
        if (cost == null || cost.getCostParts().size() != 1
                || !(cost.getCostParts().get(0) instanceof CostSacrifice cs) || cs.payCostFromSource()
                || !"1".equals(cs.getAmount())) {
            return null;
        }
        if (fed != null && !fed.isValid(cs.getType().split(","), ai, sa.getHostCard(), sa)) {
            return null;
        }
        final Card proto = TokenInfo.getProtoType(sa.getParam("TokenScript"), sa, ai);
        return proto != null && proto.isCreature() ? proto : null;
    }

    /** True when the AI controls a death refund that brings a creature back (directly or through a converter). */
    static Card refundCard(final Player ai) {
        for (final Card r : ai.getCardsIn(ZoneType.Battlefield)) {
            for (final Trigger t : r.getTriggers()) {
                if (t.getMode() != TriggerType.ChangesZone || !"Battlefield".equals(t.getParam("Origin"))
                        || !"Graveyard".equals(t.getParam("Destination"))) {
                    continue;
                }
                final String valid = t.getParam("ValidCard");
                if (valid == null || !valid.startsWith("Creature") || !valid.contains("YouCtrl")) {
                    continue;
                }
                final SpellAbility ab = t.ensureAbility();
                if (ab == null || ab.getApi() != ApiType.Token || !ab.hasParam("TokenScript")) {
                    continue;
                }
                final Card proto = TokenInfo.getProtoType(ab.getParam("TokenScript"), ab, ai);
                if (proto == null) {
                    continue;
                }
                if (proto.isCreature() || converterFor(ai, proto) != null) {
                    return r;
                }
            }
        }
        return null;
    }

    static SpellAbility converterFor(final Player ai, final Card fed) {
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            for (final SpellAbility sa : c.getSpellAbilities()) {
                if (converterToken(ai, sa, fed) != null) {
                    return sa;
                }
            }
        }
        return null;
    }

    /** A creature the outlet may sacrifice that is neither the outlet nor the refund. */
    static boolean hasFodder(final Player ai, final CostSacrifice sac, final Card outlet, final Card refund, final SpellAbility sa) {
        for (final Card c : ai.getCreaturesInPlay()) {
            if (c != outlet && c != refund && c.isValid(sac.getType().split(","), ai, outlet, sa)) {
                return true;
            }
        }
        return false;
    }

    static boolean loopReady(final Player ai, final SpellAbility outletSa) {
        final CostSacrifice sac = outletSac(outletSa);
        if (sac == null) {
            return false;
        }
        final Card host = outletSa.getHostCard();
        return host != null && host.getController() == ai && untapsOnCreatureEnter(host) && refundCard(ai) != null;
    }

    private static boolean underCap(final Player ai, final boolean count) {
        final int turn = ai.getGame().getPhaseHandler().getTurn();
        synchronized (COUNT) {
            final Map<Integer, int[]> byPlayer = COUNT.computeIfAbsent(ai.getGame(), g -> new HashMap<>());
            int[] n = byPlayer.get(ai.getId());
            if (n == null || n[0] != turn) {
                n = new int[] {turn, 0};
                byPlayer.put(ai.getId(), n);
            }
            if (n[1] >= MAX_PER_TURN) {
                return false;
            }
            if (count) {
                n[1]++;
            }
            return true;
        }
    }

    /** Activate now: the outlet at the weakest opponent's face, or the converter while an outlet of the loop waits tapped. */
    public static boolean shouldLoop(final Player ai, final SpellAbility sa) {
        if (sa == null || !ai.getGame().getStack().isEmpty() || !sa.isActivatedAbility()) {
            return false;
        }
        if (outletSac(sa) != null) {
            if (!loopReady(ai, sa) || !hasFodder(ai, outletSac(sa), sa.getHostCard(), refundCard(ai), sa)) {
                return false;
            }
            Player target = null;
            for (final Player opp : ai.getOpponents()) {
                if (sa.canTarget(opp) && (target == null || opp.getLife() < target.getLife())) {
                    target = opp;
                }
            }
            if (target == null || !underCap(ai, true)) {
                return false;
            }
            sa.resetTargets();
            sa.getTargets().add(target);
            return true;
        }
        if (converterToken(ai, sa, null) == null || refundCard(ai) == null || !underCap(ai, false)) {
            return false;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isTapped() || !untapsOnCreatureEnter(c)) {
                continue;
            }
            for (final SpellAbility o : c.getSpellAbilities()) {
                if (loopReady(ai, o)) {
                    return true;
                }
            }
        }
        return false;
    }
}
