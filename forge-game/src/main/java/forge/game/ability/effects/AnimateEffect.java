package forge.game.ability.effects;

import forge.GameCommand;
import forge.card.CardType;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.phase.PhaseType;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.util.collect.FCollectionView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AnimateEffect extends AnimateEffectBase {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Map<String, String> svars = source.getSVars();

        String animateRemembered = null;

        //if host is not on the battlefield don't apply
        if (sa.hasParam("UntilHostLeavesPlay")
                && !sa.getHostCard().isInPlay()) {
            return;
        }

        // Remember Objects
        if (sa.hasParam("RememberObjects")) {
            animateRemembered = sa.getParam("RememberObjects");
        }

        // AF specific sa
        int power = -1;
        if (sa.hasParam("Power")) {
            power = AbilityUtils.calculateAmount(source, sa.getParam("Power"), sa);
        }
        int toughness = -1;
        if (sa.hasParam("Toughness")) {
            toughness = AbilityUtils.calculateAmount(source, sa.getParam("Toughness"), sa);
        }

        final Game game = sa.getActivatingPlayer().getGame();
        // Every Animate event needs a unique time stamp
        final long timestamp = game.getNextTimestamp();

        final boolean permanent = sa.hasParam("Permanent");

        final CardType types = new CardType();
        if (sa.hasParam("Types")) {
            types.addAll(Arrays.asList(sa.getParam("Types").split(",")));
        }

        final CardType removeTypes = new CardType();
        if (sa.hasParam("RemoveTypes")) {
            removeTypes.addAll(Arrays.asList(sa.getParam("RemoveTypes").split(",")));
        }

        // allow ChosenType - overrides anything else specified
        if (types.hasSubtype("ChosenType")) {
            types.clear();
            types.add(source.getChosenType());
        }

        final List<String> keywords = new ArrayList<String>();
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }

        final List<String> removeKeywords = new ArrayList<String>();
        if (sa.hasParam("RemoveKeywords")) {
            removeKeywords.addAll(Arrays.asList(sa.getParam("RemoveKeywords").split(" & ")));
        }

        final List<String> hiddenKeywords = new ArrayList<String>();
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

                tmpDesc = CardUtil.getShortColorsString(source.getChosenColors());
            } else {
                tmpDesc = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(colors.split(","))));
            }
        }
        final String finalDesc = tmpDesc;

        // abilities to add to the animated being
        final List<String> abilities = new ArrayList<String>();
        if (sa.hasParam("Abilities")) {
            abilities.addAll(Arrays.asList(sa.getParam("Abilities").split(",")));
        }

        // replacement effects to add to the animated being
        final List<String> replacements = new ArrayList<String>();
        if (sa.hasParam("Replacements")) {
            replacements.addAll(Arrays.asList(sa.getParam("Replacements").split(",")));
        }

        // triggers to add to the animated being
        final List<String> triggers = new ArrayList<String>();
        if (sa.hasParam("Triggers")) {
            triggers.addAll(Arrays.asList(sa.getParam("Triggers").split(",")));
        }

        // static abilities to add to the animated being
        final List<String> stAbs = new ArrayList<String>();
        if (sa.hasParam("staticAbilities")) {
            stAbs.addAll(Arrays.asList(sa.getParam("staticAbilities").split(",")));
        }

        // sVars to add to the animated being
        final List<String> sVars = new ArrayList<String>();
        if (sa.hasParam("sVars")) {
            sVars.addAll(Arrays.asList(sa.getParam("sVars").split(",")));
        }

        List<Card> tgts = getTargetCards(sa);

        for (final Card c : tgts) {
            doAnimate(c, sa, power, toughness, types, removeTypes, finalDesc,
                    keywords, removeKeywords, hiddenKeywords, timestamp);

            // remove abilities
            final List<SpellAbility> removedAbilities = new ArrayList<SpellAbility>();
            boolean clearAbilities = sa.hasParam("OverwriteAbilities");
            boolean clearSpells = sa.hasParam("OverwriteSpells");
            boolean removeAll = sa.hasParam("RemoveAllAbilities");

            if (clearAbilities || clearSpells || removeAll) {
                for (final SpellAbility ab : c.getSpellAbilities()) {
                    if (removeAll || (ab.isAbility() && clearAbilities)
                            || (ab.isSpell() && clearSpells)) {
                        removedAbilities.add(ab);
                    }
                }
            }

            // Can't rmeove SAs in foreach loop that finds them
            for (final SpellAbility ab : removedAbilities) {
                    c.removeSpellAbility(ab);
            }

            if (sa.hasParam("RemoveThisAbility") && !removedAbilities.contains(sa)) {
                c.removeSpellAbility(sa);
                removedAbilities.add(sa);
            }

            // give abilities
            final List<SpellAbility> addedAbilities = new ArrayList<SpellAbility>();
            if (abilities.size() > 0) {
                for (final String s : abilities) {
                    final String actualAbility = source.getSVar(s);
                    final SpellAbility grantedAbility = AbilityFactory.getAbility(actualAbility, c);
                    addedAbilities.add(grantedAbility);
                    c.addSpellAbility(grantedAbility);
                }
            }

            // Grant triggers
            final List<Trigger> addedTriggers = new ArrayList<Trigger>();
            if (triggers.size() > 0) {
                for (final String s : triggers) {
                    final String actualTrigger = source.getSVar(s);
                    final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, c, false);
                    addedTriggers.add(c.addTrigger(parsedTrigger));
                }
            }

            // give replacement effects
            final List<ReplacementEffect> addedReplacements = new ArrayList<ReplacementEffect>();
            if (replacements.size() > 0) {
                for (final String s : replacements) {
                    final String actualReplacement = source.getSVar(s);
                    final ReplacementEffect parsedReplacement = ReplacementHandler.parseReplacement(actualReplacement, c, false);
                    addedReplacements.add(c.addReplacementEffect(parsedReplacement));
                }
            }

            // suppress triggers from the animated card
            final List<Trigger> removedTriggers = new ArrayList<Trigger>();
            if (sa.hasParam("OverwriteTriggers") || removeAll) {
                final FCollectionView<Trigger> triggersToRemove = c.getTriggers();
                for (final Trigger trigger : triggersToRemove) {
                    trigger.setSuppressed(true);
                    removedTriggers.add(trigger);
                }
            }

            // give static abilities (should only be used by cards to give
            // itself a static ability)
            if (stAbs.size() > 0) {
                for (final String s : stAbs) {
                    final String actualAbility = source.getSVar(s);
                    c.addStaticAbility(actualAbility);
                }
            }

            // give sVars
            if (sVars.size() > 0) {
                for (final String s : sVars) {
                    String actualsVar = source.getSVar(s);
                    String name = s;
                    if (actualsVar.startsWith("SVar:")) {
                        actualsVar = actualsVar.split("SVar:")[1];
                        name = actualsVar.split(":")[0];
                        actualsVar = actualsVar.split(":")[1];
                    }
                    c.setSVar(name, actualsVar);
                }
            }

            // suppress static abilities from the animated card
            final List<StaticAbility> removedStatics = new ArrayList<StaticAbility>();
            if (sa.hasParam("OverwriteStatics") || removeAll) {
                final FCollectionView<StaticAbility> staticsToRemove = c.getStaticAbilities();
                for (final StaticAbility stAb : staticsToRemove) {
                    stAb.setTemporarilySuppressed(true);
                    removedStatics.add(stAb);
                }
            }

            // suppress static abilities from the animated card
            final List<ReplacementEffect> removedReplacements = new ArrayList<ReplacementEffect>();
            if (sa.hasParam("OverwriteReplacements") || removeAll) {
                for (final ReplacementEffect re : c.getReplacementEffects()) {
                    re.setTemporarilySuppressed(true);
                    removedReplacements.add(re);
                }
            }

            // give Remembered
            if (animateRemembered != null) {
                for (final Object o : AbilityUtils.getDefinedObjects(source, animateRemembered, sa)) {
                    c.addRemembered(o);
                }
            }

            final boolean givesStAbs = (stAbs.size() > 0);

            final GameCommand unanimate = new GameCommand() {
                private static final long serialVersionUID = -5861759814760561373L;

                @Override
                public void run() {
                    doUnanimate(c, sa, finalDesc, hiddenKeywords,
                            addedAbilities, addedTriggers, addedReplacements,
                            givesStAbs, removedAbilities, timestamp);

                    game.fireEvent(new GameEventCardStatsChanged(c));
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
                    game.getEndOfCombat().addUntil(unanimate);
                } else if (sa.hasParam("UntilHostLeavesPlay")) {
                    source.addLeavesPlayCommand(unanimate);
                } else if (sa.hasParam("UntilYourNextUpkeep")) {
                    game.getUpkeep().addUntil(source.getController(), unanimate);
                } else if (sa.hasParam("UntilTheEndOfYourNextUpkeep")) {
                    if (game.getPhaseHandler().is(PhaseType.UPKEEP)) {
                        game.getUpkeep().registerUntilEnd(source.getController(), unanimate);
                    } else {
                        game.getUpkeep().addUntilEnd(source.getController(), unanimate);
                    }
                } else if (sa.hasParam("UntilControllerNextUntap")) {
                    game.getUntap().addUntil(c.getController(), unanimate);
                } else if (sa.hasParam("UntilAPlayerCastSpell")) {
                    game.getStack().addCastCommand(sa.getParam("UntilAPlayerCastSpell"), unanimate);
                } else if (sa.hasParam("UntilYourNextTurn")) {
                    game.getCleanup().addUntil(source.getController(), unanimate);
                } else {
                    game.getEndOfTurn().addUntil(unanimate);
                }
            }

            if (sa.hasParam("RevertCost")) {
            	final ManaCost cost = new ManaCost(new ManaCostParser(sa.getParam("RevertCost")));
            	final String desc = this.getStackDescription(sa);
                final SpellAbility revertSA = new AbilityStatic(c, cost) {
                    @Override
                    public void resolve() {
                        unanimate.run();
                        c.removeSpellAbility(this);
                    }
                    @Override
                    public String getDescription() {
                        return cost + ": End Effect: " + desc;
                    }
                };
                c.addSpellAbility(revertSA);
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
        final Map<String, String> svars = host.getSVars();

        Integer power = null;
        if (sa.hasParam("Power")) {
            power = AbilityUtils.calculateAmount(host, sa.getParam("Power"), sa);
        }
        Integer toughness = null;
        if (sa.hasParam("Toughness")) {
            toughness = AbilityUtils.calculateAmount(host, sa.getParam("Toughness"), sa);
        }

        final boolean permanent = sa.hasParam("Permanent");
        final List<String> types = new ArrayList<String>();
        if (sa.hasParam("Types")) {
            types.addAll(Arrays.asList(sa.getParam("Types").split(",")));
        }
        final List<String> keywords = new ArrayList<String>();
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
        final List<String> colors = new ArrayList<String>();
        if (sa.hasParam("Colors")) {
            colors.addAll(Arrays.asList(sa.getParam("Colors").split(",")));
        }

        final StringBuilder sb = new StringBuilder();

        final List<Card> tgts = getTargetCards(sa);

        for (final Card c : tgts) {
            sb.append(c).append(" ");
        }

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
