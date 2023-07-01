package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class DayTimeEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("It becomes ").append(sa.getParam("Value").toLowerCase()).append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        Card host = sa.getHostCard();
        Game game = host.getGame();
        boolean cantBeNight = game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noNight);

        String newValue = sa.getParam("Value");
        if (newValue.equals("Day")) {
            game.setDayTime(false);
        } else if (newValue.equals("Night") && !cantBeNight) {
            game.setDayTime(true);
        } else if (newValue.equals("Switch")) {
            // logic for the Celestus
            Boolean oldValue = game.getDayTime();
            if (oldValue == null && !cantBeNight) {
                game.setDayTime(true); // if it was neither it becomes night
            } else if (oldValue == true || !cantBeNight) {
                game.setDayTime(!oldValue);
            }
        }
    }
}
