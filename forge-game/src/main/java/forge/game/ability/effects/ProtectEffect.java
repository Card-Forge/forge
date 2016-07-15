package forge.game.ability.effects;

import forge.GameCommand;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.Lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ProtectEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
         * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
         */
    @Override
    protected String getStackDescription(SpellAbility sa) {

        final List<String> gains = getProtectionList(sa);
        final boolean choose = (sa.hasParam("Choices")) ? true : false;
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

            if (!sa.hasParam("Permanent")) {
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
        final List<String> gains = new ArrayList<String>();
        final List<Card> tgtCards = getTargetCards(sa);
        
        if (isChoice && !choices.isEmpty())  {
            Player choser = sa.getActivatingPlayer();
            if (sa.hasParam("Choser") && sa.getParam("Choser").equals("Controller") && !tgtCards.isEmpty()) {
            	choser = tgtCards.get(0).getController();
            }
            final String choice = choser.getController().chooseProtectionType("Choose a protection", sa, choices);
            if( null == choice)
                return;
            gains.add(choice);
            game.getAction().nofityOfValue(sa, choser, Lang.joinHomogenous(gains), choser);
        } else {
            if (sa.getParam("Gains").equals("ChosenColor")) {
                for (final String color : host.getChosenColors()) {
                    gains.add(color.toLowerCase());
                }
            } else {
                gains.addAll(choices);
            }
        }

        final List<Card> untargetedCards = new ArrayList<Card>();
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (sa.hasParam("Radiance") && (tgt != null)) {
            for (final Card c : CardUtil.getRadiance(host, tgtCards.get(0),
                    sa.getParam("ValidTgts").split(","))) {
                untargetedCards.add(c);
            }
        }


        for (final Card tgtC : tgtCards) {
            // only pump things in play
            if (!tgtC.isInPlay()) {
                continue;
            }

            // if this is a target, make sure we can still target now
            if ((tgt != null) && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }

            for (final String gain : gains) {
                tgtC.addExtrinsicKeyword("Protection from " + gain);
                tgtC.updateKeywords();
            }

            if (!sa.hasParam("Permanent")) {
                // If not Permanent, remove protection at EOT
                final GameCommand untilEOT = new GameCommand() {
                    private static final long serialVersionUID = 7682700789217703789L;

                    @Override
                    public void run() {
                        if (tgtC.isInPlay()) {
                            for (final String gain : gains) {
                                tgtC.removeExtrinsicKeyword("Protection from " + gain);
                            }
                        }
                    }
                };
                if (sa.hasParam("UntilEndOfCombat")) {
                    game.getEndOfCombat().addUntil(untilEOT);
                } else {
                    game.getEndOfTurn().addUntil(untilEOT);
                }
            }
        }

        for (final Card unTgtC : untargetedCards) {
            // only pump things in play
            if (!unTgtC.isInPlay()) {
                continue;
            }

            for (final String gain : gains) {
                unTgtC.addExtrinsicKeyword("Protection from " + gain);
                unTgtC.updateKeywords();
            }

            if (!sa.hasParam("Permanent")) {
                // If not Permanent, remove protection at EOT
                final GameCommand untilEOT = new GameCommand() {
                    private static final long serialVersionUID = 7682700789217703789L;

                    @Override
                    public void run() {
                        if (unTgtC.isInPlay()) {
                            for (final String gain : gains) {
                                unTgtC.removeExtrinsicKeyword("Protection from " + gain);
                            }
                        }
                    }
                };
                if (sa.hasParam("UntilEndOfCombat")) {
                    game.getEndOfCombat().addUntil(untilEOT);
                } else {
                    game.getEndOfTurn().addUntil(untilEOT);
                }
            }
        }
    } // protectResolve()
    

    public static List<String> getProtectionList(final SpellAbility sa) {
        final List<String> gains = new ArrayList<String>();

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
