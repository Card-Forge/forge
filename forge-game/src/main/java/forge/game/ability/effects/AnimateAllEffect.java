package forge.game.ability.effects;

import forge.GameCommand;
import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AnimateAllEffect extends AnimateEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Animate all valid cards.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Map<String, String> svars = host.getSVars();

        // AF specific sa
        Integer power = null;
        if (sa.hasParam("Power")) {
            power = AbilityUtils.calculateAmount(host, sa.getParam("Power"), sa);
        }
        Integer toughness = null;
        if (sa.hasParam("Toughness")) {
            toughness = AbilityUtils.calculateAmount(host, sa.getParam("Toughness"), sa);
        }
        final Game game = sa.getActivatingPlayer().getGame();

        // Every Animate event needs a unique time stamp
        final long timestamp = game.getNextTimestamp();

        final boolean permanent = sa.hasParam("Permanent");

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
            types.add(host.getChosenType());
        }

        final List<String> keywords = new ArrayList<>();
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }

        final List<String> removeKeywords = new ArrayList<>();
        if (sa.hasParam("RemoveKeywords")) {
            removeKeywords.addAll(Arrays.asList(sa.getParam("RemoveKeywords").split(" & ")));
        }

        final List<String> hiddenKeywords = new ArrayList<>();
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
                tmpDesc = CardUtil.getShortColorsString(host.getChosenColors());
            } else {
                tmpDesc = CardUtil.getShortColorsString(new ArrayList<>(Arrays.asList(colors.split(","))));
            }
        }
        final String finalDesc = tmpDesc;

        // abilities to add to the animated being
        final List<String> abilities = new ArrayList<>();
        if (sa.hasParam("Abilities")) {
            abilities.addAll(Arrays.asList(sa.getParam("Abilities").split(",")));
        }
        // replacement effects to add to the animated being
        final List<String> replacements = new ArrayList<>();
        if (sa.hasParam("Replacements")) {
            replacements.addAll(Arrays.asList(sa.getParam("Replacements").split(",")));
        }
        // triggers to add to the animated being
        final List<String> triggers = new ArrayList<>();
        if (sa.hasParam("Triggers")) {
            triggers.addAll(Arrays.asList(sa.getParam("Triggers").split(",")));
        }

        // sVars to add to the animated being
        final List<String> sVars = new ArrayList<>();
        if (sa.hasParam("sVars")) {
            sVars.addAll(Arrays.asList(sa.getParam("sVars").split(",")));
        }

        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        CardCollectionView list;
        List<Player> tgtPlayers = getTargetPlayers(sa);
        
        if (!sa.usesTargeting() && !sa.hasParam("Defined")) {
            list = game.getCardsIn(ZoneType.Battlefield);
        }
        else {
            list = tgtPlayers.get(0).getCardsIn(ZoneType.Battlefield);
        }

        list = CardLists.getValidCards(list, valid.split(","), host.getController(), host, sa);

        boolean removeAll = sa.hasParam("RemoveAllAbilities");
        boolean removeIntrinsic = sa.hasParam("RemoveIntrinsicAbilities");

        for (final Card c : list) {
            doAnimate(c, sa, power, toughness, types, removeTypes, finalDesc,
                    keywords, removeKeywords, hiddenKeywords, timestamp);

            // give abilities
            final List<SpellAbility> addedAbilities = new ArrayList<>();
            if (abilities.size() > 0) {
                for (final String s : abilities) {
                    final String actualAbility = host.getSVar(s);
                    final SpellAbility grantedAbility = AbilityFactory.getAbility(actualAbility, c);
                    addedAbilities.add(grantedAbility);
                }
            }

            // give replacement effects
            final List<ReplacementEffect> addedReplacements = new ArrayList<>();
            if (replacements.size() > 0) {
                for (final String s : replacements) {
                    final String actualReplacement = host.getSVar(s);
                    final ReplacementEffect parsedReplacement = ReplacementHandler.parseReplacement(actualReplacement, c, false);
                    addedReplacements.add(parsedReplacement);
                }
            }
            // Grant triggers
            final List<Trigger> addedTriggers = new ArrayList<>();
            if (triggers.size() > 0) {
                for (final String s : triggers) {
                    final String actualTrigger = host.getSVar(s);
                    final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, c, false);
                    addedTriggers.add(parsedTrigger);
                }
            }

            if (!addedAbilities.isEmpty() || !addedTriggers.isEmpty() || !addedReplacements.isEmpty()) {
                c.addChangedCardTraits(addedAbilities, null, addedTriggers, addedReplacements, null, removeAll, false, removeIntrinsic, timestamp);
            }

            // give sVars
            if (sVars.size() > 0) {
                for (final String s : sVars) {
                    final String actualsVar = host.getSVar(s);
                    c.setSVar(s, actualsVar);
                }
            }
            game.fireEvent(new GameEventCardStatsChanged(c));

            final GameCommand unanimate = new GameCommand() {
                private static final long serialVersionUID = -5861759814760561373L;

                @Override
                public void run() {
                    doUnanimate(c, sa, hiddenKeywords, timestamp);

                    game.fireEvent(new GameEventCardStatsChanged(c));
                }
            };

            if (!permanent) {
                if (sa.hasParam("UntilEndOfCombat")) {
                    game.getEndOfCombat().addUntil(unanimate);
                } else if (sa.hasParam("UntilYourNextTurn")) {
                    game.getCleanup().addUntil(host.getController(), unanimate);
                } else {
                    game.getEndOfTurn().addUntil(unanimate);
                }
            }
        }
    } // animateAllResolve

}
