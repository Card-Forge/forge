package forge.ai.ability;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Predicates;

import forge.ai.AiCardMemory;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.card.CardType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

public class ChooseTypeAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        if (!sa.hasParam("AILogic")) {
            return false;
        } else if ("MostProminentComputerControls".equals(sa.getParam("AILogic"))) {
            if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Mirror Entity Avatar")) {
                return doMirrorEntityLogic(aiPlayer, sa);
            }
            return !chooseType(sa, aiPlayer.getCardsIn(ZoneType.Battlefield)).isEmpty();
        } else if ("MostProminentOppControls".equals(sa.getParam("AILogic"))) {
            return !chooseType(sa, aiPlayer.getOpponents().getCardsIn(ZoneType.Battlefield)).isEmpty();
        }

        return doTriggerAINoCost(aiPlayer, sa, false);
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

        int maxX = ComputerUtilMana.determineLeftoverMana(sa, aiPlayer);
        int avgPower = 0;
        
        // predict the opposition
        CardCollection oppCreatures = CardLists.filter(aiPlayer.getOpponents().getCreaturesInPlay(), CardPredicates.Presets.UNTAPPED);
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
                    Predicates.and(CardPredicates.isType(chosenType), CardPredicates.Presets.UNTAPPED));
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
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) {
            sa.resetTargets();
            sa.getTargets().add(ai);
        } else {
            for (final Player p : AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa)) {
                if (p.isOpponentOf(ai) && !mandatory) {
                    return false;
                }
            }
        }
        return true;
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
                if (c.isCreature() && c.hasStartOfKeyword(Keyword.CHANGELING.toString())) {
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
