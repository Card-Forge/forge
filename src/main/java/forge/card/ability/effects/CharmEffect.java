package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.card.ability.AbilityFactory;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class CharmEffect extends SpellAbilityEffect {

    public static List<AbilitySub> makePossibleOptions(final SpellAbility sa) {
        final Card source = sa.getSourceCard();

        final String[] saChoices = sa.getParam("Choices").split(",");
        List<AbilitySub> choices = new ArrayList<AbilitySub>();
        for (final String saChoice : saChoices) {
            final String ab = source.getSVar(saChoice);
            AbilitySub sub = (AbilitySub) AbilityFactory.getAbility(ab, source);
            sub.setTrigger(sa.isTrigger());
            choices.add(sub);
        }
        return choices;
    }

    @Override
    public void resolve(SpellAbility sa) {
        // all chosen modes have been chained as subabilities to this sa.
        // so nothing to do in this resolve
    }


    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        // nothing stack specific for Charm

        return sb.toString();
    }

    public static void makeChoices(SpellAbility sa) {
        //this resets all previous choices
        sa.setSubAbility(null);

        final int num = Integer.parseInt(sa.hasParam("CharmNum") ? sa.getParam("CharmNum") : "1");
        final int min = sa.hasParam("MinCharmNum") ? Integer.parseInt(sa.getParam("MinCharmNum")) : num;
        final List<AbilitySub> choices = makePossibleOptions(sa);

        Card source = sa.getSourceCard();
        Player activator = sa.getActivatingPlayer();
        Player chooser = sa.getActivatingPlayer();

        if (sa.hasParam("Chooser")) {
            // Three modal cards require you to choose a player to make the modal choice'
            // Two of these also reference the chosen player during the spell effect
            
            //String choosers = sa.getParam("Chooser"); 
            List<Player> opponents = activator.getOpponents(); // all cards have Choser$ Opponent, so it's hardcoded here
            chooser = activator.getController().chooseSinglePlayerForEffect(opponents, sa, "Choose an opponent");
            source.setChosenPlayer(chooser);
        }
        
        List<AbilitySub> chosen = chooser.getController().chooseModeForAbility(sa, choices, min, num);
        chainAbilities(sa, chosen);
    }

    private static void chainAbilities(SpellAbility sa, List<AbilitySub> chosen) {
        SpellAbility saDeepest = sa;
        while (saDeepest.getSubAbility() != null) {
            saDeepest = saDeepest.getSubAbility();
        }

        for (AbilitySub sub : chosen) {
            saDeepest.setSubAbility(sub);
            sub.setActivatingPlayer(saDeepest.getActivatingPlayer());
            sub.setParent(saDeepest);

            // to chain the next one
            saDeepest = sub;
        }
    }


}
