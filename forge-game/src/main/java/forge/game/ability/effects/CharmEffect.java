package forge.game.ability.effects;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.collect.FCollection;

public class CharmEffect extends SpellAbilityEffect {

    public static List<AbilitySub> makePossibleOptions(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        List<String> restriction = null;

        if (sa.hasParam("ChoiceRestriction")) {
            String rest = sa.getParam("ChoiceRestriction");
            if (rest.equals("ThisGame")) {
                restriction = source.getChosenModesGame(sa);
            } else if (rest.equals("ThisTurn")) {
                restriction = source.getChosenModesTurn(sa);
            }
        }

        int indx = 0;
        List<AbilitySub> choices = Lists.newArrayList(sa.getAdditionalAbilityList("Choices"));
        if (restriction != null) {
            List<AbilitySub> toRemove = Lists.newArrayList();
            for (AbilitySub ch : choices) {
                if (restriction.contains(ch.getDescription())) {
                    toRemove.add(ch);
                }
            }
            choices.removeAll(toRemove);
        }
        // set CharmOrder
        for (AbilitySub sub : choices) {
            sub.setSVar("CharmOrder", Integer.toString(indx));
            indx++;
        }
        return choices;
    }

    public static String makeFormatedDescription(SpellAbility sa) {
        Card source = sa.getHostCard();

        List<AbilitySub> list = CharmEffect.makePossibleOptions(sa);
        final int num;
        boolean additionalDesc = sa.hasParam("AdditionalDescription");
        boolean optional = sa.hasParam("Optional");
        // hotfix for complex cards when using getCardForUi
        if (source.getController() == null && additionalDesc && !optional) {
            // using getCardForUi game is not set, so can't guess max charm
            num = Integer.MAX_VALUE;
        } else {
            num = Math.min(AbilityUtils.calculateAmount(source, sa.getParamOrDefault("CharmNum", "1"), sa), list.size());
        }
        final int min = sa.hasParam("MinCharmNum") ? AbilityUtils.calculateAmount(source, sa.getParamOrDefault("MinCharmNum", "1"), sa) : num;

        boolean repeat = sa.hasParam("CanRepeatModes");
        boolean random = sa.hasParam("Random");
        boolean limit = sa.hasParam("ActivationLimit");
        boolean oppChooses = "Opponent".equals(sa.getParam("Chooser"));

        StringBuilder sb = new StringBuilder();
        sb.append(sa.getCostDescription());
        sb.append(oppChooses ? "An opponent chooses " : "Choose ");

        if (num == min || num == Integer.MAX_VALUE) {
            sb.append(Lang.getNumeral(min));
        } else if (min == 0) {
            sb.append("up to ").append(Lang.getNumeral(num));
        } else {
            sb.append(Lang.getNumeral(min)).append(" or ").append(list.size() == 2 ? "both" : "more");
        }

        if (sa.hasParam("ChoiceRestriction")) {
            String rest = sa.getParam("ChoiceRestriction");
            if (rest.equals("ThisGame")) {
                sb.append(" that hasn't been chosen");
            } else if (rest.equals("ThisTurn")) {
                sb.append(" that hasn't been chosen this turn");
            }
        }

        if (random) {
            sb.append("at random.");
        }
        if (repeat) {
            sb.append(". You may choose the same mode more than once.");
        }
        if (limit) {
            int limitNum = AbilityUtils.calculateAmount(source, sa.getParam("ActivationLimit"), sa);
            if (limitNum == 1) {
                sb.append(". Activate only once each turn.");
            } else {
                sb.append(". Additional code needed in CharmEffect.");
            }
        }

        if (additionalDesc) {
            if (optional) {
                sb.append(". ").append(sa.getParam("AdditionalDescription").trim());
            } else {
                sb.append(" ").append(sa.getParam("AdditionalDescription").trim());
            }
        }

        if (!list.isEmpty()) {
            if (!repeat && !additionalDesc && !limit) {
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

        List<AbilitySub> choices = makePossibleOptions(sa);

        // Entwine does use all Choices
        if (sa.isEntwine()) {
            chainAbilities(sa, choices);
            return true;
        }

        Card source = sa.getHostCard();
        Player activator = sa.getActivatingPlayer();

        final int num = Math.min(AbilityUtils.calculateAmount(source, sa.getParamOrDefault("CharmNum", "1"), sa), choices.size());
        final int min = sa.hasParam("MinCharmNum") ? AbilityUtils.calculateAmount(source, sa.getParamOrDefault("MinCharmNum", "1"), sa) : num;

        // if the amount of choices is smaller than min then they can't be chosen
        if (min > choices.size()) {
            return false;
        }

        boolean isOptional = sa.hasParam("Optional");
        if (isOptional && !activator.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblWouldYouLikeCharm", CardTranslation.getTranslatedName(source.getName())))) {
            return false;
        }

        if (sa.hasParam("Random")) {
            chainAbilities(sa, Aggregates.random(choices, num));
            return true;
        }

        Player chooser = sa.getActivatingPlayer();

        if (sa.hasParam("Chooser")) {
            // Three modal cards require you to choose a player to make the modal choice'
            // Two of these also reference the chosen player during the spell effect

            //String choosers = sa.getParam("Chooser");
            FCollection<Player> opponents = activator.getOpponents(); // all cards have Choser$ Opponent, so it's hardcoded here
            chooser = activator.getController().chooseSingleEntityForEffect(opponents, sa, "Choose an opponent", null);
            source.setChosenPlayer(chooser);
        }

        List<AbilitySub> chosen = chooser.getController().chooseModeForAbility(sa, choices, min, num, sa.hasParam("CanRepeatModes"));
        chainAbilities(sa, chosen);

        // trigger without chosen modes are removed from stack
        if (sa.isTrigger()) {
            return chosen != null && !chosen.isEmpty();
        }

        // for spells and activated abilities it is possible to chose zero if minCharmNum allows it
        return true;
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
            clone.putParam("StackDescription", "SpellDescription");

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
