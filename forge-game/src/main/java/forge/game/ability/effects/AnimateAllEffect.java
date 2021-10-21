package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;

import forge.GameCommand;
import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class AnimateAllEffect extends AnimateEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Animate all valid cards.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();

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

        final boolean permanent = "Permanent".equals(sa.getParam("Duration"));

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
        } else if (types.hasSubtype("ChosenType2")) {
            types.clear();
            types.add(host.getChosenType2());
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
            if (host.hasSVar(k)) {
                keywords.add(host.getSVar(k));
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

        final String valid = sa.getParamOrDefault("ValidCards", "");

        CardCollectionView list;

        if (!sa.usesTargeting() && !sa.hasParam("Defined")) {
            list = game.getCardsIn(ZoneType.Battlefield);
        } else {
            list = getTargetPlayers(sa).getCardsIn(ZoneType.Battlefield);
        }

        list = CardLists.getValidCards(list, valid.split(","), host.getController(), host, sa);

        for (final Card c : list) {
            doAnimate(c, sa, power, toughness, types, removeTypes, finalDesc,
                    keywords, removeKeywords, hiddenKeywords,
                    abilities, triggers, replacements, ImmutableList.of(),
                    timestamp);

            // give sVars
            for (final String s : sVars) {
                c.setSVar(s, AbilityUtils.getSVar(sa, s));
            }
            game.fireEvent(new GameEventCardStatsChanged(c));

            final GameCommand unanimate = new GameCommand() {
                private static final long serialVersionUID = -5861759814760561373L;

                @Override
                public void run() {
                    doUnanimate(c, timestamp);

                    game.fireEvent(new GameEventCardStatsChanged(c));
                }
            };

            if (!permanent) {
                addUntilCommand(sa, unanimate);
            }
        }
    } // animateAllResolve

}
