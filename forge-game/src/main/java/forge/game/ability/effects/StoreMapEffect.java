package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class StoreMapEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        List<GameEntity> entity = new ArrayList<GameEntity>();
        List<Object> objects = new ArrayList<Object>();

        if (sa.hasParam("RememberEntity")) {
            entity.addAll(AbilityUtils.getDefinedPlayers(source, sa.getParam("RememberEntity"), sa));
            entity.addAll(AbilityUtils.getDefinedCards(source, sa.getParam("RememberEntity"), sa));
        }

        if (sa.hasParam("RememberObjects")) {
            String type = sa.hasParam("ObjectType") ? sa.getParam("ObjectType") : "Card";
            if (type.equals("Card")) {
                objects.addAll(AbilityUtils.getDefinedCards(source, sa.getParam("RememberObjects"), sa));
            }
        }
        
        for (GameEntity e : entity) {
            source.addRememberMap(e, objects);
        }

        if (sa.hasParam("Clear")) {
            for (GameEntity e : entity) {
                source.getRememberMap().get(e).clear();
            }
        }
        
    }

}
