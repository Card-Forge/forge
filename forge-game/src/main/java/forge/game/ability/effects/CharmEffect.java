package forge.game.ability.effects;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.cost.Cost;
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
            restriction = source.getChosenModes(sa, sa.getParam("ChoiceRestriction"));
        }

        List<AbilitySub> choices = Lists.newArrayList(sa.getAdditionalAbilityList("Choices"));

        if (source.getZone() != null) {
            List<AbilitySub> toRemove = Lists.newArrayList();
            for (AbilitySub ch : choices) {
                // 603.3c If one of the modes would be illegal, that mode can't be chosen.
                if ((ch.usesTargeting() && ch.getMinTargets() > 0 &&
                        ch.getTargetRestrictions().getNumCandidates(ch, true) == 0) ||
                        (restriction != null && restriction.contains(ch.getDescription()))) {
                    toRemove.add(ch);
                }
            }
            choices.removeAll(toRemove);
        }

        int indx = 0;
        // set CharmOrder
        for (AbilitySub sub : choices) {
            sub.setSVar("CharmOrder", Integer.toString(indx));
            indx++;
        }
        return choices;
    }

    public static String makeFormatedDescription(SpellAbility sa) {
        return makeFormatedDescription(sa, true);
    }
    public static String makeFormatedDescription(SpellAbility sa, boolean includeChosen) {
        Card source = sa.getHostCard();

        List<AbilitySub> list = CharmEffect.makePossibleOptions(sa);
        String numParam = sa.getParamOrDefault("CharmNum", "1");
        boolean isX = numParam.equals("X");
        int num = 0;
        boolean additionalDesc = sa.hasParam("AdditionalDescription");
        boolean optional = sa.hasParam("Optional");
        // hotfix for complex cards when using getCardForUi
        if (source.getController() == null && !StringUtils.isNumeric(numParam) && additionalDesc && !optional) {
            // using getCardForUi game is not set, so can't guess max charm
            num = Integer.MAX_VALUE;
        } else {
            // fallback needed while ability building
            if (sa.getActivatingPlayer() == null) {
                sa.setActivatingPlayer(source.getController(), true);
            }
            if (!isX) {
                num = Math.min(AbilityUtils.calculateAmount(source, numParam, sa), list.size());
            }
        }
        final int min = sa.hasParam("MinCharmNum") ? AbilityUtils.calculateAmount(source, sa.getParam("MinCharmNum"), sa) : num;

        boolean repeat = sa.hasParam("CanRepeatModes");
        boolean random = sa.hasParam("Random");
        boolean limit = sa.hasParam("ActivationLimit");
        boolean gameLimit = sa.hasParam("GameActivationLimit");
        boolean oppChooses = "Opponent".equals(sa.getParam("Chooser"));
        boolean spree = sa.hasParam("Spree");

        StringBuilder sb = new StringBuilder();
        sb.append(sa.getCostDescription());

        if (!spree) {
            sb.append(oppChooses ? "An opponent chooses " : "Choose ");
            if (isX) {
                sb.append(sa.hasParam("MinCharmNum") && min == 0 ? "up to " : "").append("X");
            } else if (num == min || num == Integer.MAX_VALUE) {
                sb.append(num == 0 ? "up to that many" : Lang.getNumeral(min));
            } else if (min == 0 && num == sa.getParam("Choices").split(",").length) {
                sb.append("any number ");
            } else if (min == 0) {
                sb.append("up to ").append(Lang.getNumeral(num));
            } else {
                sb.append(Lang.getNumeral(min)).append(" or ").append(list.size() == 2 ? "both" : "more");
            }
        }

        if (sa.hasParam("ChoiceRestriction")) {
            String rest = sa.getParam("ChoiceRestriction");
            if (rest.equals("ThisGame")) {
                sb.append(" that hasn't been chosen");
            } else if (rest.equals("ThisTurn")) {
                sb.append(" that hasn't been chosen this turn");
            } else if (rest.equals("YourLastCombat")) {
                sb.append(" that wasn't chosen during your last combat");
            }
        }

        if (random) {
            sb.append(" at random.");
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
        if (gameLimit) {
            int limitNum = AbilityUtils.calculateAmount(source, sa.getParam("GameActivationLimit"), sa);
            if (limitNum == 1) {
                sb.append(". Activate only once.");
            } else {
                sb.append(". Additional code needed in CharmEffect.");
            }
        }

        if (additionalDesc) {
            String addDescS = sa.getParam("AdditionalDescription");
            if (optional) {
                sb.append(". ").append(addDescS.trim());
            } else if (addDescS.startsWith(("."))) {
                sb.append(addDescS.trim());
            } else if (addDescS.startsWith("where X")) {
                sb.append(", ").append(addDescS.trim()).append(" \u2014");
            } else {
                sb.append(" ").append(addDescS.trim());
            }
        }

        if (!includeChosen) {
            sb.append(num == 1 ? " mode." : " modes.");
        } else if (!list.isEmpty()) {
            if (!spree) {
                if (!repeat && !additionalDesc && !limit && !gameLimit) {
                    sb.append(" \u2014");
                }
                sb.append("\r\n");
            }
            for (AbilitySub sub : list) {
                sb.append(spree ? "+ " + new Cost(sub.getParam("SpreeCost"), false).toSimpleString() + " \u2014 " : "\u2022 ").append(sub.getParam("SpellDescription"));
                sb.append("\r\n");
            }
            sb.append("\r\n");
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
        // StackDescription based on Chosen SubAbilities allowed in chainAbilities
        return "";
    }

    public static boolean makeChoices(SpellAbility sa) {
        // CR 700.2g
        if (sa.isCopied()) {
            return true;
        }

        //this resets all previous choices
        sa.setSubAbility(null);

        List<AbilitySub> choices = makePossibleOptions(sa);

        // Entwine does use all Choices
        if (sa.isEntwine()) {
            chainAbilities(sa, choices);
            return true;
        }

        final Card source = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();

        boolean canRepeat = sa.hasParam("CanRepeatModes");
        int num = AbilityUtils.calculateAmount(source, sa.getParamOrDefault("CharmNum", "1"), sa);
        final int min = sa.hasParam("MinCharmNum") ? AbilityUtils.calculateAmount(source, sa.getParam("MinCharmNum"), sa) : num;

        // if the amount of choices is smaller than min then they can't be chosen
        if (!canRepeat) {
            if (min > choices.size()) {
                return false;
            }
            num = Math.min(num, choices.size());
        }

        boolean isOptional = sa.hasParam("Optional");
        if (isOptional && !activator.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblWouldYouLikeCharm", CardTranslation.getTranslatedName(source.getName())), null)) {
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
            sa.setChoosingPlayer(chooser);
        }

        List<AbilitySub> chosen = chooser.getController().chooseModeForAbility(sa, choices, min, num, canRepeat);
        chainAbilities(sa, chosen);

        // trigger without chosen modes are removed from stack
        if (sa.isTrigger()) {
            return chosen != null && !chosen.isEmpty();
        }

        // for spells and activated abilities it is possible to chose zero if minCharmNum allows it
        return true;
    }

    public static void chainAbilities(SpellAbility sa, List<AbilitySub> chosen) {
        if (chosen == null) {
            return;
        }

        // Sort Chosen by SA order
        chosen.sort(new Comparator<AbilitySub>() {
            @Override
            public int compare(AbilitySub o1, AbilitySub o2) {
                return Integer.compare(o1.getSVarInt("CharmOrder"), o2.getSVarInt("CharmOrder"));
            }
        });

        for (AbilitySub sub : chosen) {
            // Clone the chosen, just in case the same subAb gets chosen multiple times
            AbilitySub clone = (AbilitySub)sub.copy(sa.getActivatingPlayer());

            // make StackDescription be the SpellDescription if it doesn't already have one
            if (!clone.hasParam("StackDescription")) {
                clone.putParam("StackDescription", "SpellDescription");
            }

            // add Clone to Tail of sa
            sa.appendSubAbility(clone);
        }
    }

}
