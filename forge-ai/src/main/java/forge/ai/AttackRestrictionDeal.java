package forge.ai;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;

/**
 * "Each player may draw a card / put counters / make a land; each opponent who does can't attack you next turn"
 * (The Second Doctor, Orzhov Advokist, Noble Heritage, Zagorka): a bribe the controller offers. The AI takes the card
 * or the counters when the attack it gives up is not worth much, and refuses when that attack is what it wants to do.
 * Taking it always means a board that never attacks.
 */
public final class AttackRestrictionDeal {
    private AttackRestrictionDeal() { }

    /**
     * @param ai the player who may take the deal
     * @param offerer the player whose permanents ai could not attack after taking it
     * @return whether the deal is worth taking: the attack it gives up is small
     */
    public static boolean worthTaking(final Player ai, final Player offerer) {
        if (offerer == null || ai.equals(offerer) || !ai.isOpponentOf(offerer)) {
            return true; // nothing is given up
        }
        if (ai.getOpponents().size() > 1 && !offerer.equals(AiAttackController.choosePreferredDefenderPlayer(ai))) {
            return true; // another opponent is the better target anyway
        }
        // next turn it is untapped and no longer sick: what can attack then is what would stay at home
        final List<Integer> powers = new ArrayList<>();
        for (final Card c : ai.getCreaturesInPlay()) {
            if (c.getNetCombatDamage() > 0 && CombatUtil.canAttackNextTurn(c, offerer)) {
                powers.add(c.getNetCombatDamage());
            }
        }
        if (powers.isEmpty()) {
            return true;
        }
        powers.sort((a, b) -> b - a);
        final int total = powers.stream().mapToInt(Integer::intValue).sum();
        if (total >= offerer.getLife()) {
            return false; // the attack could kill
        }
        // the biggest attackers meet the blockers; what gets through is what the deal costs
        final int blockers = (int) offerer.getCreaturesInPlay().stream().filter(c -> CombatUtil.canBlock(c, true)).count();
        final int through = powers.stream().skip(blockers).mapToInt(Integer::intValue).sum();
        return through < Math.max(3, offerer.getLife() / 4);
    }
}
