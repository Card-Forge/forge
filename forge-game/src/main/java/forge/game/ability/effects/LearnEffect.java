package forge.game.ability.effects;

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class LearnEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Learn. (You may reveal a Lesson card you own from outside the game and put it into your hand, or discard a card to draw a card.)";
    }
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        CardZoneTable table = AbilityKey.addCardZoneTableParams(moveParams, sa);
        for (Player p : getTargetPlayers(sa)) {
            p.learnLesson(sa, moveParams);
        }
        table.triggerChangesZoneAll(game, sa);
    }

}
