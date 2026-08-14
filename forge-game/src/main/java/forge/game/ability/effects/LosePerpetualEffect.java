package forge.game.ability.effects;

import com.google.common.collect.Lists;
import com.google.common.collect.Table.Cell;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.ICardTraitChanges;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;

public class LosePerpetualEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        long toRemove = (long) 0;
        // currently only part of perpetual triggers... expand in future as needed
        if (sa.getTrigger() != null) {
            Trigger trig = sa.getTrigger();
            for (Cell<Long, Long, ICardTraitChanges> cell : host.getChangedCardTraits().cellSet()) {
                if (cell.getValue().applyTrigger(Lists.newArrayList()).contains(trig)) {
                    toRemove = cell.getRowKey();
                    break;
                }
            }
            if (toRemove != (long) 0) {
                // Go through Card's mutator so derived trigger/static/replacement views are
                // invalidated along with the perpetual trait layer.
                host.removeChangedCardTraits(toRemove, (long) 0);
                host.removePerpetual(toRemove);
            }       
        }
    }
}
