package forge.card.ability.effects;


import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class SetInMotionEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        System.out.println("AF_SIM");
        Card source = sa.getSourceCard();
        Player controller = source.getController();

        int repeats = 1;

        if (sa.hasParam("RepeatNum")) {

            repeats = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("RepeatNum"), sa);
        }

        for (int i = 0; i < repeats; i++) {

            controller.setSchemeInMotion();
        }
    }

}
