package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class PhasesEffect extends SpellAbilityEffect {

    // ******************************************
    // ************** Phases ********************
    // ******************************************
    // Phases generally Phase Out. Time and Tide is the only card that can force
    // Phased Out cards in.

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();
        final List<Card> tgtCards = getTargetCards(sa);
        sb.append(StringUtils.join(tgtCards, ", "));
        sb.append(" Phases Out.");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        CardCollectionView tgtCards;
        final Game game = sa.getActivatingPlayer().getGame();
        final Card source = sa.getHostCard();
        final boolean phaseInOrOut = sa.hasParam("PhaseInOrOutAll");

        if (sa.hasParam("AllValid")) {
            if (phaseInOrOut) {
                tgtCards = game.getCardsIncludePhasingIn(ZoneType.Battlefield);
            } else {
                tgtCards = game.getCardsIn(ZoneType.Battlefield);
            }
            tgtCards = AbilityUtils.filterListByType(tgtCards, sa.getParam("AllValid"), sa);
        } else if (sa.hasParam("Defined")) {
            tgtCards = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
        } else {
            tgtCards = getTargetCards(sa);
        }
        if (phaseInOrOut) { // Time and Tide
            for (final Card tgtC : tgtCards) {
                tgtC.phase();
            }
        } else { // just phase out
            for (final Card tgtC : tgtCards) {
                if (!tgtC.isPhasedOut()) {
                    tgtC.phase();
                }
            }
        }
    }
}
