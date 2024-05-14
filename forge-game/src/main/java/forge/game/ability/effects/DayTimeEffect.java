package forge.game.ability.effects;

import org.apache.commons.lang3.ObjectUtils;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class DayTimeEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        if ("Switch".equals(sa.getParam("Value"))) {
            sb.append("if it’s night, it becomes day. Otherwise, it becomes night.");
        } else {
            sb.append("It becomes ").append(sa.getParam("Value").toLowerCase()).append(".");
        }
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        Card host = sa.getHostCard();
        Game game = host.getGame();
        String newValue = sa.getParam("Value");
        if (newValue.equals("Day")) {
            game.setDayTime(false);
        } else if (newValue.equals("Night")) {
            game.setDayTime(true);
        } else if (newValue.equals("Switch")) {
            // logic for the Celestus
            game.setDayTime(!ObjectUtils.firstNonNull(game.getDayTime(), false));
        }
    }
}
