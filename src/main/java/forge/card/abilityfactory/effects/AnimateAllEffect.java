package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
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
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class AnimateAllEffect extends AnimateEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Animate all valid cards.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Map<String, String> svars = host.getSVars();
        long timest = -1;

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

        // sVars to add to the animated being
        final ArrayList<String> sVars = new ArrayList<String>();
        if (sa.hasParam("sVars")) {
            sVars.addAll(Arrays.asList(sa.getParam("sVars").split(",")));
        }

        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        List<Card> list;
        List<Player> tgtPlayers = null;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (sa.hasParam("Defined")) {
            // use it
            tgtPlayers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }

        if ((tgtPlayers == null) || tgtPlayers.isEmpty()) {
            list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        } else {
            list = new ArrayList<Card>(tgtPlayers.get(0).getCardsIn(ZoneType.Battlefield));
        }

        list = CardLists.getValidCards(list, valid.split(","), host.getController(), host);

        for (final Card c : list) {

            final long colorTimestamp = doAnimate(c, sa, power, toughness, types, removeTypes,
                    finalDesc, keywords, null, hiddenKeywords, timestamp);

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

            // remove abilities
            final ArrayList<SpellAbility> removedAbilities = new ArrayList<SpellAbility>();
            if (sa.hasParam("OverwriteAbilities") || sa.hasParam("RemoveAllAbilities")) {
                for (final SpellAbility ab : c.getSpellAbilities()) {
                    if (ab.isAbility()) {
                        c.removeSpellAbility(ab);
                        removedAbilities.add(ab);
                    }
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
            if (sa.hasParam("OverwriteTriggers") || sa.hasParam("RemoveAllAbilities")) {
                final List<Trigger> triggersToRemove = c.getTriggers();
                for (final Trigger trigger : triggersToRemove) {
                    trigger.setSuppressed(true);
                    removedTriggers.add(trigger);
                }
            }

            // suppress static abilities from the animated card
            final ArrayList<StaticAbility> removedStatics = new ArrayList<StaticAbility>();
            if (sa.hasParam("OverwriteStatics") || sa.hasParam("RemoveAllAbilities")) {
                final ArrayList<StaticAbility> staticsToRemove = c.getStaticAbilities();
                for (final StaticAbility stAb : staticsToRemove) {
                    stAb.setTemporarilySuppressed(true);
                    removedStatics.add(stAb);
                }
            }

            // suppress static abilities from the animated card
            final ArrayList<ReplacementEffect> removedReplacements = new ArrayList<ReplacementEffect>();
            if (sa.hasParam("OverwriteReplacements") || sa.hasParam("RemoveAllAbilities")) {
                final ArrayList<ReplacementEffect> replacementsToRemove = c.getReplacementEffects();
                for (final ReplacementEffect re : replacementsToRemove) {
                    re.setTemporarilySuppressed(true);
                    removedReplacements.add(re);
                }
            }

            // give sVars
            if (sVars.size() > 0) {
                for (final String s : sVars) {
                    final String actualsVar = host.getSVar(s);
                    c.setSVar(s, actualsVar);
                }
            }

            final Command unanimate = new Command() {
                private static final long serialVersionUID = -5861759814760561373L;

                @Override
                public void execute() {
                    doUnanimate(c, sa, finalDesc, hiddenKeywords, addedAbilities, addedTriggers,
                            colorTimestamp, false, removedAbilities, timestamp);

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
                } else {
                    Singletons.getModel().getGame().getEndOfTurn().addUntil(unanimate);
                }
            }
        }
    } // animateAllResolve

}
