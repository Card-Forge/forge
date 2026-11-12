package forge.ai.simulation;

import forge.ai.ComputerUtilCard;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Board wipes and mass removal for Flat Monte Carlo: Wrath of God, Pyroclasm, Armageddon. The heuristics only play a wipe
 * when the opponent's permanents are worth more than the AI's; this lets the rollouts weigh one the heuristics turned
 * down, for the cases where it wins the game: the opponent loses what he was winning with and the AI keeps what it is
 * winning with. A land wipe (Armageddon, Ravages of War) is the same idea: it is a good play when the AI's creatures
 * are already ahead on the board, or when the AI's own lands survive it (indestructible lands, Crucible of Worlds), and
 * a bad one when the AI needs the lands. Nothing here approves a play; the rollouts still compare it with holding.
 */
public final class MassRemoval {
    /** A creature the AI would trade away for the opponent's: about one good creature of margin. */
    static final int MARGIN = 150;
    /** A permanent that is not a creature or a land, by its mana value. */
    private static final int PERMANENT_VALUE = 40;
    private static final int LAND_COUNT_NEEDED = 2;

    private MassRemoval() {
    }

    static boolean isWipe(final SpellAbility sa) {
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            final ApiType api = a.getApi();
            if (api == ApiType.DestroyAll || api == ApiType.DamageAll || api == ApiType.SacrificeAll) {
                return a.hasParam("ValidCards") && !a.usesTargeting();
            }
        }
        return false;
    }

    private static SpellAbility wipeNode(final SpellAbility sa) {
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            final ApiType api = a.getApi();
            if (api == ApiType.DestroyAll || api == ApiType.DamageAll || api == ApiType.SacrificeAll) {
                return a;
            }
        }
        return null;
    }

    /** Whether the rollouts should be offered this wipe now. */
    public static boolean worthOffering(final Player ai, final SpellAbility sa) {
        try {
            final SpellAbility node = isWipe(sa) ? wipeNode(sa) : null;
            return node != null && ai != null && judge(ai, sa, node);
        } catch (final RuntimeException e) {
            return false; // a refinement only: the heuristics' own choice stands
        }
    }

    private static boolean dies(final Card c, final SpellAbility node, final int damage) {
        if (node.getApi() == ApiType.DamageAll) {
            return c.isCreature() && c.getNetToughness() - c.getDamage() <= damage
                    && !c.hasKeyword(Keyword.INDESTRUCTIBLE) && c.getCounters(CounterEnumType.SHIELD) == 0;
        }
        if (node.getApi() == ApiType.DestroyAll) {
            return !c.hasKeyword(Keyword.INDESTRUCTIBLE) && c.getCounters(CounterEnumType.SHIELD) == 0;
        }
        return true; // a sacrifice goes through indestructible
    }

    private static int value(final Card c) {
        return c.isCreature() ? ComputerUtilCard.evaluateCreature(c)
                : c.isLand() ? 0 : PERMANENT_VALUE * (c.getCMC() + 1);
    }

    private static boolean judge(final Player ai, final SpellAbility sa, final SpellAbility node) {
        final Card host = sa.getHostCard();
        final String valid = node.getParam("ValidCards");
        final int damage = node.getApi() == ApiType.DamageAll ? AbilityUtils.calculateAmount(host, node.getParam("NumDmg"), node) : 0;
        final CardCollection onBoard = CardLists.getValidCards(ai.getGame().getCardsIn(ZoneType.Battlefield), valid, ai, host, node);
        final CardCollection oppLost = new CardCollection();
        final CardCollection aiLost = new CardCollection();
        for (final Card c : onBoard) {
            if (!dies(c, node, damage)) {
                continue;
            }
            (c.getController().isOpponentOf(ai) ? oppLost : aiLost).add(c);
        }
        if (oppLost.isEmpty()) {
            return false;
        }
        final boolean landsOnly = onBoard.allMatch(Card::isLand);
        return landsOnly ? landWipe(ai, oppLost, aiLost) : valueWipe(oppLost, aiLost);
    }

    /** Wrath and Pyroclasm: what the opponent loses is worth clearly more than what the AI loses. */
    private static boolean valueWipe(final CardCollection oppLost, final CardCollection aiLost) {
        int opp = 0;
        int mine = 0;
        for (final Card c : oppLost) {
            opp += value(c);
        }
        for (final Card c : aiLost) {
            mine += value(c);
        }
        return opp >= MARGIN && opp - mine >= MARGIN && mine * 2 <= opp;
    }

    /**
     * Armageddon: worth it when the AI keeps its lands (they are indestructible, or it has few to lose), or when its
     * creatures trump the opponent's and finish the game before the opponent can rebuild without mana, and the opponent
     * is no threat to it in the meantime.
     */
    private static boolean landWipe(final Player ai, final CardCollection oppLost, final CardCollection aiLost) {
        if (oppLost.size() < LAND_COUNT_NEEDED) {
            return false;
        }
        final CardCollection myCreatures = ai.getCreaturesInPlay();
        final int myValue = ComputerUtilCard.evaluateCreatureList(myCreatures);
        int theirValue = 0;
        int theirPower = 0;
        Player target = null;
        for (final Player o : ai.getOpponents()) {
            theirValue += ComputerUtilCard.evaluateCreatureList(o.getCreaturesInPlay());
            for (final Card c : o.getCreaturesInPlay()) {
                theirPower += Math.max(0, c.getNetPower());
            }
            if (target == null || o.getLife() < target.getLife()) {
                target = o;
            }
        }
        // the AI's own lands survive (or nearly): the wipe costs it nothing
        if (aiLost.size() <= 1 && aiLost.size() * 3 < oppLost.size()) {
            return true;
        }
        if (myCreatures.isEmpty() || myValue < theirValue + MARGIN) {
            return false; // a wipe that leaves the AI as mana-starved as the opponent is a coin flip at best
        }
        int myPower = 0;
        for (final Card c : myCreatures) {
            myPower += Math.max(0, c.getNetPower());
        }
        if (myPower <= 0 || ai.getLife() <= theirPower * 2) {
            return false;
        }
        // a clock: the opponent, without lands, is dead in a few turns of attacks, or has nothing to block them with
        return target != null && (target.getLife() <= myPower * 4 || theirValue == 0);
    }
}
