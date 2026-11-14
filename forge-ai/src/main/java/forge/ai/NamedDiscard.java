package forge.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import forge.ai.simulation.ComboPlan;
import forge.game.GameEntity;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.cost.CostSacrifice;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Cabal Therapy and cards like it ("AILogic$ NamedDiscard"): name a card, then a player discards every copy of it.
 * The AI casts it when the name is worth something and names a card it knows is there: a card of the opponent's hand it
 * has seen revealed this turn, or the big creature a reanimator deck throws away from its own hand. With nothing known
 * it names the card the opponent's deck has most of, which is the same kind of guess as "MostProminentInHumanDeck".
 */
public final class NamedDiscard {
    private NamedDiscard() {
    }

    /**
     * The value of {@code AILogic} in the card script.
     */
    public static final String LOGIC = "NamedDiscard";

    /**
     * The player the discard part of the spell is aimed at, or null when no target has been chosen yet.
     */
    public static Player discardTarget(final SpellAbility sa) {
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (a.getApi() == ApiType.Discard && a.usesTargeting() && a.getTargets() != null) {
                for (final GameEntity e : a.getTargets().getTargetEntities()) {
                    if (e instanceof Player) {
                        return (Player) e;
                    }
                }
            }
        }
        return null;
    }

    /**
     * The nonland cards of the owner's hand that this AI has seen revealed this turn and that are still there.
     */
    static CardCollection knownInHand(final Player ai, final Player owner) {
        final CardCollection known = new CardCollection();
        final Set<Card> revealed = AiCardMemory.getMemorySet(ai, AiCardMemory.MemorySet.REVEALED_CARDS);
        if (revealed == null || revealed.isEmpty()) {
            return known;
        }
        for (final Card c : owner.getCardsIn(ZoneType.Hand)) {
            if (!c.isLand() && revealed.contains(c)) {
                known.add(c);
            }
        }
        return known;
    }

    /**
     * Of cards known to be in the hand, the name that hits the most copies, then the most expensive card.
     */
    private static String bestKnownName(final CardCollection known) {
        final Map<String, Integer> copies = new HashMap<>();
        final Map<String, Integer> cmc = new HashMap<>();
        for (final Card c : known) {
            copies.merge(c.getName(), 1, Integer::sum);
            cmc.put(c.getName(), c.getCMC());
        }
        String best = "";
        int bestScore = -1;
        for (final Map.Entry<String, Integer> e : copies.entrySet()) {
            final int score = e.getValue() * 1000 + cmc.get(e.getKey()) * 10;
            if (score > bestScore) {
                bestScore = score;
                best = e.getKey();
            }
        }
        return best;
    }

    private static ComboPlan combosOf(final Player ai) {
        return ai.getController() instanceof PlayerControllerAi
                ? ((PlayerControllerAi) ai.getController()).getAi().deckPlan(ai).combos : ComboPlan.EMPTY;
    }

    /**
     * The name to choose when the spell resolves; empty when there is nothing sensible (the caller then falls back).
     */
    public static String chooseName(final Player ai, final SpellAbility sa, final String valid) {
        final Player target = discardTarget(sa);
        if (target == null) {
            return "";
        }
        if (target == ai) {
            final String own = combosOf(ai).selfDiscardName(ai, sa.getHostCard());
            return own == null ? "" : own;
        }
        final CardCollection known = knownInHand(ai, target);
        if (!known.isEmpty()) {
            return bestKnownName(known);
        }
        CardCollectionView library = target.getCardsIn(ZoneType.Library);
        final Card source = sa.getHostCard();
        if (valid != null && !valid.isEmpty() && source != null) {
            library = CardLists.getValidCards(library, valid, source.getController(), source, sa);
        }
        return bestGuess(target, library);
    }

    /**
     * A blind guess at what the opponent holds: the card of their deck that matters most to it. Pieces of its combos
     * and its win cards come first, then the names it has the most copies of, then the most expensive.
     */
    static String bestGuess(final Player opp, final CardCollectionView pool) {
        final CardCollection cards = new CardCollection();
        for (final Card c : pool) {
            if (!c.isLand()) {
                cards.add(c);
            }
        }
        if (cards.isEmpty()) {
            return ComputerUtilCard.getMostProminentCardName(pool);
        }
        ComboPlan plan = ComboPlan.EMPTY;
        try {
            plan = forge.ai.simulation.DeckPlan.forSetting("auto", opp).combos;
        } catch (final RuntimeException e) {
            // no plan for this deck: the copies and the cost decide
        }
        final Map<String, Integer> score = new HashMap<>();
        for (final Card c : cards) {
            int v = 10 + c.getCMC();
            if (plan.isWinCard(c)) {
                v += 60;
            }
            if (plan.isKeyCard(c)) {
                v += 40;
            }
            score.merge(c.getName(), v, Integer::sum);
        }
        String best = "";
        int bestScore = -1;
        for (final Map.Entry<String, Integer> e : score.entrySet()) {
            if (e.getValue() > bestScore) {
                bestScore = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    /**
     * Whether to cast it now. Against the AI itself: when its deck wants a big creature thrown away. Against an
     * opponent: when a card seen revealed is still in their hand, or, for the spell cast from the hand, when they hold
     * at least two cards. The flashback costs a creature, so it is never a blind guess.
     */
    public static AiAbilityDecision consider(final Player ai, final SpellAbility sa) {
        if (combosOf(ai).selfDiscardName(ai, sa.getHostCard()) != null) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        final boolean flashback = sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostSacrifice.class);
        for (final Player opp : ai.getOpponents()) {
            if (opp.getCardsIn(ZoneType.Hand).isEmpty() || !opp.canDiscardBy(sa, true)) {
                continue;
            }
            if (!knownInHand(ai, opp).isEmpty()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            if (!flashback && opp.getCardsIn(ZoneType.Hand).size() >= 2) {
                return new AiAbilityDecision(70, AiPlayDecision.WillPlay);
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}