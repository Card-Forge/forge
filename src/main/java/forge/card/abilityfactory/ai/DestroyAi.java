package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Counters;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class DestroyAi extends SpellAiLogic {

    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(Map<String, String> params, SpellAbility sa, Player ai) {
        return canPlayAI(ai, params, sa);
    }
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public boolean canPlayAI(Player ai, Map<String, String> params, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Target abTgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final boolean noRegen = params.containsKey("NoRegen");
        List<Card> list;

        if (abCost != null) {
            if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                return false;
            }
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // Targeting
        if (abTgt != null) {
            abTgt.resetTargets();
            list = CardLists.getTargetableCards(ai.getOpponent().getCardsIn(ZoneType.Battlefield), sa);
            list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source);
            if (params.containsKey("AITgts")) {
                list = CardLists.getValidCards(list, params.get("AITgts"), sa.getActivatingPlayer(), source);
            }
            list = CardLists.getNotKeyword(list, "Indestructible");
            if (!AbilityFactory.playReusable(ai, sa)) {
                list = CardLists.filter(list, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return (!c.hasKeyword("Undying") || c.getCounters(Counters.P1P1) > 0);
                    }
                });
            }

            // If NoRegen is not set, filter out creatures that have a
            // regeneration shield
            if (!noRegen) {
                // TODO filter out things that might be tougher?
                list = CardLists.filter(list, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return ((c.getShield() == 0) && !ComputerUtil.canRegenerate(c));
                    }
                });
            }


            if (list.size() == 0) {
                return false;
            }
            // target loop
            while (abTgt.getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)) {
                if (list.size() == 0) {
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                Card choice = null;
                // If the targets are only of one type, take the best
                if (CardLists.getNotType(list, "Creature").isEmpty()) {
                    choice = CardFactoryUtil.getBestCreatureAI(list);
                } else if (CardLists.getNotType(list, "Land").isEmpty()) {
                    choice = CardFactoryUtil.getBestLandAI(list);
                } else {
                    choice = CardFactoryUtil.getMostExpensivePermanentAI(list, sa, true);
                }

                if (choice == null) { // can't find anything left
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }
                list.remove(choice);
                abTgt.addTarget(choice);
            }
        } else {
            if (params.containsKey("Defined")) {
                list = new ArrayList<Card>(AbilityFactory.getDefinedCards(source, params.get("Defined"), sa));
                if (list.isEmpty()
                        || !CardLists.filterControlledBy(list, ai).isEmpty()
                        || CardLists.getNotKeyword(list, "Indestructible").isEmpty()) {
                    return false;
                }
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final boolean noRegen = params.containsKey("NoRegen");
        final Player opp = ai.getOpponent();
        if (tgt != null) {
            List<Card> list;
            list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            list = CardLists.getTargetableCards(list, sa);
            list = CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source);

            if ((list.size() == 0) || (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa))) {
                return false;
            }

            tgt.resetTargets();

            List<Card> preferred = CardLists.getNotKeyword(list, "Indestructible");
            preferred = CardLists.filterControlledBy(preferred, opp);

            // If NoRegen is not set, filter out creatures that have a
            // regeneration shield
            if (!noRegen) {
                // TODO filter out things that could regenerate in response?
                // might be tougher?
                preferred = CardLists.filter(preferred, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return c.getShield() == 0;
                    }
                });
            }

            for (final Card c : preferred) {
                list.remove(c);
            }

            while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
                if (preferred.size() == 0) {
                    if ((tgt.getNumTargeted() == 0)
                            || (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa))) {
                        if (!mandatory) {
                            tgt.resetTargets();
                            return false;
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                } else {
                    Card c;
                    if (CardLists.getNotType(preferred, "Creature").size() == 0) {
                        c = CardFactoryUtil.getBestCreatureAI(preferred);
                    } else if (CardLists.getNotType(preferred, "Land").size() == 0) {
                        c = CardFactoryUtil.getBestLandAI(preferred);
                    } else {
                        c = CardFactoryUtil.getMostExpensivePermanentAI(preferred, sa, false);
                    }
                    tgt.addTarget(c);
                    preferred.remove(c);
                }
            }

            while (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
                if (list.size() == 0) {
                    break;
                } else {
                    Card c;
                    if (CardLists.getNotType(list, "Creature").size() == 0) {
                        c = CardFactoryUtil.getWorstCreatureAI(list);
                    } else {
                        c = CardFactoryUtil.getCheapestPermanentAI(list, sa, false);
                    }
                    tgt.addTarget(c);
                    list.remove(c);
                }
            }

            if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
                return false;
            }
        } else {
            if (!mandatory) {
                return false;
            }
        }

        return true;
    }

}