package forge.game.ability.effects;

import forge.game.Game;
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
        final Player p = sa.getActivatingPlayer();
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        Player controller = host.getController();
        Map<GameEntity, CounterType> proliferateChoice = controller.getController().chooseProliferation(sa);
        if (proliferateChoice == null )
            return;
        for(Entry<GameEntity, CounterType> ge: proliferateChoice.entrySet()) {
            if( ge.getKey() instanceof Player )
                ((Player) ge.getKey()).addCounter(ge.getValue(), 1, p, true);
            else if( ge.getKey() instanceof Card) {
                Card c = (Card) ge.getKey(); 
                c.addCounter(ge.getValue(), 1, p, true);
                game.updateLastStateForCard(c);
            }
        }
    }
}
