package forge.ai;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostSacrifice;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import forge.ai.AiCardMemory.MemorySet;

/**
 * Mana abilities that sacrifice a permanent as part of their cost: Phyrexian Tower, Ashnod's Altar, Culling Dais,
 * Blasting Station, and the like.
 * <p>
 * The payment code can already use them as a last resort, but it never asked whether the trade is worth it: any spell
 * that could be paid for this way was paid for this way, with whatever creature the generic "worst permanent" rule
 * picked, even for a spell worth far less than the creature. This class answers four questions:
 * <ul>
 *   <li>{@link #worthIt}: is the spell (or ability) that is being paid for worth more than what is sacrificed? Fodder
 *       (a token, a creature that is useless, about to die anyway, or that comes back) is always fine; a real creature
 *       needs a spell that is worth clearly more, or one the deck is built around (a combo piece, the commander).</li>
 *   <li>{@link #chooseFodder}: which permanent to give up, by what it would still be worth to the AI: a token before a
 *       real creature, a creature with undying or persist before one without.</li>
 *   <li>{@link #hasExpendableFodder}: whether the mana of such an ability may be counted on when planning a turn.</li>
 *   <li>{@link #canUseAgainInTest}: whether a test payment may use such an ability again.</li>
 * </ul>
 * Values are in the evaluator's points (a 2/2 creature is worth about 150).
 */
public final class ManaSacrifice {
    /** A sacrifice that costs this little (or less) is fodder: nothing is lost that the AI would miss. */
    static final int FODDER_MAX = 25;
    /** What is paid for must be worth this much more than what is given up. */
    static final int MARGIN = 60;
    /** Extra worth of a card the deck is built around (a combo piece or the commander). */
    static final int KEY_CARD_BONUS = 150;
    /** What an ability (not a spell) that is paid for with a sacrifice counts as: it needs fodder or a combo piece. */
    private static final int ABILITY_VALUE = 40;

    private ManaSacrifice() {
    }

    /** The part of the cost that sacrifices something other than the source, or null if there is none. */
    static CostSacrifice sacrificePart(final SpellAbility ma) {
        if (ma == null || !ma.isManaAbility()) {
            return null;
        }
        final Cost cost = ma.getPayCosts();
        if (cost == null) {
            return null;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice && !part.payCostFromSource() && !"OriginalHost".equals(part.getType())
                    && !"All".equals(part.getAmount())) {
                return (CostSacrifice) part;
            }
        }
        return null;
    }

    /** Whether this is a mana ability that sacrifices a permanent other than its own source. */
    public static boolean isSacrificeMana(final SpellAbility ma) {
        return sacrificePart(ma) != null;
    }

    /**
     * Whether the AI should pay for {@code paidFor} with this mana ability. True for every ability that does not
     * sacrifice something, and for a player that is not an AI (the human pays by hand).
     */
    public static boolean worthIt(final Player ai, final SpellAbility ma, final SpellAbility paidFor) {
        final CostSacrifice part = sacrificePart(ma);
        if (part == null || ai == null || !ai.isAI() || paidFor == null || paidFor.isManaAbility()) {
            return true;
        }
        try {
            final CardCollection given = chosen(ai, part, ma);
            if (given == null || given.isEmpty()) {
                return true; // the payment code will find out that nothing can be sacrificed
            }
            int cost = 0;
            for (final Card c : given) {
                cost += sacrificeValue(ai, c);
            }
            if (cost <= FODDER_MAX * given.size()) {
                return true;
            }
            return worth(ai, paidFor) >= cost + MARGIN;
        } catch (final RuntimeException e) {
            return true; // a refinement only: the old behaviour is the fallback
        }
    }

    /** Whether the mana of this ability can be planned on: something that costs next to nothing can be given up. */
    public static boolean hasExpendableFodder(final Player p, final SpellAbility ma) {
        final CostSacrifice part = sacrificePart(ma);
        if (part == null) {
            return true;
        }
        if (!p.isAI()) {
            return true; // a human decides for themselves
        }
        try {
            final CardCollection given = chosen(p, part, ma);
            if (given == null || given.isEmpty()) {
                return false;
            }
            for (final Card c : given) {
                if (sacrificeValue(p, c) > FODDER_MAX) {
                    return false;
                }
            }
            return true;
        } catch (final RuntimeException e) {
            return true;
        }
    }

    /**
     * Whether a test payment may use this mana ability again. A test sacrifices nothing, so the fodder of each use is
     * set aside in the memory of an AI controller instead, and the ability can be used again while enough fodder is
     * left. An AI's fodder is already set aside by {@link ComputerUtilCost#checkForManaSacrificeCost}.
     */
    public static boolean canUseAgainInTest(final Player p, final SpellAbility ma) {
        final CostSacrifice part = sacrificePart(ma);
        // without an AI controller nothing can be set aside, as when the AI tests an opponent's payment
        if (part == null || ma.getPayCosts().hasTapCost() || !p.getController().isAI()) {
            return false;
        }
        final int amount = part.getAbilityAmount(ma);
        if (amount <= 0) {
            return false;
        }
        final CardCollection fodder = CardLists.getValidCards(p.getCardsIn(ZoneType.Battlefield), part.getType().split(";"), p, ma.getHostCard(), ma);
        fodder.removeIf(c -> !c.canBeSacrificedBy(ma, false) || AiCardMemory.isRememberedCard(p, c, MemorySet.PAYS_SAC_COST));
        if (!p.isAI()) {
            // a human's own sacrifice for mana (Blood Pet) is not set aside, so such a card can't count as fodder too
            fodder.removeIf(c -> c.getManaAbilities().anyMatch(m -> ComputerUtilCost.isSacrificeSelfCost(m.getPayCosts())));
            for (int i = 0; i < amount && !fodder.isEmpty(); i++) {
                AiCardMemory.rememberCard(p, fodder.remove(0), MemorySet.PAYS_SAC_COST);
            }
        }
        return fodder.size() >= amount;
    }

    private static CardCollection chosen(final Player ai, final CostSacrifice part, final SpellAbility ma) {
        final CardCollection exclude = new CardCollection();
        if (AiCardMemory.getMemorySet(ai, MemorySet.PAYS_SAC_COST) != null) {
            exclude.addAll(AiCardMemory.getMemorySet(ai, MemorySet.PAYS_SAC_COST));
        }
        return ComputerUtil.chooseSacrificeType(ai, part.getType(), ma, ma.getTargetCard(), false,
                part.getAbilityAmount(ma), exclude);
    }

    /**
     * What the AI still gets out of this permanent, in evaluator points; the lower, the better to sacrifice. A token
     * is worth half, a creature that comes back (undying, persist) a quarter, and a creature that is useless, marked
     * as sacrifice fodder by its script, or about to be lost anyway nothing. A creature that cannot attack yet is worth
     * a little less than the same creature that can.
     */
    static int sacrificeValue(final Player ai, final Card c) {
        if (c.hasSVar("SacMe") || c.isCreature() && ComputerUtilCard.isUselessCreature(ai, c)) {
            return 0;
        }
        double value;
        if (c.isCreature()) {
            value = ComputerUtilCard.evaluateCreature(c);
            if (ComputerUtilCard.hasActiveUndyingOrPersist(c)) {
                value *= 0.25;
            }
            if (c.isSick() && !c.hasKeyword(forge.game.keyword.Keyword.HASTE)) {
                value *= 0.8;
            }
        } else {
            value = 40 + 25 * c.getCMC();
        }
        if (c.isToken()) {
            value *= 0.5;
        }
        return (int) Math.round(value);
    }

    /** Which permanent to give up out of these: the one the AI would miss least (see {@link #sacrificeValue}). */
    public static Card chooseFodder(final Player ai, final CardCollectionView candidates) {
        Card best = null;
        int bestValue = Integer.MAX_VALUE;
        for (final Card c : candidates) {
            final int v = sacrificeValue(ai, c);
            if (v < bestValue) {
                bestValue = v;
                best = c;
            }
        }
        return best;
    }

    /**
     * What it is worth to the AI to have the spell or ability paid for: the stats of a creature, the same rules of
     * thumb as the strategic evaluator for the rest, and a bonus when it is the commander or a piece of a combo.
     */
    static int worth(final Player ai, final SpellAbility paidFor) {
        final Card host = paidFor.getHostCard();
        if (host == null) {
            return ABILITY_VALUE;
        }
        int value;
        if (!paidFor.isSpell()) {
            value = ABILITY_VALUE;
        } else if (host.isCreature()) {
            value = ComputerUtilCard.evaluateCreature(host);
        } else if (host.isInstant() || host.isSorcery()) {
            value = 40 + 12 * host.getCMC();
        } else {
            value = 50 + 30 * host.getCMC();
        }
        if (host.isCommander()) {
            value += KEY_CARD_BONUS;
        } else if (isComboPiece(ai, host)) {
            value += KEY_CARD_BONUS;
        }
        return value;
    }

    private static boolean isComboPiece(final Player ai, final Card host) {
        if (ai.getController() instanceof PlayerControllerAi) {
            final AiController brain = ((PlayerControllerAi) ai.getController()).getAi();
            return brain.deckPlan(ai).combos.isKeyCard(host) || brain.deckPlan(ai).commander.isKeyCard(host);
        }
        return false;
    }
}
