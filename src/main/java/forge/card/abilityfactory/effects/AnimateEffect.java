package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardUtil;
import forge.Command;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.replacement.ReplacementEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

public class AnimateEffect extends AnimateEffectBase {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getSourceCard();
        final Card host = sa.getSourceCard();
        final Map<String, String> svars = host.getSVars();
        long timest = -1;
        String animateRemembered = null;

        //if host is not on the battlefield don't apply
        if (sa.hasParam("UntilHostLeavesPlay")
                && !sa.getSourceCard().isInPlay()) {
            return;
        }

        // Remember Objects
        if (sa.hasParam("RememberObjects")) {
            animateRemembered = sa.getParam("RememberObjects");
        }

        // AF specific sa
        int power = -1;
        if (sa.hasParam("Power")) {
            power = AbilityUtils.calculateAmount(host, sa.getParam("Power"), sa);
        }
        int toughness = -1;
        if (sa.hasParam("Toughness")) {
            toughness = AbilityUtils.calculateAmount(host, sa.getParam("Toughness"), sa);
        }

        // Every Animate event needs a unique time stamp
        timest = Singletons.getModel().getGame().getNextTimestamp();

        final long timestamp = timest;

        final boolean permanent = sa.hasParam("Permanent");

        final ArrayList<String> types = new ArrayList<String>();
        if (sa.hasParam("Types")) {
            types.addAll(Arrays.asList(sa.getParam("Types").split(",")));
        }

        final ArrayList<String> removeTypes = new ArrayList<String>();
        if (sa.hasParam("RemoveTypes")) {
            removeTypes.addAll(Arrays.asList(sa.getParam("RemoveTypes").split(",")));
        }

        // allow ChosenType - overrides anything else specified
        if (types.contains("ChosenType")) {
            types.clear();
            types.add(host.getChosenType());
        }

        final ArrayList<String> keywords = new ArrayList<String>();
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }

        final ArrayList<String> removeKeywords = new ArrayList<String>();
        if (sa.hasParam("RemoveKeywords")) {
            removeKeywords.addAll(Arrays.asList(sa.getParam("RemoveKeywords").split(" & ")));
        }

        final ArrayList<String> hiddenKeywords = new ArrayList<String>();
        if (sa.hasParam("HiddenKeywords")) {
            hiddenKeywords.addAll(Arrays.asList(sa.getParam("HiddenKeywords").split(" & ")));
        }
        // allow SVar substitution for keywords
        for (int i = 0; i < keywords.size(); i++) {
            final String k = keywords.get(i);
            if (svars.containsKey(k)) {
                keywords.add(svars.get(k));
                keywords.remove(k);
            }
        }

        // colors to be added or changed to
        String tmpDesc = "";
        if (sa.hasParam("Colors")) {
            final String colors = sa.getParam("Colors");
            if (colors.equals("ChosenColor")) {

                tmpDesc = CardUtil.getShortColorsString(host.getChosenColor());
            } else {
                tmpDesc = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(colors.split(","))));
            }
        }
        final String finalDesc = tmpDesc;

        // abilities to add to the animated being
        final ArrayList<String> abilities = new ArrayList<String>();
        if (sa.hasParam("Abilities")) {
            abilities.addAll(Arrays.asList(sa.getParam("Abilities").split(",")));
        }

        // triggers to add to the animated being
        final ArrayList<String> triggers = new ArrayList<String>();
        if (sa.hasParam("Triggers")) {
            triggers.addAll(Arrays.asList(sa.getParam("Triggers").split(",")));
        }

        // static abilities to add to the animated being
        final ArrayList<String> stAbs = new ArrayList<String>();
        if (sa.hasParam("staticAbilities")) {
            stAbs.addAll(Arrays.asList(sa.getParam("staticAbilities").split(",")));
        }

        // sVars to add to the animated being
        final ArrayList<String> sVars = new ArrayList<String>();
        if (sa.hasParam("sVars")) {
            sVars.addAll(Arrays.asList(sa.getParam("sVars").split(",")));
        }

        final Target tgt = sa.getTarget();
        List<Card> tgts = tgt != null ? tgts = tgt.getTargetCards() : AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);

        for (final Card c : tgts) {

            final long colorTimestamp = doAnimate(c, sa, power, toughness, types, removeTypes,
                    finalDesc, keywords, removeKeywords, hiddenKeywords, timestamp);

            // remove abilities
            final ArrayList<SpellAbility> removedAbilities = new ArrayList<SpellAbility>();
            boolean clearAbilities = sa.hasParam("OverwriteAbilities");
            boolean clearSpells = sa.hasParam("OverwriteSpells");
            boolean removeAll = sa.hasParam("RemoveAllAbilities");

            if (clearAbilities || clearSpells || removeAll) {
                for (final SpellAbility ab : c.getSpellAbilities()) {
                    if (removeAll || (ab.isAbility() && clearAbilities)
                            || (ab.isSpell() && clearSpells)) {
                        c.removeSpellAbility(ab);
                        removedAbilities.add(ab);
                    }
                }
            }

            // give abilities
            final ArrayList<SpellAbility> addedAbilities = new ArrayList<SpellAbility>();
            if (abilities.size() > 0) {
                for (final String s : abilities) {
                    final AbilityFactory newAF = new AbilityFactory();
                    final String actualAbility = host.getSVar(s);
                    final SpellAbility grantedAbility = newAF.getAbility(actualAbility, c);
                    addedAbilities.add(grantedAbility);
                    c.addSpellAbility(grantedAbility);
                }
            }

            // Grant triggers
            final ArrayList<Trigger> addedTriggers = new ArrayList<Trigger>();
            if (triggers.size() > 0) {
                for (final String s : triggers) {
                    final String actualTrigger = host.getSVar(s);
                    final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, c, false);
                    addedTriggers.add(c.addTrigger(parsedTrigger));
                }
            }

            // suppress triggers from the animated card
            final ArrayList<Trigger> removedTriggers = new ArrayList<Trigger>();
            if (sa.hasParam("OverwriteTriggers") || removeAll) {
                final List<Trigger> triggersToRemove = c.getTriggers();
                for (final Trigger trigger : triggersToRemove) {
                    trigger.setSuppressed(true);
                    removedTriggers.add(trigger);
                }
            }

            // give static abilities (should only be used by cards to give
            // itself a static ability)
            if (stAbs.size() > 0) {
                for (final String s : stAbs) {
                    final String actualAbility = host.getSVar(s);
                    c.addStaticAbility(actualAbility);
                }
            }

            // give sVars
            if (sVars.size() > 0) {
                for (final String s : sVars) {
                    final String actualsVar = host.getSVar(s);
                    c.setSVar(s, actualsVar);
                }
            }

            // suppress static abilities from the animated card
            final ArrayList<StaticAbility> removedStatics = new ArrayList<StaticAbility>();
            if (sa.hasParam("OverwriteStatics") || removeAll) {
                final ArrayList<StaticAbility> staticsToRemove = c.getStaticAbilities();
                for (final StaticAbility stAb : staticsToRemove) {
                    stAb.setTemporarilySuppressed(true);
                    removedStatics.add(stAb);
                }
            }

            // suppress static abilities from the animated card
            final ArrayList<ReplacementEffect> removedReplacements = new ArrayList<ReplacementEffect>();
            if (sa.hasParam("OverwriteReplacements") || removeAll) {
                final ArrayList<ReplacementEffect> replacementsToRemove = c.getReplacementEffects();
                for (final ReplacementEffect re : replacementsToRemove) {
                    re.setTemporarilySuppressed(true);
                    removedReplacements.add(re);
                }
            }

            // give Remembered
            if (animateRemembered != null) {
                for (final Object o : AbilityUtils.getDefinedObjects(host, animateRemembered, sa)) {
                    c.addRemembered(o);
                }
            }

            final boolean givesStAbs = (stAbs.size() > 0);

            final Command unanimate = new Command() {
                private static final long serialVersionUID = -5861759814760561373L;

                @Override
                public void execute() {
                    doUnanimate(c, sa, finalDesc, hiddenKeywords, addedAbilities, addedTriggers,
                            colorTimestamp, givesStAbs, removedAbilities, timestamp);

                    // give back suppressed triggers
                    for (final Trigger t : removedTriggers) {
                        t.setSuppressed(false);
                    }

                    // give back suppressed static abilities
                    for (final StaticAbility s : removedStatics) {
                        s.setTemporarilySuppressed(false);
                    }

                    // give back suppressed replacement effects
                    for (final ReplacementEffect re : removedReplacements) {
                        re.setTemporarilySuppressed(false);
                    }
                }
            };

            if (!permanent) {
                if (sa.hasParam("UntilEndOfCombat")) {
                    Singletons.getModel().getGame().getEndOfCombat().addUntil(unanimate);
                } else if (sa.hasParam("UntilHostLeavesPlay")) {
                    host.addLeavesPlayCommand(unanimate);
                } else if (sa.hasParam("UntilYourNextUpkeep")) {
                    Singletons.getModel().getGame().getUpkeep().addUntil(host.getController(), unanimate);
                } else if (sa.hasParam("UntilControllerNextUntap")) {
                    Singletons.getModel().getGame().getUntap().addUntil(c.getController(), unanimate);
                } else if (sa.hasParam("UntilYourNextTurn")) {
                    Singletons.getModel().getGame().getCleanup().addUntilYourNextTurn(host.getController(), unanimate);
                } else {
                    Singletons.getModel().getGame().getEndOfTurn().addUntil(unanimate);
                }
            }
        }
    } // animateResolve extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Map<String, String> svars = host.getSVars();

        int power = -1;
        if (sa.hasParam("Power")) {
            power = AbilityUtils.calculateAmount(host, sa.getParam("Power"), sa);
        }
        int toughness = -1;
        if (sa.hasParam("Toughness")) {
            toughness = AbilityUtils.calculateAmount(host, sa.getParam("Toughness"), sa);
        }

        final boolean permanent = sa.hasParam("Permanent");
        final ArrayList<String> types = new ArrayList<String>();
        if (sa.hasParam("Types")) {
            types.addAll(Arrays.asList(sa.getParam("Types").split(",")));
        }
        final ArrayList<String> keywords = new ArrayList<String>();
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }
        // allow SVar substitution for keywords
        for (int i = 0; i < keywords.size(); i++) {
            final String k = keywords.get(i);
            if (svars.containsKey(k)) {
                keywords.add("\"" + k + "\"");
                keywords.remove(k);
            }
        }
        final ArrayList<String> colors = new ArrayList<String>();
        if (sa.hasParam("Colors")) {
            colors.addAll(Arrays.asList(sa.getParam("Colors").split(",")));
        }

        final StringBuilder sb = new StringBuilder();

        final Target tgt = sa.getTarget();
        final List<Card> tgts = tgt != null ? tgt.getTargetCards() :  AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);

        for (final Card c : tgts) {
            sb.append(c).append(" ");
        }
        sb.append("become");
        if (tgts.size() == 1) {
            sb.append("s a");
        }
        // if power is -1, we'll assume it's not just setting toughness
        if (power != -1) {
            sb.append(" ").append(power).append("/").append(toughness);
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
            if (sa.hasParam("UntilEndOfCombat")) {
                sb.append(" until end of combat.");
            } else if (sa.hasParam("UntilHostLeavesPlay")) {
                sb.append(" until ").append(host).append(" leaves the battlefield.");
            } else if (sa.hasParam("UntilYourNextUpkeep")) {
                sb.append(" until your next upkeep.");
            } else if (sa.hasParam("UntilControllerNextUntap")) {
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
