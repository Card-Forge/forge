package forge.game.ability.effects;

import com.google.common.collect.Lists;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.collect.FCollection;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CharmEffect extends SpellAbilityEffect {

    public static List<AbilitySub> makePossibleOptions(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        Iterable<Object> restriction = null;

        if (sa.hasParam("ChoiceRestriction")) {
            String rest = sa.getParam("ChoiceRestriction");
            if (rest.equals("NotRemembered")) {
                restriction = source.getRemembered();
            }
        }
        
        int indx = 0;
        List<AbilitySub> choices = Lists.newArrayList(sa.getAdditionalAbilityList("Choices"));
        if (restriction != null) {
            List<AbilitySub> toRemove = Lists.newArrayList();
            for (Object o : restriction) {
                if (o instanceof AbilitySub) {
                    String abText = ((AbilitySub)o).getDescription();
                    for (AbilitySub ch : choices) {
                        if (ch.getDescription().equals(abText)) {
                            toRemove.add(ch);
                        }
                    }
                }
            }
            choices.removeAll(toRemove);
        }
        // set CharmOrder
        for (AbilitySub sub : choices) {
            sub.setTrigger(sa.isTrigger());
            sub.setSVar("CharmOrder", Integer.toString(indx));
            indx++;
        }
        return choices;
    }

    public static String makeSpellDescription(SpellAbility sa) {
        int num = Integer.parseInt(sa.getParamOrDefault("CharmNum", "1"));
        int min = Integer.parseInt(sa.getParamOrDefault("MinCharmNum", String.valueOf(num)));
        boolean repeat = sa.hasParam("CanRepeatModes");
        boolean random = sa.hasParam("Random");
        boolean oppChooses = "Opponent".equals(sa.getParam("Chooser"));

        List<AbilitySub> list = CharmEffect.makePossibleOptions(sa);

        StringBuilder sb = new StringBuilder();
        sb.append(sa.getCostDescription());
        sb.append(oppChooses ? "An opponent chooses " : "Choose ");

        if (num == min) {
            sb.append(Lang.getNumeral(num));
        } else if (min == 0) {
            sb.append("up to ").append(Lang.getNumeral(num));
        } else {
            sb.append(Lang.getNumeral(min)).append(" or ").append(list.size() == 2 ? "both" : "more");
        }

        if (random) {
            sb.append("at random.");
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

    public static String makeFormatedDescription(SpellAbility sa) {
        int num = Integer.parseInt(sa.getParamOrDefault("CharmNum", "1"));
        int min = Integer.parseInt(sa.getParamOrDefault("MinCharmNum", String.valueOf(num)));
        boolean repeat = sa.hasParam("CanRepeatModes");
        boolean random = sa.hasParam("Random");
        boolean oppChooses = "Opponent".equals(sa.getParam("Chooser"));

        List<AbilitySub> list = CharmEffect.makePossibleOptions(sa);

        StringBuilder sb = new StringBuilder();
        sb.append(sa.getCostDescription());
        sb.append(oppChooses ? "An opponent chooses " : "Choose ");

        if (num == min) {
            sb.append(Lang.getNumeral(num));
        } else if (min == 0) {
            sb.append("up to ").append(Lang.getNumeral(num));
        } else {
            sb.append(Lang.getNumeral(min)).append(" or ").append(list.size() == 2 ? "both" : "more");
        }

        if (sa.hasParam("ChoiceRestriction")) {
            String rest = sa.getParam("ChoiceRestriction");
            if (rest.equals("NotRemembered")) {
                sb.append(" that hasn't been chosen");
            }
        }

        if (random) {
            sb.append("at random.");
        }
        if (repeat) {
            sb.append(". You may choose the same mode more than once.");
        }

        boolean additionalDesc = sa.hasParam("AdditionalDescription");
        if (additionalDesc) {
            sb.append(" ").append(sa.getParam("AdditionalDescription").trim());
        }

        if (!list.isEmpty()) {
            if (!repeat && !additionalDesc) {
                sb.append(" \u2014");
            }
            sb.append("\r\n");
            for (AbilitySub sub : list) {
                sb.append("\u2022 ").append(sub.getParam("SpellDescription"));
                sb.append("\r\n");
            }
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

    public static boolean makeChoices(SpellAbility sa) {
        //this resets all previous choices
        sa.setSubAbility(null);

        // Entwine does use all Choices
        if (sa.isEntwine()) {
            chainAbilities(sa, makePossibleOptions(sa));
            return true;
        }

        final int num = sa.hasParam("CharmNumOnResolve") ?
                AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("CharmNumOnResolve"), sa)
                : Integer.parseInt(sa.getParamOrDefault("CharmNum", "1"));
        final int min = sa.hasParam("MinCharmNum") ? Integer.parseInt(sa.getParam("MinCharmNum")) : num;

        if (sa.hasParam("Random")) {
            chainAbilities(sa, Aggregates.random(makePossibleOptions(sa), num));
            return true;
        }

        Card source = sa.getHostCard();
        Player activator = sa.getActivatingPlayer();
        Player chooser = sa.getActivatingPlayer();

        if (sa.hasParam("Chooser")) {
            // Three modal cards require you to choose a player to make the modal choice'
            // Two of these also reference the chosen player during the spell effect
            
            //String choosers = sa.getParam("Chooser"); 
            FCollection<Player> opponents = activator.getOpponents(); // all cards have Choser$ Opponent, so it's hardcoded here
            chooser = activator.getController().chooseSingleEntityForEffect(opponents, sa, "Choose an opponent", null);
            source.setChosenPlayer(chooser);
        }
        
        List<AbilitySub> chosen = chooser.getController().chooseModeForAbility(sa, min, num, sa.hasParam("CanRepeatModes"));
        chainAbilities(sa, chosen);
        return chosen != null && !chosen.isEmpty();
    }

    private static void chainAbilities(SpellAbility sa, List<AbilitySub> chosen) {

        if (chosen == null) {
            return;
        }

        // Sort Chosen by SA order
        Collections.sort(chosen, new Comparator<AbilitySub>() {
            @Override
            public int compare(AbilitySub o1, AbilitySub o2) {
                return Integer.compare(o1.getSVarInt("CharmOrder"), o2.getSVarInt("CharmOrder"));
            }
        });

        for (AbilitySub sub : chosen) {
            // Clone the chosen, just in case the some subAb gets chosen multiple times
            AbilitySub clone = (AbilitySub)sub.copy();

            // update ActivatingPlayer
            clone.setActivatingPlayer(sa.getActivatingPlayer());

            // make StackDescription be the SpellDescription
            clone.getMapParams().put("StackDescription", "SpellDescription");

            // do not forget what was targeted by the subability
            SpellAbility ssa = sub;
            SpellAbility ssaClone = clone;
            while (ssa != null) {
                ssaClone.setTargetRestrictions(ssa.getTargetRestrictions());
                if (ssa.getTargetCard() != null)
                    ssaClone.setTargetCard(ssa.getTargetCard());
                ssaClone.setTargetingPlayer(ssa.getTargetingPlayer());
                ssaClone.setTargets(ssa.getTargets());

                ssa = ssa.getSubAbility();
                ssaClone = ssaClone.getSubAbility();
            }

            // add Clone to Tail of sa
            sa.appendSubAbility(clone);
        }
        
    }


}
