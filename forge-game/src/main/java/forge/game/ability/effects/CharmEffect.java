package forge.game.ability.effects;

import com.google.common.collect.Iterables;
import forge.game.ability.AbilityFactory;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.collect.FCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CharmEffect extends SpellAbilityEffect {

    public static List<AbilitySub> makePossibleOptions(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        Iterable<Object> restriction = null;
        final String[] saChoices = sa.getParam("Choices").split(",");

        if (sa.hasParam("ChoiceRestriction")) {
            String rest = sa.getParam("ChoiceRestriction");
            if (rest.equals("NotRemembered")) {
                restriction = source.getRemembered();
            }
        }

        List<AbilitySub> choices = new ArrayList<AbilitySub>();
        int indx = 0;
        for (final String saChoice : saChoices) {
            if (restriction != null && Iterables.contains(restriction, saChoice)) {
                // If there is a choice restriction, and the current choice fails that, skip it.
                continue;
            }
            final String ab = source.getSVar(saChoice);
            AbilitySub sub = (AbilitySub) AbilityFactory.getAbility(ab, source);
            if (sa.isIntrinsic()) {
            	sub.setIntrinsic(true);
            	sub.changeText();
            }
            sub.setTrigger(sa.isTrigger());

            sub.setSVar("CharmOrder", Integer.toString(indx));
            choices.add(sub);
            indx++;
        }
        return choices;
    }

    public static String makeSpellDescription(SpellAbility sa) {
        int num = Integer.parseInt(sa.getParamOrDefault("CharmNum", "1"));
        int min = Integer.parseInt(sa.getParamOrDefault("MinCharmNum", String.valueOf(num)));
        boolean repeat = sa.hasParam("CanRepeatModes");

        List<AbilitySub> list = CharmEffect.makePossibleOptions(sa);

        StringBuilder sb = new StringBuilder("Choose ");
        if (num == min) {
            sb.append(Lang.getNumeral(num));
        } else {
            sb.append(Lang.getNumeral(min)).append(" or ").append(list.size() == 2 ? "both" : "more");
        }

        if (repeat) {
    	    sb.append(". You may choose the same mode more than once.");
        }
        sb.append(" - ");
        int i = 0;
        for (AbilitySub sub : list) {
    	    if (i > 0) {
    	        sb.append("; ");
    	    }
    	    sb.append(sub.getParam("SpellDescription"));
    	    ++i;
    	}

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        // all chosen modes have been chained as subabilities to this sa.
        // so nothing to do in this resolve
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        // TODO Build StackDescription based on Chosen SubAbilities
        return "";
    }

    public static void makeChoices(SpellAbility sa) {
        //this resets all previous choices
        sa.setSubAbility(null);

        final int num = Integer.parseInt(sa.hasParam("CharmNum") ? sa.getParam("CharmNum") : "1");
        final int min = sa.hasParam("MinCharmNum") ? Integer.parseInt(sa.getParam("MinCharmNum")) : num;

        Card source = sa.getHostCard();
        Player activator = sa.getActivatingPlayer();
        Player chooser = sa.getActivatingPlayer();

        if (sa.hasParam("Chooser")) {
            // Three modal cards require you to choose a player to make the modal choice'
            // Two of these also reference the chosen player during the spell effect
            
            //String choosers = sa.getParam("Chooser"); 
            FCollection<Player> opponents = activator.getOpponents(); // all cards have Choser$ Opponent, so it's hardcoded here
            chooser = activator.getController().chooseSingleEntityForEffect(opponents, sa, "Choose an opponent");
            source.setChosenPlayer(chooser);
        }
        
        List<AbilitySub> chosen = chooser.getController().chooseModeForAbility(sa, min, num, sa.hasParam(("CanRepeatModes")));
        chainAbilities(sa, chosen);
    }

    private static void chainAbilities(SpellAbility sa, List<AbilitySub> chosen) {
        SpellAbility saDeepest = sa;
        while (saDeepest.getSubAbility() != null) {
            saDeepest = saDeepest.getSubAbility();
        }

        // Sort Chosen by SA order
        Collections.sort(chosen, new Comparator<AbilitySub>() {
            @Override
            public int compare(AbilitySub o1, AbilitySub o2) {
                return Integer.parseInt(o1.getSVar("CharmOrder")) - Integer.parseInt(o2.getSVar("CharmOrder"));
            }
        });

        for (AbilitySub sub : chosen) {
            // Clone the chosen, just in case the some subAb gets chosen multiple times
            AbilitySub clone = (AbilitySub)sub.getCopy();

            saDeepest.setSubAbility(clone);
            clone.setActivatingPlayer(saDeepest.getActivatingPlayer());

            // do not forget what was targeted by the subability
            SpellAbility ssa = sub;
            SpellAbility ssaClone = clone;
            while (ssa != null) {
                ssaClone.setTargets(ssa.getTargets());
                ssaClone.setTargetRestrictions(ssa.getTargetRestrictions());
                if (ssa.getTargetCard() != null)
                    ssaClone.setTargetCard(ssa.getTargetCard());
                ssaClone.setTargetingPlayer(ssa.getTargetingPlayer());

                ssa = ssa.getSubAbility();
                ssaClone = ssaClone.getSubAbility();
            }

            clone.setParent(saDeepest);

            // to chain the next one (but make sure it goes all the way at the end of the SA chain)
            saDeepest = clone;
            while (saDeepest.getSubAbility() != null) {
                saDeepest = saDeepest.getSubAbility();
            }
        }
    }


}
