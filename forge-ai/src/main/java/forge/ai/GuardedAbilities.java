package forge.ai;

import forge.ai.simulation.DeckPlan;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.cost.Cost;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Abilities whose script says {@code AILogic$ Never}: the heuristic AI never uses them, because the one rule of thumb
 * that fits all of them does not exist. Flat Monte Carlo may still weigh one of them, but only through here:
 * <ul>
 *   <li>{@link #mayConsider}: the ability is one of the kinds that can be judged at all (a counterspell paid with mana
 *       and discards, like Kozilek, the Great Distortion's), and either Flat MC's discovery is on or the card is the
 *       key card of a ramp deck;</li>
 *   <li>{@link #worthIt}: what is countered is worth clearly more than the card that is given up, and the discard can
 *       really be paid: the spell's mana value has to be matched by a card in hand;</li>
 *   <li>{@link #chooseMvDiscard}: which card to give up, the one the AI would miss least. This is also what pays the
 *       cost: "Discard a card with mana value X" used to be left to the generic discard rules, which know nothing of
 *       X.</li>
 * </ul>
 * Values are in the evaluator's points (a 2/2 creature is worth about 150).
 */
public final class GuardedAbilities {
    /** What is countered must be worth this much more than what is discarded. */
    static final int MARGIN = 40;
    /** Extra worth of a card the deck is built around: it is not discarded for a cheap spell. */
    static final int KEY_CARD_BONUS = 150;
    /** A ramp deck's top end is worth at least this much when it sits in hand. */
    static final int RAMP_PAYOFF_MIN_CMC = 6;
    /** What a spell that kills or burns the AI's own board or face is worth on top of its own value. */
    static final int THREAT_BONUS = 90;

    private GuardedAbilities() {
    }

    /** Whether the script keeps the heuristic AI away from this ability. */
    public static boolean isNeverLogic(final SpellAbility sa) {
        return sa != null && "Never".equals(sa.getParam("AILogic"));
    }

    /** The discard part that names a mana value, like {@code Discard<1/Card.cmcEQX>}; null for any other cost. */
    static CostDiscard mvDiscard(final SpellAbility sa) {
        final Cost cost = sa == null ? null : sa.getPayCosts();
        if (cost == null) {
            return null;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostDiscard && !part.payCostFromSource() && part.getType() != null
                    && part.getType().contains(".cmcEQ")) {
                return (CostDiscard) part;
            }
        }
        return null;
    }

    /** Whether the discard cost needs a card of the mana value of what is targeted (so it cannot be paid without a target). */
    public static boolean isMvDiscard(final CostDiscard cost) {
        return cost != null && cost.getType() != null && cost.getType().contains(".cmcEQ");
    }

    /**
     * Whether Flat MC may weigh this {@code AILogic$ Never} ability at all. Only counterspells paid with mana and
     * discards qualify; everything else with that mark stays out. Then either discovery is on, or the card is the key
     * card of a ramp deck (its top end, the commander, or a piece of a combo): the deck was built to have it out.
     */
    public static boolean mayConsider(final Player ai, final SpellAbility sa, final boolean discovery) {
        if (!isNeverLogic(sa)) {
            return true; // not one of these
        }
        if (ai == null || sa.getApi() != ApiType.Counter || sa.isSpell() || !ai.isAI()) {
            return false;
        }
        final Cost cost = sa.getPayCosts();
        if (cost == null || mvDiscard(sa) == null) {
            return false;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (!(part instanceof CostDiscard) && !(part instanceof CostPartMana)) {
                return false; // sacrifice, life, counters: no rule of thumb for what that is worth
            }
        }
        return discovery || isRampKeyCard(ai, sa.getHostCard());
    }

    /** The key card of a ramp deck, or of a combo or a commander plan: the card the deck is built to have in play. */
    static boolean isRampKeyCard(final Player ai, final Card host) {
        if (host == null || !(ai.getController() instanceof PlayerControllerAi)) {
            return false;
        }
        final DeckPlan plan = ((PlayerControllerAi) ai.getController()).getAi().deckPlan(ai);
        return plan.archetype == DeckPlan.Archetype.RAMP && host.getCMC() >= RAMP_PAYOFF_MIN_CMC
                || plan.combos.isKeyCard(host) || plan.commander.isKeyCard(host);
    }

    // ------------------------------------------------------------------------------------------------ the payment

    /**
     * The mana value the discard has to match: the one of the spell that is targeted, or -1 when nothing is targeted
     * (the cost cannot be paid then, and it must not be guessed as 0).
     */
    static int targetedMv(final SpellAbility sa) {
        if (sa == null || sa.getTargets() == null) {
            return -1;
        }
        final SpellAbility spell = sa.getTargets().getFirstTargetedSpell();
        if (spell != null && spell.getHostCard() != null) {
            return spell.getHostCard().getCMC();
        }
        final Card c = sa.getTargetCard();
        return c == null ? -1 : c.getCMC();
    }

    /** The cards in hand that can pay the discard: the cost's own filter, and the mana value of the target if it names none. */
    static CardCollection payers(final Player ai, final SpellAbility sa, final CardCollectionView exclude) {
        final CostDiscard part = mvDiscard(sa);
        final CardCollection hand = new CardCollection(ai.getCardsIn(ZoneType.Hand));
        if (exclude != null) {
            hand.removeAll(exclude);
        }
        if (part == null) {
            return hand;
        }
        CardCollection valid = CardLists.getValidCards(hand, part.getType().split(";"), sa.getActivatingPlayer() != null
                ? sa.getActivatingPlayer() : ai, sa.getHostCard(), sa);
        if (valid.isEmpty()) {
            // "X" could not be read off the ability (not aimed yet, or a copy of it): ask the target directly
            final int mv = targetedMv(sa);
            if (mv >= 0) {
                valid = CardLists.filter(hand, CardPredicates.hasCMC(mv));
            }
        }
        return valid;
    }

    /**
     * Pays "Discard a card with mana value X": the card out of those that fit that the AI would miss least. Null when
     * no card fits, so the payment fails honestly instead of discarding something else.
     */
    public static CardCollection chooseMvDiscard(final Player ai, final SpellAbility sa, final int amount,
                                                 final CardCollectionView alreadyChosen) {
        final CardCollection valid = payers(ai, sa, alreadyChosen);
        if (valid.size() < amount) {
            return null;
        }
        final CardCollection out = new CardCollection();
        final CardCollection left = new CardCollection(valid);
        for (int i = 0; i < amount; i++) {
            Card best = null;
            int bestValue = Integer.MAX_VALUE;
            for (final Card c : left) {
                final int v = handCardValue(ai, c);
                if (v < bestValue) {
                    bestValue = v;
                    best = c;
                }
            }
            out.add(best);
            left.remove(best);
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ the worth

    /**
     * Whether countering {@code top} with this ability is worth what it costs: the best card that can pay the discard
     * must be worth clearly less than the spell. Rollouts decide the rest; this keeps the obviously bad trades (a
     * land and a key card for a vanilla creature) out of the comparison and the obviously impossible ones too.
     */
    public static boolean worthIt(final Player ai, final SpellAbility sa, final SpellAbility top) {
        if (ai == null || sa == null || top == null || top.getHostCard() == null) {
            return false;
        }
        try {
            if (top.getActivatingPlayer() == null || !top.getActivatingPlayer().isOpponentOf(ai)) {
                return false; // never counter the AI's own spell
            }
            final CardCollection valid = payers(ai, sa, null);
            if (valid.isEmpty()) {
                return false; // nothing in hand has the mana value: the cost cannot be paid
            }
            int cheapest = Integer.MAX_VALUE;
            for (final Card c : valid) {
                cheapest = Math.min(cheapest, handCardValue(ai, c));
            }
            return spellWorth(ai, top) >= cheapest + MARGIN;
        } catch (final RuntimeException e) {
            return false; // a refinement only: without it the ability stays out
        }
    }

    /** What the spell would do to the AI if it resolved, in evaluator points. */
    static int spellWorth(final Player ai, final SpellAbility top) {
        final Card host = top.getHostCard();
        int value;
        if (host.isCreature()) {
            value = ComputerUtilCard.evaluateCreature(host);
        } else if (host.isInstant() || host.isSorcery()) {
            value = 40 + 12 * host.getCMC();
        } else {
            value = 50 + 30 * host.getCMC();
        }
        boolean mass = false;
        boolean aimedAtMe = false;
        for (SpellAbility a = top; a != null; a = a.getSubAbility()) {
            final ApiType api = a.getApi();
            mass |= api == ApiType.DestroyAll || api == ApiType.DamageAll || api == ApiType.ChangeZoneAll
                    || api == ApiType.SacrificeAll || api == ApiType.Sacrifice;
            if (a.usesTargeting() && a.getTargets() != null) {
                for (final Card t : a.getTargets().getTargetCards()) {
                    aimedAtMe |= t.getController() == ai;
                }
                for (final Player p : a.getTargets().getTargetPlayers()) {
                    aimedAtMe |= p == ai && (api == ApiType.DealDamage || api == ApiType.LoseLife || api == ApiType.Discard
                            || api == ApiType.Destroy);
                }
            }
        }
        if (aimedAtMe || mass && ai.getCreaturesInPlay().size() >= 2) {
            value += THREAT_BONUS;
        }
        return value;
    }

    /**
     * What a card in hand is worth to the AI, for choosing what to give up: lands by how many more are needed, spells
     * by their size, a lot more for a key card, nothing for a card the script marks as one to throw away.
     */
    static int handCardValue(final Player ai, final Card c) {
        if (c.hasSVar("DiscardMe")) {
            return 0;
        }
        int value;
        if (c.isLand()) {
            int lands = 0;
            for (final Card x : ai.getCardsIn(ZoneType.Battlefield)) {
                lands += x.isLand() ? 1 : 0;
            }
            int inHand = 0;
            for (final Card x : ai.getCardsIn(ZoneType.Hand)) {
                inHand += x.isLand() ? 1 : 0;
            }
            value = lands < 4 ? 120 : lands < 7 ? 50 : 8;
            if (inHand > 1) {
                value /= 2; // another one is waiting
            }
        } else if (c.isCreature()) {
            value = ComputerUtilCard.evaluateCreature(c);
        } else if (c.isInstant() || c.isSorcery()) {
            value = 40 + 12 * c.getCMC();
        } else {
            value = 50 + 30 * c.getCMC();
        }
        if (c.hasSVar("DoNotDiscardIfAble")) {
            value += 100;
        }
        if (isRampKeyCard(ai, c)) {
            value += KEY_CARD_BONUS;
        }
        return value;
    }
}
