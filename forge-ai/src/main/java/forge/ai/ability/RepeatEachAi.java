package forge.ai.ability;

import forge.ai.*;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

import java.util.List;
import java.util.Map;


public class RepeatEachAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParam("AILogic");

        if ("PriceOfProgress".equals(logic)) {
            return SpecialCardAi.PriceOfProgress.consider(aiPlayer, sa);
        } else if ("Never".equals(logic)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else if ("CloneAllTokens".equals(logic)) {
            List<Card> humTokenCreats = CardLists.filter(aiPlayer.getOpponents().getCreaturesInPlay(), CardPredicates.TOKEN);
            List<Card> compTokenCreats = aiPlayer.getTokensInPlay();

            if (compTokenCreats.size() > humTokenCreats.size()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if ("BalanceLands".equals(logic)) {
            if (aiPlayer.getLandsInPlay().size() >= 5) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            List<Player> opponents = aiPlayer.getOpponents();
            for(Player opp : opponents) {
                if (opp.getLandsInPlay().size() < 4) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
        } else if ("AllPlayerLoseLife".equals(logic)) {
            final Card source = sa.getHostCard();
            SpellAbility repeat = sa.getAdditionalAbility("RepeatSubAbility");

            String svar = repeat.getSVar(repeat.getParam("LifeAmount"));
            // replace RememberedPlayerCtrl with YouCtrl
            String svarYou = TextUtil.fastReplace(svar, "RememberedPlayer", "You");

            // Currently all Cards with that are affect all player, including AI
            if (aiPlayer.canLoseLife()) {
                int lossYou = AbilityUtils.calculateAmount(source, svarYou, repeat);

                // if playing it would cause AI to lose most life, don't do that
                if (lossYou + 5 > aiPlayer.getLife()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }

            boolean hitOpp = false;
            // need a copy for source so YouCtrl can be faked
            final Card sourceLKI = CardCopyService.getLKICopy(source);

            // check if any opponent is affected
            for (final Player opp : aiPlayer.getOpponents()) {
                if (opp.canLoseLife()) {
                    sourceLKI.setOwner(opp);
                    int lossOpp = AbilityUtils.calculateAmount(source, svarYou, repeat);
                    if (lossOpp > 0) {
                        hitOpp = true;
                    }
                }
            }

            if (hitOpp) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if ("EquipAll".equals(logic)) {
            if (aiPlayer.getGame().getPhaseHandler().is(PhaseType.MAIN1, aiPlayer)) {
                final CardCollection unequipped = CardLists.filter(aiPlayer.getCardsIn(ZoneType.Battlefield), card -> card.isEquipment() && card.getAttachedTo() != sa.getHostCard());

                if (!unequipped.isEmpty()) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }

            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // TODO Add some normal AI variability here

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        return ComputerUtilCard.getBestCreatureAI(options);
    }
}
