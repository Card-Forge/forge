package forge.ai.ability;

import forge.ai.*;
import forge.card.mana.ManaCost;
import forge.game.ability.ApiType;
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
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        if ("Always".equals(sa.getParam("AILogic"))) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        trigsa.setActivatingPlayer(ai);

        if (trigsa instanceof AbilitySub) {
            return SpellApiToAi.Converter.get(trigsa).chkDrawbackWithSubs(ai, (AbilitySub)trigsa);
        } else {
            AiPlayDecision decision = ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(trigsa);
            if (decision == AiPlayDecision.WillPlay) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        trigsa.setActivatingPlayer(ai);

        if (!sa.hasParam("OptionalDecider")) {
            if (aic.doTrigger(trigsa, true)) {
                // If the trigger is mandatory, we can play it
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else {
            if (aic.doTrigger(trigsa, !sa.getParam("OptionalDecider").equals("You"))) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
    }

    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        // Card-specific logic
        String logic = sa.getParamOrDefault("AILogic", "");
        if (logic.equals("SpellCopy")) {
            // fetch Instant or Sorcery and AI has reason to play this turn
            // does not try to get itself
            final ManaCost costSa = sa.getPayCosts().getTotalMana();
            final int count = CardLists.count(ai.getCardsIn(ZoneType.Hand), c -> {
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
                        if (ComputerUtilMana.canPayManaCost(combinedAb, ai, 0, true)) {
                            return true;
                        }
                    }
                }
                return false;
            });

            if (count == 0) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else if (logic.equals("NarsetRebound")) {
            // should be done in Main2, but it might broke for other cards
            //if (phase.getPhase().isBefore(PhaseType.MAIN2)) {
            //    return false;
            //}

            // fetch Instant or Sorcery without Rebound and AI has reason to play this turn
            // only need count, not the list
            final int count = CardLists.count(ai.getCardsIn(ZoneType.Hand), c -> {
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
                        if (ComputerUtilMana.canPayManaCost(ab, ai, 0, true)) {
                            return true;
                        }
                    }
                }
                return false;
            });

            if (count == 0) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else if (logic.equals("SaveCreature")) {
            CardCollection ownCreatures = ai.getCreaturesInPlay();

            ownCreatures = CardLists.filter(ownCreatures, card -> {
                if (ComputerUtilCard.isUselessCreature(ai, card)) {
                    return false;
                }

                return ComputerUtil.predictCreatureWillDieThisTurn(ai, card, sa);
            });

            if (!ownCreatures.isEmpty()) {
                sa.getTargets().add(ComputerUtilCard.getBestAI(ownCreatures));
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }

        // Generic logic
        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        trigsa.setActivatingPlayer(ai);

        AiPlayDecision decision = ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(trigsa);
        if (decision == AiPlayDecision.WillPlay) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }

}
