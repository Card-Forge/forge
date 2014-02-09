package forge.game.ability.effects;


import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class SetInMotionEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        System.out.println("AF_SIM");
        Card source = sa.getHostCard();
        Player controller = source.getController();

        int repeats = 1;

        if (sa.hasParam("RepeatNum")) {

            repeats = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("RepeatNum"), sa);
        }

        for (int i = 0; i < repeats; i++) {

            controller.setSchemeInMotion();
        }
    }

}
