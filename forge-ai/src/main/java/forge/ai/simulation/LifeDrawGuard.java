package forge.ai.simulation;

import forge.ai.ComputerUtil;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.cost.CostPayLife;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Griselbrand ("Pay 7 life: Draw seven cards") and the like. The rollouts like seven fresh cards, so the play is offered
 * whenever it can be paid, and an AI at 10 life pays 7 and loses to the next attack, or draws out its own library.
 * The play is refused when the life that is left, the library or the hand cannot take it, unless the AI is about to win
 * anyway. The hand: seven cards into a hand of four is eleven, and the cleanup step throws away what the AI has not played;
 * used again and again it drew twenty cards for twenty-one life and discarded down to seven. Both the Flat Monte Carlo guard
 * ({@link ComboGuard}) and the plain AI ({@code DrawAi}) ask here.
 */
public final class LifeDrawGuard {
    private LifeDrawGuard() {
    }

    /** The life the AI must have left after paying. */
    static final int LIFE_FLOOR = 8;
    /** Cards the library must still have after the draw (a library-out deck draws on purpose). */
    static final int LIBRARY_MARGIN = 3;

    /** Cards over the maximum hand size that a draw may leave: the worst of them is discarded for little. */
    static final int HAND_OVERFLOW = 2;

    public static boolean reckless(final Player ai, final SpellAbility sa, final ComboPlan plan) {
        if (ai == null || sa == null || !sa.isActivatedAbility() || sa.getPayCosts() == null
                || !sa.getPayCosts().hasSpecificCostType(CostPayLife.class)) {
            return false;
        }
        // the cards come from the Draw part, which is a sub-ability for some cards (Avacyn and Griselbrand: "then draw a card for each
        // creature you control"): its amount is read from there, not from the ability that is activated
        int cardsDrawn = 0;
        boolean draws = false;
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (a.getApi() == ApiType.Draw) {
                draws = true;
                final String amount = a.getParam("NumCards");
                cardsDrawn += amount == null ? 1 : AbilityUtils.calculateAmount(a.getHostCard(), amount, a);
            }
        }
        if (!draws || ai.cantLoseForZeroOrLessLife()) {
            return false;
        }
        final int pay = sa.getPayCosts().getCostPartByType(CostPayLife.class).getAbilityAmount(sa);
        if (pay <= 0) {
            return false;
        }
        final int after = ai.getLife() - pay;
        // A Doomsday deck with its library cut to the pile draws through it on purpose, with life paid for the free
        // draws (Street Wraith's cycling, Gitaxian Probe): the pile is the whole point and the Oracle ends the game
        if (plan != null && plan.has(ComboPlan.Kind.DOOMSDAY)
                && ai.getCardsIn(ZoneType.Library).size() <= 2 * DoomsdayPile.PILE && after > 0) {
            return false;
        }
        int threat = 0;
        for (final Card c : ai.getOpponents().getCreaturesInPlay()) {
            threat += Math.max(0, c.getNetCombatDamage());
        }
        final boolean dies = after < LIFE_FLOOR || threat >= after
                || ComputerUtil.predictNextCombatsDanger(ai, ai.getOpponents()) > 0;
        boolean decks = false;
        if (plan == null || !plan.has(ComboPlan.Kind.LIBRARY_OUT)) {
            decks = ai.getCardsIn(ZoneType.Library).size() < cardsDrawn + LIBRARY_MARGIN;
        }
        final boolean overflows = !ai.isUnlimitedHandSize()
                && ai.getCardsIn(ZoneType.Hand).size() + cardsDrawn > ai.getMaxHandSize() + HAND_OVERFLOW;
        return (dies || decks || overflows) && !ComboGuard.winLikelySoon(ai, plan);
    }
}
