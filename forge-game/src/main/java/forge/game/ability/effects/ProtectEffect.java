package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;

public class ProtectEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
         * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
         */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final List<String> gains = getProtectionList(sa);
        final boolean choose = sa.hasParam("Choices");
        final String joiner = choose ? "or" : "and";

        final StringBuilder sb = new StringBuilder();

        List<Card> tgtCards = getTargetCards(sa);

        if (!tgtCards.isEmpty()) {
            final Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                final Card tgtC = it.next();
                if (tgtC.isFaceDown()) {
                    sb.append("Morph");
                } else {
                    sb.append(tgtC);
                }

                if (it.hasNext()) {
                    sb.append(", ");
                }
            }

            if (sa.hasParam("Radiance") && (sa.usesTargeting())) {
                sb.append(" and each other ").append(sa.getParam("ValidTgts"))
                        .append(" that shares a color with ");
                if (tgtCards.size() > 1) {
                    sb.append("them");
                } else {
                    sb.append("it");
                }
            }

            sb.append(" gain");
            if (tgtCards.size() == 1) {
                sb.append("s");
            }
            sb.append(" protection from ");

            if (choose) {
                sb.append("your choice of ");
            }

            for (int i = 0; i < gains.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }

                if (i == (gains.size() - 1)) {
                    sb.append(joiner).append(" ");
                }

                sb.append(gains.get(i));
            }

            if (!"Permanent".equals(sa.getParam("Duration"))) {
                sb.append(" until end of turn");
            }

            sb.append(".");
        }

        return sb.toString();
    } // protectStackDescription()

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = sa.getActivatingPlayer().getGame();

        final boolean isChoice = sa.getParam("Gains").contains("Choice");
        final List<String> choices = getProtectionList(sa);
        final List<String> gains = new ArrayList<>();
        final List<Card> tgtCards = getTargetCards(sa);
        
        if (isChoice && !choices.isEmpty())  {
            Player choser = sa.getActivatingPlayer();
            if (sa.hasParam("Choser") && sa.getParam("Choser").equals("Controller") && !tgtCards.isEmpty()) {
                choser = tgtCards.get(0).getController();
            }
            final String choice = choser.getController().chooseProtectionType(Localizer.getInstance().getMessage("lblChooseAProtection"), sa, choices);
            if (null == choice)
                return;
            gains.add(choice);
            game.getAction().notifyOfValue(sa, choser, Lang.joinHomogenous(gains), choser);
        } else {
            if (sa.getParam("Gains").equals("ChosenColor")) {
                for (final String color : host.getChosenColors()) {
                    gains.add(color.toLowerCase());
                }
            } else {
                gains.addAll(choices);
            }
        }

        List<String> gainsKWList = Lists.newArrayList();
        for (String color : gains) {
            gainsKWList.add(TextUtil.concatWithSpace("Protection from", color));
        }

        final CardCollection untargetedCards = CardUtil.getRadiance(sa);

        final long timestamp = game.getNextTimestamp();

        for (final Card tgtC : tgtCards) {
            // only pump things in play
            if (!tgtC.isInPlay()) {
                continue;
            }

            // if this is a target, make sure we can still target now
            if (sa.usesTargeting() && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }

            tgtC.addChangedCardKeywords(gainsKWList, null, false, timestamp, 0, true);

            if (!"Permanent".equals(sa.getParam("Duration"))) {
                // If not Permanent, remove protection at EOT
                final GameCommand untilEOT = new GameCommand() {
                    private static final long serialVersionUID = 7682700789217703789L;

                    @Override
                    public void run() {
                        if (tgtC.isInPlay()) {
                            tgtC.removeChangedCardKeywords(timestamp, 0, true);
                        }
                    }
                };
                addUntilCommand(sa, untilEOT);
            }
        }

        for (final Card unTgtC : untargetedCards) {
            // only pump things in play
            if (!unTgtC.isInPlay()) {
                continue;
            }

            unTgtC.addChangedCardKeywords(gainsKWList, null, false, timestamp, 0, true);

            if (!"Permanent".equals(sa.getParam("Duration"))) {
                // If not Permanent, remove protection at EOT
                final GameCommand untilEOT = new GameCommand() {
                    private static final long serialVersionUID = 7682700789217703789L;

                    @Override
                    public void run() {
                        if (unTgtC.isInPlay()) {
                            unTgtC.removeChangedCardKeywords(timestamp, 0, true);
                        }
                    }
                };
                addUntilCommand(sa, untilEOT);
            }
        }
    } // protectResolve()
    
    public static List<String> getProtectionList(final SpellAbility sa) {
        final List<String> gains = new ArrayList<>();

        final String gainStr = sa.getParam("Gains");
        if (gainStr.equals("Choice")) {
            String choices = sa.getParam("Choices");

            // Replace AnyColor with the 5 colors
            if (choices.contains("AnyColor")) {
                gains.addAll(MagicColor.Constant.ONLY_COLORS);
                choices = choices.replaceAll("AnyColor,?", "");
            }
            // Add any remaining choices
            if (choices.length() > 0) {
                gains.addAll(Arrays.asList(choices.split(",")));
            }
        } else {
            gains.addAll(Arrays.asList(gainStr.split(",")));
        }
        return gains;
    }

}
