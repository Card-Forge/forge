package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
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

    /**
     * <p>
     * phasesResolve.
     * </p>
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(SpellAbility sa) {
        List<Card> tgtCards = new ArrayList<Card>();
        final Game game = sa.getActivatingPlayer().getGame();
        final Card source = sa.getSourceCard();
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
