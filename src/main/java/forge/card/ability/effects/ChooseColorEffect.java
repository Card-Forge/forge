package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.Constant;
import forge.Singletons;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class ChooseColorEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a color");
        if (sa.hasParam("OrColors")) {
            sb.append(" or colors");
        }
        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        final Target tgt = sa.getTarget();

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (sa.getActivatingPlayer().isHuman()) {
                    if (sa.hasParam("OrColors")) {
                        String[] choices = Constant.Color.ONLY_COLORS;
                        final List<String> o = GuiChoose.getChoices("Choose a color or colors", 1, choices.length, choices);
                        card.setChosenColor(new ArrayList<String>(o));
                    } else if (sa.hasParam("TwoColors")) {
                        final List<String> o = GuiChoose.getChoices("Choose two colors", 2, 2, Constant.Color.ONLY_COLORS);
                        card.setChosenColor(new ArrayList<String>(o));
                    } else {
                        final Object o = GuiChoose.one("Choose a color", Constant.Color.ONLY_COLORS);
                        if (null == o) {
                            return;
                        }
                        final String choice = (String) o;
                        final ArrayList<String> tmpColors = new ArrayList<String>();
                        tmpColors.add(choice);
                        card.setChosenColor(tmpColors);
                    }
                } else {
                    List<String> chosen = new ArrayList<String>();
                    Player ai = sa.getActivatingPlayer();
                    Player opp = ai.getOpponent();
                    if (sa.hasParam("AILogic")) {
                        final String logic = sa.getParam("AILogic");
                        if (logic.equals("MostProminentInHumanDeck")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(CardLists.filterControlledBy(Singletons.getModel().getGame().getCardsInGame(), opp)));
                        } else if (logic.equals("MostProminentInComputerDeck")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(CardLists.filterControlledBy(Singletons.getModel().getGame().getCardsInGame(), ai)));
                        } else if (logic.equals("MostProminentDualInComputerDeck")) {
                            List<String> prominence = CardFactoryUtil.getColorByProminence(CardLists.filterControlledBy(Singletons.getModel().getGame().getCardsInGame(), ai));
                            chosen.add(prominence.get(0));
                            chosen.add(prominence.get(1));
                        }
                        else if (logic.equals("MostProminentInGame")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(Singletons.getModel().getGame().getCardsInGame()));
                        }
                        else if (logic.equals("MostProminentHumanCreatures")) {
                            List<Card> list = opp.getCreaturesInPlay();
                            if (list.isEmpty()) {
                                list = CardLists.filter(CardLists.filterControlledBy(Singletons.getModel().getGame().getCardsInGame(), opp), CardPredicates.Presets.CREATURES);
                            }
                            chosen.add(CardFactoryUtil.getMostProminentColor(list));
                        }
                        else if (logic.equals("MostProminentComputerControls")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(ai.getCardsIn(ZoneType.Battlefield)));
                        }
                        else if (logic.equals("MostProminentHumanControls")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(ai.getOpponent().getCardsIn(ZoneType.Battlefield)));
                        }
                        else if (logic.equals("MostProminentPermanent")) {
                            final List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
                            chosen.add(CardFactoryUtil.getMostProminentColor(list));
                        }
                        else if (logic.equals("MostProminentAttackers")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(Singletons.getModel().getGame().getCombat()
                                    .getAttackerList()));
                        }
                        else if (logic.equals("MostProminentKeywordInComputerDeck")) {
                            List<Card> list = ai.getAllCards();
                            int max = 0;
                            String chosenColor = Constant.Color.WHITE;

                            for (final String c : Constant.Color.ONLY_COLORS) {
                                final int cmp = CardLists.filter(list, CardPredicates.containsKeyword(c)).size();
                                if (cmp > max) {
                                    max = cmp;
                                    chosenColor = c;
                                }
                            }
                            chosen.add(chosenColor);
                        }
                    }
                    if (chosen.size() == 0) {
                        chosen.add(Constant.Color.GREEN);
                    }
                    GuiChoose.one("Computer picked: ", chosen);
                    final ArrayList<String> colorTemp = new ArrayList<String>();
                    colorTemp.addAll(chosen);
                    card.setChosenColor(colorTemp);
                }
            }
        }
    }

}
