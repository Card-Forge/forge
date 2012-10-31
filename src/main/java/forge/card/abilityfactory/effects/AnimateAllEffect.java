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
import forge.card.replacement.ReplacementEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class AnimateAllEffect extends AnimateEffectBase 
{
    /**
     * <p>
     * animateAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        String desc = "";
        if (params.containsKey("SpellDescription")) {
            desc = params.get("SpellDescription");
        } else {
            desc = "Animate all valid cards.";
        }

        sb.append(desc);
        return sb.toString();
    }

    @Override
    public void resolve(final java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Map<String, String> svars = host.getSVars();
        long timest = -1;
    
        // AF specific params
        int power = -1;
        if (params.containsKey("Power")) {
            power = AbilityFactory.calculateAmount(host, params.get("Power"), sa);
        }
        int toughness = -1;
        if (params.containsKey("Toughness")) {
            toughness = AbilityFactory.calculateAmount(host, params.get("Toughness"), sa);
        }
    
        // Every Animate event needs a unique time stamp
        timest = Singletons.getModel().getGame().getNextTimestamp();
    
        final long timestamp = timest;
    
        final boolean permanent = params.containsKey("Permanent");
    
        final ArrayList<String> types = new ArrayList<String>();
        if (params.containsKey("Types")) {
            types.addAll(Arrays.asList(params.get("Types").split(",")));
        }
    
        final ArrayList<String> removeTypes = new ArrayList<String>();
        if (params.containsKey("RemoveTypes")) {
            removeTypes.addAll(Arrays.asList(params.get("RemoveTypes").split(",")));
        }
    
        // allow ChosenType - overrides anything else specified
        if (types.contains("ChosenType")) {
            types.clear();
            types.add(host.getChosenType());
        }
    
        final ArrayList<String> keywords = new ArrayList<String>();
        if (params.containsKey("Keywords")) {
            keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
        }
    
        final ArrayList<String> hiddenKeywords = new ArrayList<String>();
        if (params.containsKey("HiddenKeywords")) {
            hiddenKeywords.addAll(Arrays.asList(params.get("HiddenKeywords").split(" & ")));
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
        if (params.containsKey("Colors")) {
            final String colors = params.get("Colors");
            if (colors.equals("ChosenColor")) {
                tmpDesc = CardUtil.getShortColorsString(host.getChosenColor());
            } else {
                tmpDesc = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(colors.split(","))));
            }
        }
        final String finalDesc = tmpDesc;
    
        // abilities to add to the animated being
        final ArrayList<String> abilities = new ArrayList<String>();
        if (params.containsKey("Abilities")) {
            abilities.addAll(Arrays.asList(params.get("Abilities").split(",")));
        }
    
        // triggers to add to the animated being
        final ArrayList<String> triggers = new ArrayList<String>();
        if (params.containsKey("Triggers")) {
            triggers.addAll(Arrays.asList(params.get("Triggers").split(",")));
        }
    
        // sVars to add to the animated being
        final ArrayList<String> sVars = new ArrayList<String>();
        if (params.containsKey("sVars")) {
            sVars.addAll(Arrays.asList(params.get("sVars").split(",")));
        }
    
        String valid = "";
    
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }
    
        List<Card> list;
        ArrayList<Player> tgtPlayers = null;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (params.containsKey("Defined")) {
            // use it
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        if ((tgtPlayers == null) || tgtPlayers.isEmpty()) {
            list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        } else {
            list = new ArrayList<Card>(tgtPlayers.get(0).getCardsIn(ZoneType.Battlefield));
        }
    
        list = CardLists.getValidCards(list, valid.split(","), host.getController(), host);
    
        for (final Card c : list) {
    
            final long colorTimestamp = doAnimate(c, params, power, toughness, types, removeTypes,
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
            if (params.containsKey("OverwriteAbilities") || params.containsKey("RemoveAllAbilities")) {
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
            if (params.containsKey("OverwriteTriggers") || params.containsKey("RemoveAllAbilities")) {
                final List<Trigger> triggersToRemove = c.getTriggers();
                for (final Trigger trigger : triggersToRemove) {
                    trigger.setSuppressed(true);
                    removedTriggers.add(trigger);
                }
            }
    
            // suppress static abilities from the animated card
            final ArrayList<StaticAbility> removedStatics = new ArrayList<StaticAbility>();
            if (params.containsKey("OverwriteStatics") || params.containsKey("RemoveAllAbilities")) {
                final ArrayList<StaticAbility> staticsToRemove = c.getStaticAbilities();
                for (final StaticAbility stAb : staticsToRemove) {
                    stAb.setTemporarilySuppressed(true);
                    removedStatics.add(stAb);
                }
            }
    
            // suppress static abilities from the animated card
            final ArrayList<ReplacementEffect> removedReplacements = new ArrayList<ReplacementEffect>();
            if (params.containsKey("OverwriteReplacements") || params.containsKey("RemoveAllAbilities")) {
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
                    doUnanimate(c, params, finalDesc, hiddenKeywords, addedAbilities, addedTriggers,
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
                if (params.containsKey("UntilEndOfCombat")) {
                    Singletons.getModel().getGame().getEndOfCombat().addUntil(unanimate);
                } else {
                    Singletons.getModel().getGame().getEndOfTurn().addUntil(unanimate);
                }
            }
        }
    } // animateAllResolve

}