package forge.ai.ability;

import com.google.common.collect.Iterables;
import forge.ai.*;
import forge.card.CardType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChooseTypeAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        String aiLogic = sa.getParamOrDefault("AILogic", "");

        if (aiLogic.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.MissingLogic);
        } else if ("MostProminentComputerControls".equals(aiLogic)) {
            if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Mirror Entity Avatar")) {
                if (doMirrorEntityLogic(aiPlayer, sa)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }


            if (!chooseType(sa, aiPlayer.getCardsIn(ZoneType.Battlefield)).isEmpty()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if ("MostProminentComputerControlsOrOwns".equals(aiLogic)) {
            return !chooseType(sa, aiPlayer.getCardsIn(Arrays.asList(ZoneType.Hand, ZoneType.Battlefield))).isEmpty()
                    ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                    : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else if ("MostProminentOppControls".equals(aiLogic)) {
            return !chooseType(sa, aiPlayer.getOpponents().getCardsIn(ZoneType.Battlefield)).isEmpty()
                    ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                    : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return doTriggerNoCost(aiPlayer, sa, false);
    }

    private boolean doMirrorEntityLogic(Player aiPlayer, SpellAbility sa) {
        if (AiCardMemory.isRememberedCard(aiPlayer, sa.getHostCard(), AiCardMemory.MemorySet.ANIMATED_THIS_TURN)) {
            return false;
        }
        if (!aiPlayer.getGame().getPhaseHandler().is(PhaseType.MAIN1, aiPlayer)) {
            return false;
        }
        
        String chosenType = chooseType(sa, aiPlayer.getCardsIn(ZoneType.Battlefield));
        if (chosenType.isEmpty()) {
            return false;
        }

        int maxX = ComputerUtilMana.determineLeftoverMana(sa, aiPlayer, false);
        int avgPower = 0;
        
        // predict the opposition
        CardCollection oppCreatures = CardLists.filter(aiPlayer.getOpponents().getCreaturesInPlay(), CardPredicates.UNTAPPED);
        int maxOppPower = 0;
        int maxOppToughness = 0;
        int oppUsefulCreatures = 0;
        
        for (Card oppCre : oppCreatures) {
            if (ComputerUtilCard.isUselessCreature(aiPlayer, oppCre)) {
                continue;
            }
            if (oppCre.getNetPower() > maxOppPower) {
                maxOppPower = oppCre.getNetPower();
            }
            if (oppCre.getNetToughness() > maxOppToughness) {
                maxOppToughness = oppCre.getNetToughness();
            }
            oppUsefulCreatures++;
        }

        if (maxX > 1) {
            CardCollection cre = CardLists.filter(aiPlayer.getCardsIn(ZoneType.Battlefield),
                    CardPredicates.isType(chosenType), CardPredicates.UNTAPPED);
            if (!cre.isEmpty()) {
                for (Card c: cre) {
                    avgPower += c.getNetPower();
                }
                avgPower /= cre.size();
                
                boolean overpower = cre.size() > oppUsefulCreatures;
                if (!overpower) {
                    maxX = Math.max(0, maxX - 3); // conserve some mana unless the board position looks overpowering
                }

                if (maxX > avgPower && maxX > maxOppPower && maxX >= maxOppToughness) {
                    sa.setXManaCostPaid(maxX);
                    AiCardMemory.rememberCard(aiPlayer, sa.getHostCard(), AiCardMemory.MemorySet.ANIMATED_THIS_TURN);
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        boolean isCurse = sa.isCurse();

        if (sa.usesTargeting()) {
            final List<Player> oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
            final List<Player> alliesList = ai.getAllies().filter(PlayerPredicates.isTargetableBy(sa));

            sa.resetTargets();

            if (isCurse) {
                if (!oppList.isEmpty()) {
                    sa.getTargets().add(Iterables.getFirst(oppList, null));
                } else if (mandatory) {
                    if (!alliesList.isEmpty()) {
                        sa.getTargets().add(Iterables.getFirst(alliesList, null));
                    } else if (ai.canBeTargetedBy(sa)) {
                        sa.getTargets().add(ai);
                    }
                }
            } else {
                if (ai.canBeTargetedBy(sa)) {
                    sa.getTargets().add(ai);
                } else {
                    if (!alliesList.isEmpty()) {
                        sa.getTargets().add(Iterables.getFirst(alliesList, null));
                    } else if (!oppList.isEmpty() && mandatory) {
                        sa.getTargets().add(Iterables.getFirst(oppList, null));
                    }
                }
            }

            if (!sa.isTargetNumberValid()) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        } else {
            for (final Player p : AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa)) {
                if (p.isOpponentOf(ai) && !mandatory && !isCurse) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private String chooseType(SpellAbility sa, CardCollectionView cards) {
        Set<String> valid = new HashSet<>();

        if (sa.getSubAbility() != null && sa.getSubAbility().getApi() == ApiType.PumpAll
                && sa.getSubAbility().isCurse() && sa.getSubAbility().hasParam("NumDef")) {
            final SpellAbility pumpSa = sa.getSubAbility();
            final int defense = AbilityUtils.calculateAmount(sa.getHostCard(), pumpSa.getParam("NumDef"), pumpSa);
            for (Card c : cards) {
                if (c.isCreature() && c.getNetToughness() <= -defense) {
                    valid.addAll(c.getType().getCreatureTypes());
                }
            }
        } else {
            valid.addAll(CardType.getAllCreatureTypes());
        }

        String chosenType = ComputerUtilCard.getMostProminentType(cards, valid);
        if (chosenType.isEmpty()) {
            // Account for the situation when only changelings are on the battlefield
            boolean allChangeling = false;
            for (Card c : cards) {
                if (c.isCreature() && c.hasKeyword(Keyword.CHANGELING)) {
                    chosenType = Aggregates.random(valid); // just choose a random type for changelings
                    allChangeling = true;
                    break;
                }
            }

            if (!allChangeling) {
                // Still empty, probably no creatures on board
                return "";
            }
        }

        return chosenType;
    }
}
