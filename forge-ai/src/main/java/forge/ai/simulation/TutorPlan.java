package forge.ai.simulation;

import java.util.List;

import forge.ai.PlayerControllerAi;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Tutors: spells that search the AI's own library for a card to put into the hand or on top of the library (Demonic
 * Tutor, Vampiric Tutor, Mystical Tutor, Intuition...). The heuristics have no opinion on when that is worth a card
 * and the mana; the rollouts cannot see it either, because the evaluator counts a tutor and the card it fetches as
 * one card for one card. So a tutor was cast late or never, least of all at the opponent's end step, where it costs
 * nothing but mana that would go unused.
 * <p>
 * Here a tutor is worth casting when the deck plan knows a card it is missing (the piece of a combo, the commander's
 * key card, the mana or the big spell of a ramp deck): that is a play for the rollouts to compare, and it gets a bonus
 * for the card that is found. A tutor without such a target only gets a small bonus for using mana that would be
 * wasted, and only where it is offered (the end step, or discovery).
 */
public final class TutorPlan {
    /** Evaluator points for finding the card the deck plan is missing; a card is worth 100-250 in play. */
    static final int PLAN_PICK_BONUS = 120;
    /** The same when the card only goes on top of the library: it is drawn next turn instead of held already. */
    static final int PLAN_PICK_ON_TOP_BONUS = 90;
    /** For searching at all: a better next draw, paid with mana that would be unused. */
    static final int IDLE_BONUS = 20;

    private TutorPlan() {
    }

    /** A spell that searches its controller's library for a card (or cards) to take or to put on top. */
    public static boolean isLibraryTutor(final SpellAbility sa) {
        // Intuition's root spell ability is not hidden. Its selection is handled
        // by a separate hidden sub-ability, so recognize it by its AILogic.
        final boolean intuition = sa != null && "Intuition".equals(sa.getParam("AILogic"));
        if (sa == null || !sa.isSpell() || sa.getApi() != ApiType.ChangeZone
                || (!sa.isHidden() && !intuition) || !sa.hasParam("Origin")) {
            return false;
        }
        final List<ZoneType> origin = ZoneType.listValueOf(sa.getParam("Origin"));
        if (origin.size() != 1 || origin.get(0) != ZoneType.Library) {
            return false;
        }
        final String dest = sa.getParam("Destination");
        final boolean onTop = "Library".equals(dest)
                && ("0".equals(sa.getParam("LibraryPosition")) || "Intuition".equals(sa.getParam("AILogic")));
        if (!"Hand".equals(dest) && !onTop) {
            return false;
        }
        if (sa.hasParam("Defined") && !"You".equals(sa.getParam("Defined")) || sa.hasParam("DefinedPlayer")
                && !"You".equals(sa.getParam("DefinedPlayer")) && !sa.hasParam("Chooser")) {
            return false; // somebody else's library
        }
        final String type = sa.getParamOrDefault("ChangeType", "");
        return !(type.contains("Land") && !type.contains("Nonland")); // fetching lands is the heuristics' ramp logic
    }

    /** What the spell can find: the cards of the library that fit its description. */
    public static CardCollection pool(final Player ai, final SpellAbility sa) {
        final CardCollection lib = new CardCollection(ai.getCardsIn(ZoneType.Library));
        final Card host = sa.getHostCard();
        final CardCollection valid = CardLists.getValidCards(lib, sa.getParamOrDefault("ChangeType", "Card"), ai, host, sa);
        final CardCollectionView searchable = ai.getSearchableLibrary(sa, ai);
        if (searchable != null) {
            valid.retainAll(searchable);
        }
        valid.removeIf(c -> host != null && c.getName().equals(host.getName())); // Intuition does not fetch Intuition
        return valid;
    }

    private static DeckPlan plan(final Player ai) {
        return ai.getController() instanceof PlayerControllerAi
                ? ((PlayerControllerAi) ai.getController()).getAi().deckPlan(ai) : null;
    }

    /** The cards the deck plan wants from this tutor, or null when it knows nothing it is missing. */
    public static CardCollection planPicks(final Player ai, final SpellAbility sa) {
        final DeckPlan plan = plan(ai);
        if (plan == null || !isLibraryTutor(sa)) {
            return null;
        }
        final CardCollection pool = pool(ai, sa);
        if (pool.isEmpty()) {
            return null;
        }
        final int n = Math.max(1, AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("ChangeNum", "1"), sa));
        return plan.tutorPicks(ai, pool, n);
    }

    /** Whether the tutor has something to find that the deck plan is missing. */
    public static boolean hasPlanTarget(final Player ai, final SpellAbility sa) {
        return planPicks(ai, sa) != null;
    }

    /** Whether there is anything worth searching for at all (a land-only library is not). */
    public static boolean hasUse(final Player ai, final SpellAbility sa) {
        if (!isLibraryTutor(sa)) {
            return false;
        }
        for (final Card c : pool(ai, sa)) {
            if (!c.isLand()) {
                return true;
            }
        }
        return false;
    }

    /** Value of casting this play, in evaluator points, added to the rollouts' value of the position. */
    static int bonus(final SpellAbility sa, final FlatMonteCarloPicker.Config cfg) {
        if (sa == null || cfg.strategicWeightPercent <= 0 || !isLibraryTutor(sa)) {
            return 0;
        }
        final Player ai = sa.getActivatingPlayer() != null ? sa.getActivatingPlayer() : sa.getHostCard().getController();
        int value = 0;
        try {
            final CardCollection picks = planPicks(ai, sa);
            if (picks != null) {
                value = "Hand".equals(sa.getParam("Destination")) ? PLAN_PICK_BONUS : PLAN_PICK_ON_TOP_BONUS;
                // a search that sends the cards it does not take to the graveyard is worth what they are there
                final DeckPlan plan = plan(ai);
                if (plan != null && picks.size() > 1) {
                    value = Math.max(value, plan.combos.graveyardGain(ai, picks));
                }
            } else if (hasUse(ai, sa)) {
                value = IDLE_BONUS;
            }
        } catch (final RuntimeException e) {
            return 0; // a refinement only
        }
        return value * cfg.strategicWeightPercent / 100;
    }
}
