package forge.game.ability.effects;

import forge.game.GameEntity;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.Map;
import java.util.Map.Entry;

public class CountersProliferateEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Proliferate.");
        sb.append(" (You choose any number of permanents and/or players with ");
        sb.append("counters on them, then give each another counter of a kind already there.)");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        Player controller = sa.getHostCard().getController();
        Map<GameEntity, CounterType> proliferateChoice = controller.getController().chooseProliferation();
        if (proliferateChoice == null )
            return;
        for(Entry<GameEntity, CounterType> ge: proliferateChoice.entrySet()) {
            if( ge.getKey() instanceof Player )
                ((Player) ge.getKey()).addPoisonCounters(1, sa.getHostCard());
            else if( ge.getKey() instanceof Card)
                ((Card) ge.getKey()).addCounter(ge.getValue(), 1, true);
        }
    }
}
