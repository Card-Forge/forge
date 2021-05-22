package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardUtil;
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
        String tmpDesc = "";
        if (sa.hasParam("Colors")) {
            final String colors = sa.getParam("Colors");
            if (colors.equals("ChosenColor")) {

                tmpDesc = CardUtil.getShortColorsString(source.getChosenColors());
            } else {
                tmpDesc = CardUtil.getShortColorsString(Arrays.asList(colors.split(",")));
            }
        }
        final String finalDesc = tmpDesc;

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
        final List<String> sVars = Lists.newArrayList();
        if (sa.hasParam("sVars")) {
            sVars.addAll(Arrays.asList(sa.getParam("sVars").split(",")));
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
            doAnimate(c, sa, power, toughness, types, removeTypes, finalDesc,
                    keywords, removeKeywords, hiddenKeywords,
                    abilities, triggers, replacements, stAbs, timestamp);

            if (sa.hasParam("Name")) {
                c.addChangedName(sa.getParam("Name"), timestamp);
            }

            // give sVars
            for (final String s : sVars) {
                String actualsVar = AbilityUtils.getSVar(sa, s);
                String name = s;
                if (actualsVar.startsWith("SVar:")) {
                    actualsVar = actualsVar.split("SVar:")[1];
                    name = actualsVar.split(":")[0];
                    actualsVar = actualsVar.split(":")[1];
                }
                c.setSVar(name, actualsVar);
            }

            // give Remembered
            if (animateRemembered != null) {
                for (final Object o : AbilityUtils.getDefinedObjects(source, animateRemembered, sa)) {
                    c.addRemembered(o);
                }
            }

            // give Imprinted
            if (animateImprinted != null) {
                for (final Card imprintedCard : AbilityUtils.getDefinedCards(source, animateImprinted, sa)) {
                    c.addImprintedCard(imprintedCard);
                }
            }

            // Restore immutable to effect
            if (sa.hasParam("Immutable")) {
                c.setImmutable(true);
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

        final StringBuilder sb = new StringBuilder();

        final List<Card> tgts = getCardsfromTargets(sa);

        sb.append(Lang.joinHomogenous(tgts)).append(" ");

        // if power is -1, we'll assume it's not just setting toughness
        if (power != null && toughness != null) {
            sb.append("become");
            if (tgts.size() == 1) {
                sb.append("s ");
            }
            sb.append(" ").append(power).append("/").append(toughness);
        } else if (power != null) {
            sb.append("power becomes ").append(power);
        } else if (toughness != null) {
            sb.append("toughness becomes ").append(toughness);
        } else{
            sb.append("become");
            if (tgts.size() == 1) {
                sb.append("s ");
            }
        }

        if (colors.size() > 0) {
            sb.append(" ");
        }
        if (colors.contains("ChosenColor")) {
            sb.append("color of that player's choice");
        } else {
            for (int i = 0; i < colors.size(); i++) {
                sb.append(colors.get(i));
                if (i < (colors.size() - 1)) {
                    sb.append(" and ");
                }
            }
        }
        sb.append(" ");
        if (types.contains("ChosenType")) {
            sb.append("type of player's choice ");
        } else {
            for (int i = types.size() - 1; i >= 0; i--) {
                sb.append(types.get(i));
                sb.append(" ");
            }
        }
        if (keywords.size() > 0) {
            sb.append("with ");
        }
        for (int i = 0; i < keywords.size(); i++) {
            sb.append(keywords.get(i));
            if (i < (keywords.size() - 1)) {
                sb.append(" and ");
            }
        }
        // sb.append(abilities)
        // sb.append(triggers)
        if (!permanent) {
            final String duration = sa.getParam("Duration");
            if ("UntilEndOfCombat".equals(duration)) {
                sb.append(" until end of combat.");
            } else if ("UntilHostLeavesPlay".equals(duration)) {
                sb.append(" until ").append(host).append(" leaves the battlefield.");
            } else if ("UntilYourNextUpkeep".equals(duration)) {
                sb.append(" until your next upkeep.");
            } else if ("UntilYourNextTurn".equals(duration)) {
                sb.append(" until your next turn.");
            } else if ("UntilControllerNextUntap".equals(duration)) {
                sb.append(" until its controller's next untap step.");
            } else {
                sb.append(" until end of turn.");
            }
        } else {
            sb.append(".");
        }


        return sb.toString();
    }

}
