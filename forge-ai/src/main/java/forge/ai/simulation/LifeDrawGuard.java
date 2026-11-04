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
 * The play is refused when the life that is left or the library cannot take it, unless the AI is about to win anyway.
 */
final class LifeDrawGuard {
    private LifeDrawGuard() {
    }

    /** The life the AI must have left after paying. */
    static final int LIFE_FLOOR = 8;
    /** Cards the library must still have after the draw (a library-out deck draws on purpose). */
    static final int LIBRARY_MARGIN = 3;

    static boolean reckless(final Player ai, final SpellAbility sa, final ComboPlan plan) {
        if (ai == null || sa == null || !sa.isActivatedAbility() || sa.getPayCosts() == null
                || !sa.getPayCosts().hasSpecificCostType(CostPayLife.class)) {
            return false;
        }
        boolean draws = false;
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            draws |= a.getApi() == ApiType.Draw;
        }
        if (!draws || ai.cantLoseForZeroOrLessLife()) {
            return false;
        }
        final int pay = sa.getPayCosts().getCostPartByType(CostPayLife.class).getAbilityAmount(sa);
        if (pay <= 0) {
            return false;
        }
        final int after = ai.getLife() - pay;
        int threat = 0;
        for (final Card c : ai.getOpponents().getCreaturesInPlay()) {
            threat += Math.max(0, c.getNetPower());
        }
        final boolean dies = after < LIFE_FLOOR || threat >= after
                || ComputerUtil.predictNextCombatsDanger(ai, ai.getOpponents()) > 0;
        boolean decks = false;
        if (plan == null || !plan.has(ComboPlan.Kind.LIBRARY_OUT)) {
            final String amount = sa.getParam("NumCards");
            final int cards = amount == null ? 1 : AbilityUtils.calculateAmount(sa.getHostCard(), amount, sa);
            decks = ai.getCardsIn(ZoneType.Library).size() < cards + LIBRARY_MARGIN;
        }
        return (dies || decks) && !ComboGuard.winLikelySoon(ai, plan);
    }
}
