package forge.game.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

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
        final boolean phaseInOrOut = sa.hasParam("PhaseInOrOut");
        final boolean wontPhaseInNormal = sa.hasParam("WontPhaseInNormal");

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
        if (phaseInOrOut) { // Time and Tide and Oubliette
            for (final Card tgtC : tgtCards) {
                tgtC.phase(false);
                if (!tgtC.isPhasedOut()) {
                    // won't trigger tap or untap triggers when phase in
                    if (sa.hasParam("Tapped")) {
                        tgtC.setTapped(true);
                    } else if (sa.hasParam("Untapped")) {
                        tgtC.setTapped(false);
                    }
                    tgtC.setWontPhaseInNormal(false);
                } else {
                    tgtC.setWontPhaseInNormal(wontPhaseInNormal);
                }
            }
        } else { // just phase out
            for (final Card tgtC : tgtCards) {
                if (!tgtC.isPhasedOut()) {
                    tgtC.phase(false);
                    if (tgtC.isPhasedOut()) {
                        tgtC.setWontPhaseInNormal(wontPhaseInNormal);
                    }
                }
            }
        }
    }
}
