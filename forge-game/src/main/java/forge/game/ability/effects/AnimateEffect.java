package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.card.ColorSet;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.TextUtil;


public class AnimateEffect extends AnimateEffectBase {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getHostCard();

        String animateRemembered = null;
        String animateImprinted = null;

        //if host is not on the battlefield don't apply
        if (("UntilHostLeavesPlay".equals(sa.getParam("Duration")) || "UntilLoseControlOfHost".equals(sa.getParam("Duration")))
                && !source.isInPlay()) {
            return;
        }
        if ("UntilLoseControlOfHost".equals(sa.getParam("Duration")) && source.getController() != sa.getActivatingPlayer()) {
            return;
        }

        // Remember Objects
        if (sa.hasParam("RememberObjects")) {
            animateRemembered = sa.getParam("RememberObjects");
        }
        // Imprint Cards
        if (sa.hasParam("ImprintCards")) {
            animateImprinted = sa.getParam("ImprintCards");
        }

        // AF specific sa
        Integer power = null;
        if (sa.hasParam("Power")) {
            power = AbilityUtils.calculateAmount(source, sa.getParam("Power"), sa);
        }
        Integer toughness = null;
        if (sa.hasParam("Toughness")) {
            toughness = AbilityUtils.calculateAmount(source, sa.getParam("Toughness"), sa);
        }

        final Game game = sa.getActivatingPlayer().getGame();
        // Every Animate event needs a unique time stamp
        final long timestamp = game.getNextTimestamp();

        final CardType types = new CardType(true);
        if (sa.hasParam("Types")) {
            types.addAll(Arrays.asList(sa.getParam("Types").split(",")));
        }

        final CardType removeTypes = new CardType(true);
        if (sa.hasParam("RemoveTypes")) {
            removeTypes.addAll(Arrays.asList(sa.getParam("RemoveTypes").split(",")));
        }

        // allow ChosenType - overrides anything else specified
        if (types.hasSubtype("ChosenType")) {
            types.clear();
            types.add(source.getChosenType());
        } else if (types.hasSubtype("ChosenType2")) {
            types.clear();
            types.add(source.getChosenType2());
        }

        final List<String> keywords = Lists.newArrayList();
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }

        final List<String> removeKeywords = Lists.newArrayList();
        if (sa.hasParam("RemoveKeywords")) {
            removeKeywords.addAll(Arrays.asList(sa.getParam("RemoveKeywords").split(" & ")));
        }

        final List<String> hiddenKeywords = Lists.newArrayList();
        if (sa.hasParam("HiddenKeywords")) {
            hiddenKeywords.addAll(Arrays.asList(sa.getParam("HiddenKeywords").split(" & ")));
        }
        // allow SVar substitution for keywords
        for (int i = 0; i < keywords.size(); i++) {
            final String k = keywords.get(i);
            if (source.hasSVar(k)) {
                keywords.add(source.getSVar(k));
                keywords.remove(k);
            }
        }

        // colors to be added or changed to
        ColorSet finalColors = ColorSet.getNullColor();
        if (sa.hasParam("Colors")) {
            final String colors = sa.getParam("Colors");
            if (colors.equals("ChosenColor")) {
                finalColors = ColorSet.fromNames(source.getChosenColors());
            } else {
                finalColors = ColorSet.fromNames(Arrays.asList(colors.split(",")));
            }
        }

        // abilities to add to the animated being
        final List<String> abilities = Lists.newArrayList();
        if (sa.hasParam("Abilities")) {
            abilities.addAll(Arrays.asList(sa.getParam("Abilities").split(",")));
        }

        // replacement effects to add to the animated being
        final List<String> replacements = Lists.newArrayList();
        if (sa.hasParam("Replacements")) {
            replacements.addAll(Arrays.asList(sa.getParam("Replacements").split(",")));
        }

        // triggers to add to the animated being
        final List<String> triggers = Lists.newArrayList();
        if (sa.hasParam("Triggers")) {
            triggers.addAll(Arrays.asList(sa.getParam("Triggers").split(",")));
        }

        // static abilities to add to the animated being
        final List<String> stAbs = Lists.newArrayList();
        if (sa.hasParam("staticAbilities")) {
            stAbs.addAll(Arrays.asList(sa.getParam("staticAbilities").split(",")));
        }

        // sVars to add to the animated being
        Map<String, String> sVarsMap = Maps.newHashMap();
        if (sa.hasParam("sVars")) {
            for (final String s : sa.getParam("sVars").split(",")) {
                String actualsVar = AbilityUtils.getSVar(sa, s);
                String name = s;
                if (actualsVar.startsWith("SVar:")) {
                    actualsVar = actualsVar.split("SVar:")[1];
                    name = actualsVar.split(":")[0];
                    actualsVar = actualsVar.split(":")[1];
                }
                sVarsMap.put(name, actualsVar);
            }
        }



        List<Card> tgts = getCardsfromTargets(sa);

        if (sa.hasParam("Optional")) {
            final String targets = Lang.joinHomogenous(tgts);
            final String message = sa.hasParam("OptionQuestion")
                    ? TextUtil.fastReplace(sa.getParam("OptionQuestion"), "TARGETS", targets)
                    : getStackDescription(sa);

            if (!sa.getActivatingPlayer().getController().confirmAction(sa, null, message)) {
                return;
            }
        }

        for (final Card c : tgts) {
            doAnimate(c, sa, power, toughness, types, removeTypes, finalColors,
                    keywords, removeKeywords, hiddenKeywords,
                    abilities, triggers, replacements, stAbs, timestamp);

            if (sa.hasParam("Name")) {
                c.addChangedName(sa.getParam("Name"), false, timestamp, 0);
            }

            // give sVars
            if (!sVarsMap.isEmpty()) {
                c.addChangedSVars(sVarsMap, timestamp, 0);
            }

            // give Remembered
            if (animateRemembered != null) {
                c.addRemembered(AbilityUtils.getDefinedObjects(source, animateRemembered, sa));
            }

            // give Imprinted
            if (animateImprinted != null) {
                c.addImprintedCards(AbilityUtils.getDefinedCards(source, animateImprinted, sa));
            }

            if (sa.hasParam("Crew")) {
                c.becomesCrewed(sa);
            }

            game.fireEvent(new GameEventCardStatsChanged(c));
        }

        if (sa.hasParam("AtEOT") && !tgts.isEmpty()) {
            registerDelayedTrigger(sa, sa.getParam("AtEOT"), tgts);
        }
    } // animateResolve extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();
        final List<Card> tgts = getDefinedCardsOrTargeted(sa);
        //possible to be building stack desc before Defined is populated... for now, 0 will default to singular
        final boolean justOne = tgts.size() <= 1;

        if (sa.hasParam("IfDesc")) {
            if (sa.getParam("IfDesc").equals("True") && sa.hasParam("SpellDescription")) {
                String ifDesc = sa.getParam("SpellDescription");
                sb.append(ifDesc, 0, ifDesc.indexOf(",") + 1);
            } else {
                tokenizeString(sa, sb, sa.getParam("IfDesc"));
            }
            sb.append(" ");
        }

        sb.append(sa.hasParam("DefinedDesc") ? sa.getParam("DefinedDesc") : Lang.joinHomogenous(tgts));
        sb.append(" ");
        int initial = sb.length();

        Integer power = null;
        if (sa.hasParam("Power")) {
            power = AbilityUtils.calculateAmount(host, sa.getParam("Power"), sa);
        }
        Integer toughness = null;
        if (sa.hasParam("Toughness")) {
            toughness = AbilityUtils.calculateAmount(host, sa.getParam("Toughness"), sa);
        }

        final boolean permanent = "Permanent".equals(sa.getParam("Duration"));
        final List<String> types = Lists.newArrayList();
        if (sa.hasParam("Types")) {
            types.addAll(Arrays.asList(sa.getParam("Types").split(",")));
        }
        final List<String> keywords = Lists.newArrayList();
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }
        // allow SVar substitution for keywords
        for (int i = 0; i < keywords.size(); i++) {
            final String k = keywords.get(i);
            if (sa.hasSVar(k)) {
                keywords.add("\"" + k + "\"");
                keywords.remove(k);
            }
        }
        final List<String> colors =Lists.newArrayList();
        if (sa.hasParam("Colors")) {
            colors.addAll(Arrays.asList(sa.getParam("Colors").split(",")));
        }

        // if power is -1, we'll assume it's not just setting toughness
        if (power != null || toughness != null) {
            sb.append(justOne ? "has" : "have" ).append(" base ");
            if (power != null && toughness != null) {
                sb.append("power and toughness ").append(power).append("/").append(toughness).append(" ");
            } else if (power != null) {
                sb.append("power ").append(power).append(" ");
            } else {
                sb.append("toughness ").append(toughness).append(" ");
            }
        } else if (sb.length() > initial) {
            sb.append(justOne ? "becomes " : "become ");
        }

        if (colors.contains("ChosenColor")) {
            sb.append("color of that player's choice");
        } else {
            for (int i = 0; i < colors.size(); i++) {
                sb.append(colors.get(i).toLowerCase()).append(" ");
                if (i < (colors.size() - 1)) {
                    sb.append("and ");
                }
            }
        }

        if (types.contains("ChosenType")) {
            sb.append("type of player's choice ");
        } else {
            for (int i = 0; i < types.size(); i++) {
                String type = types.get(i);
                if (i == 0 && justOne) {
                    sb.append(Lang.startsWithVowel(type) ? "an " : "a ");
                }
                sb.append(CardType.CoreType.isValidEnum(type) ? type.toLowerCase() : type).append(" ");
            }
        }
        if (keywords.size() > 0) {
            sb.append(sb.length() > initial ? "and " : "").append(" gains ");
            sb.append(Lang.joinHomogenous(keywords).toLowerCase()).append(" ");
        }
        // sb.append(abilities)
        // sb.append(triggers)
        if (!permanent && sb.length() > initial) {
            final String duration = sa.getParam("Duration");
            if ("UntilEndOfCombat".equals(duration)) {
                sb.append("until end of combat");
            } else if ("UntilHostLeavesPlay".equals(duration)) {
                sb.append("until ").append(host).append(" leaves the battlefield");
            } else if ("UntilYourNextUpkeep".equals(duration)) {
                sb.append("until your next upkeep");
            } else if ("UntilYourNextTurn".equals(duration)) {
                sb.append("until your next turn");
            } else if ("UntilControllerNextUntap".equals(duration)) {
                sb.append("until its controller's next untap step");
            } else {
                sb.append("until end of turn");
            }
        }
        if (sa.hasParam("staticAbilities") && sa.getParam("staticAbilities").contains("MustAttack")) {
            sb.append(sb.length() > initial ? " and " : "");
            sb.append(justOne ? "attacks" : "attack").append(" this turn if able");
        }
        sb.append(".");

        if (sa.hasParam("AtEOT")) {
            sb.append(" ");
            final String eot = sa.getParam("AtEOT");
            final String pronoun = justOne ? "it" : "them";
            if (eot.equals("Hand")) {
                sb.append("Return ").append(pronoun).append(" to your hand");
            } else if (eot.equals("SacrificeCtrl")) {
                sb.append(justOne ? "Its controller sacrifices it" : "Their controllers sacrifice them");
            } else { //Sacrifice,Exile
                sb.append(eot).append(" ").append(pronoun);
            }
            sb.append(" at the beginning of the next end step.");
        }

        return sb.toString();
    }

}
