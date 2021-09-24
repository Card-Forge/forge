package forge.ai.ability;

import com.google.common.base.Predicate;
import forge.ai.*;
import forge.card.mana.ManaCost;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class DelayedTriggerAi extends SpellAbilityAi {

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        if ("Always".equals(sa.getParam("AILogic"))) {
            // TODO: improve ai
            return true;
        }
        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return false;
        }
        trigsa.setActivatingPlayer(ai);

        if (trigsa instanceof AbilitySub) {
            return SpellApiToAi.Converter.get(trigsa.getApi()).chkDrawbackWithSubs(ai, (AbilitySub)trigsa);
        } else {
            return AiPlayDecision.WillPlay == ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(trigsa);
        }
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return false;
        }

        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        trigsa.setActivatingPlayer(ai);

        if (!sa.hasParam("OptionalDecider")) {
            return aic.doTrigger(trigsa, true);
        } else {
            return aic.doTrigger(trigsa, !sa.getParam("OptionalDecider").equals("You"));
        }
    }

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // Card-specific logic
        String logic = sa.getParamOrDefault("AILogic", "");
        if (logic.equals("SpellCopy")) {
            // fetch Instant or Sorcery and AI has reason to play this turn
            // does not try to get itself
            final ManaCost costSa = sa.getPayCosts().getTotalMana();
            final int count = CardLists.count(ai.getCardsIn(ZoneType.Hand), new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (!(c.isInstant() || c.isSorcery()) || c.equals(sa.getHostCard())) {
                        return false;
                    }
                    for (SpellAbility ab : c.getSpellAbilities()) {
                        if (ComputerUtilAbility.getAbilitySourceName(sa).equals(ComputerUtilAbility.getAbilitySourceName(ab))
                                || ab.hasParam("AINoRecursiveCheck")) {
                            // prevent infinitely recursing mana ritual and other abilities with reentry
                            continue;
                        } else if ("SpellCopy".equals(ab.getParam("AILogic")) && ab.getApi() == ApiType.DelayedTrigger) {
                            // don't copy another copy spell, too complex for the AI
                            continue;
                        }
                        if (!ab.canPlay()) {
                            continue;
                        }
                        AiPlayDecision decision = ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(ab);
                        // see if we can pay both for this spell and for the Effect spell we're considering
                        if (decision == AiPlayDecision.WillPlay || decision == AiPlayDecision.WaitForMain2) {
                            ManaCost costAb = ab.getPayCosts().getTotalMana();
                            ManaCost total = ManaCost.combine(costSa, costAb);
                            SpellAbility combinedAb = ab.copyWithDefinedCost(new Cost(total, false));
                            // can we pay both costs?
                            if (ComputerUtilMana.canPayManaCost(combinedAb, ai, 0)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });

            if(count == 0) {
                return false;
            }
            return true;
        } else if (logic.equals("NarsetRebound")) {
            // should be done in Main2, but it might broke for other cards
            //if (phase.getPhase().isBefore(PhaseType.MAIN2)) {
            //    return false;
            //}

            // fetch Instant or Sorcery without Rebound and AI has reason to play this turn
            // only need count, not the list
            final int count = CardLists.count(ai.getCardsIn(ZoneType.Hand), new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (!(c.isInstant() || c.isSorcery()) || c.hasKeyword(Keyword.REBOUND)) {
                        return false;
                    }
                    for (SpellAbility ab : c.getSpellAbilities()) {
                        if (ComputerUtilAbility.getAbilitySourceName(sa).equals(ComputerUtilAbility.getAbilitySourceName(ab))
                                || ab.hasParam("AINoRecursiveCheck")) {
                            // prevent infinitely recursing mana ritual and other abilities with reentry
                            continue;
                        }
                        if (!ab.canPlay()) {
                            continue;
                        }
                        AiPlayDecision decision = ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(ab);
                        if (decision == AiPlayDecision.WillPlay || decision == AiPlayDecision.WaitForMain2) {
                            if (ComputerUtilMana.canPayManaCost(ab, ai, 0)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });

            if (count == 0) {
                return false;
            }

            return true;
        } else if (logic.equals("SaveCreature")) {
            CardCollection ownCreatures = ai.getCreaturesInPlay();

            ownCreatures = CardLists.filter(ownCreatures, new Predicate<Card>() {
                @Override
                public boolean apply(final Card card) {
                    if (ComputerUtilCard.isUselessCreature(ai, card)) {
                        return false;
                    }

                    return ComputerUtil.predictCreatureWillDieThisTurn(ai, card, sa);
                }
            });

            if (!ownCreatures.isEmpty()) {
                sa.getTargets().add(ComputerUtilCard.getBestAI(ownCreatures));
                return true;
            }

            return false;
        }

        // Generic logic
        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return false;
        }
        trigsa.setActivatingPlayer(ai);
        return AiPlayDecision.WillPlay == ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(trigsa);
    }

}
