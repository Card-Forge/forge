package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.Game;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.Player;
import forge.gui.GuiChoose;
import forge.util.Lang;


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


        if (tgtCards.size() > 0) {

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
        final Card host = sa.getSourceCard();
        final Game game = sa.getActivatingPlayer().getGame();

        final boolean isChoice = sa.getParam("Gains").contains("Choice");
        final List<String> choices = getProtectionList(sa);
        final List<String> gains = new ArrayList<String>();
        if (isChoice && !choices.isEmpty())  {
            Player choser = sa.getActivatingPlayer();
            
            if (choser.isHuman()) {
                final String choice = GuiChoose.one("Choose a protection", choices);
                gains.add(choice);
            } else {
                String choice = choices.get(0);
                final String logic = sa.getParam("AILogic");
                if (logic == null || logic.equals("MostProminentHumanCreatures")) {
                    List<Card> list = new ArrayList<Card>();
                    for (Player opp : choser.getOpponents()) {
                        list.addAll(opp.getCreaturesInPlay());
                    }
                    if (list.isEmpty()) {
                        list = CardLists.filterControlledBy(game.getCardsInGame(), choser.getOpponents());
                    }
                    if (!list.isEmpty()) {
                        choice = ComputerUtilCard.getMostProminentColor(list);
                    }
                }
                gains.add(choice);
            }
            game.getAction().nofityOfValue(sa, choser, Lang.joinHomogenous(gains), choser);
        } else {
            if (sa.getParam("Gains").equals("ChosenColor")) {
                for (final String color : host.getChosenColor()) {
                    gains.add(color.toLowerCase());
                }
            } else {
                gains.addAll(choices);
            }
        }

        final List<Card> tgtCards = getTargetCards(sa);
        final ArrayList<Card> untargetedCards = new ArrayList<Card>();
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
            }

            if (!sa.hasParam("Permanent")) {
                // If not Permanent, remove protection at EOT
                final Command untilEOT = new Command() {
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
            }

            if (!sa.hasParam("Permanent")) {
                // If not Permanent, remove protection at EOT
                final Command untilEOT = new Command() {
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
        final ArrayList<String> gains = new ArrayList<String>();

        final String gainStr = sa.getParam("Gains");
        if (gainStr.equals("Choice")) {
            String choices = sa.getParam("Choices");

            // Replace AnyColor with the 5 colors
            if (choices.contains("AnyColor")) {
                gains.addAll(Constant.Color.ONLY_COLORS);
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
