package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.CardCollection;
import forge.game.trigger.TriggerType;
import forge.util.Lang;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantPhase;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class PhasesEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();
        final List<Card> tgtCards = getTargetCards(sa);
        sb.append(Lang.joinHomogenous(tgtCards));
        sb.append(tgtCards.size() == 1 ? " phases out." : " phase out.");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        CardCollectionView tgtCards;
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
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
        } else {
            tgtCards = getDefinedCardsOrTargeted(sa);
        }
        if (sa.hasParam("AnyNumber")) {
            tgtCards = activator.getController().chooseCardsForEffect(tgtCards, sa,
                    Localizer.getInstance().getMessage("lblChooseAnyNumberToPhase"),
                    0, tgtCards.size(), true, null);
        }

        CardCollection phasedOut = new CardCollection();
        if (phaseInOrOut) { // Time and Tide and Oubliette
            CardCollection toPhase = new CardCollection();
            for (final Card tgtC : tgtCards) {
                if (tgtC.isPhasedOut() && StaticAbilityCantPhase.cantPhaseIn(tgtC)) {
                    continue;
                }
                if (!tgtC.isPhasedOut() && StaticAbilityCantPhase.cantPhaseOut(tgtC)) {
                    continue;
                }
                toPhase.add(tgtC);
            }
            for (final Card tgtC : toPhase) {
                tgtC.phase(false);
                if (tgtC.isPhasedOut()) {
                    phasedOut.add(tgtC);
                    tgtC.setWontPhaseInNormal(wontPhaseInNormal);
                } else {
                    // won't trigger tap or untap triggers when phase in
                    if (sa.hasParam("Tapped")) {
                        tgtC.setTapped(true);
                    } else if (sa.hasParam("Untapped")) {
                        tgtC.setTapped(false);
                    }
                    tgtC.setWontPhaseInNormal(false);
                }
            }
        } else { // just phase out
            for (final Card tgtC : tgtCards) {
                if (!tgtC.isPhasedOut() && !StaticAbilityCantPhase.cantPhaseOut(tgtC)) {
                    tgtC.phase(false);
                    if (tgtC.isPhasedOut()) {
                        if (sa.hasParam("RememberAffected")) {
                            source.addRemembered(tgtC);
                        }
                        phasedOut.add(tgtC);
                        tgtC.setWontPhaseInNormal(wontPhaseInNormal);
                    }
                }
            }
        }
        if (sa.hasParam("RememberValids")) {
            source.addRemembered(tgtCards);
        }
        if (!phasedOut.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Cards, phasedOut);
            game.getTriggerHandler().runTrigger(TriggerType.PhaseOutAll, runParams, false);
        }
    }
}
