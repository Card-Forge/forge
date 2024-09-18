package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.ComputerUtilCard;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.card.CardPredicates.Presets;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;


public class RepeatEachAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParam("AILogic");

        if ("PriceOfProgress".equals(logic)) {
            return SpecialCardAi.PriceOfProgress.consider(aiPlayer, sa);
        } else if ("Never".equals(logic)) {
            return false;
        } else if ("CloneAllTokens".equals(logic)) {
            List<Card> humTokenCreats = CardLists.filter(aiPlayer.getOpponents().getCreaturesInPlay(), Presets.TOKEN);
            List<Card> compTokenCreats = aiPlayer.getTokensInPlay();

            return compTokenCreats.size() > humTokenCreats.size();
        } else if ("BalanceLands".equals(logic)) {
            if (aiPlayer.getLandsInPlay().size() >= 5) {
                return false;
            }

            List<Player> opponents = aiPlayer.getOpponents();
            for(Player opp : opponents) {
                if (opp.getLandsInPlay().size() < 4) {
                    return false;
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
                    return false;
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
            // would not hit opponent, don't do that
            return hitOpp;
        } else if ("EquipAll".equals(logic)) {
            if (aiPlayer.getGame().getPhaseHandler().is(PhaseType.MAIN1, aiPlayer)) {
                final CardCollection unequipped = CardLists.filter(aiPlayer.getCardsIn(ZoneType.Battlefield), card -> card.isEquipment() && card.getAttachedTo() != sa.getHostCard());

                return !unequipped.isEmpty();
            }

            return false;
        }

        // TODO Add some normal AI variability here

        return true;
    }

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        return ComputerUtilCard.getBestCreatureAI(options);
    }
}
