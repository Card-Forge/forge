package forge.card.ability.effects;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import forge.Card;
import forge.CounterType;
import forge.ITargetable;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Lang;

/** 
 * API for adding to or subtracting from existing counters on a target.
 *
 */
public class CountersPutOrRemoveEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append(sa.getActivatingPlayer().getName());
        sb.append(" removes a counter from or puts another of those counters on ");

        final List<ITargetable> targets = getTargets(sa);
        sb.append(Lang.joinHomogenous(targets));

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final int counterAmount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("CounterNum"), sa);

        List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

        for (final Card tgtCard : tgtCards) {
            if (!sa.usesTargeting() || tgtCard.canBeTargetedBy(sa)) {
                if (tgtCard.hasCounters()) {
                    Pair<CounterType,String> selection = activator.getController().chooseAndRemoveOrPutCounter(tgtCard);
                    final CounterType chosenCounter = selection.getLeft();
                    final boolean putCounter = selection.getRight().startsWith("Put");

                    if (putCounter) {
                        // Put another of the chosen counter on card
                        final Zone zone = tgtCard.getGame().getZoneOf(tgtCard);
                        if (zone == null || zone.is(ZoneType.Battlefield) || zone.is(ZoneType.Stack)) {
                            tgtCard.addCounter(chosenCounter, counterAmount, true);
                        } else {
                            // adding counters to something like re-suspend cards
                            tgtCard.addCounter(chosenCounter, counterAmount, false);
                        }
                    } else {
                        tgtCard.subtractCounter(chosenCounter, counterAmount);
                    }
                }
            }
        }
    }

}
