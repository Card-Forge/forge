package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.Constant;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class ChooseColorEffect extends SpellEffect {
    
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }
    
        ArrayList<Player> tgtPlayers;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a color");
        if (params.containsKey("OrColors")) {
            sb.append(" or colors");
        }
        sb.append(".");

        return sb.toString();
    }

    /**
     * <p>
     * chooseColorResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (sa.getActivatingPlayer().isHuman()) {
                    if (params.containsKey("OrColors")) {
                        final List<String> o = GuiChoose.oneOrMany("Choose a color or colors",
                                Constant.Color.ONLY_COLORS);
                        card.setChosenColor(new ArrayList<String>(o));
                    } else if (params.containsKey("TwoColors")) {
                        final List<String> o = GuiChoose.amount("Choose two colors", Constant.Color.ONLY_COLORS, 2);
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
                    if (params.containsKey("AILogic")) {
                        final String logic = params.get("AILogic");
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
                        else if (logic.equals("MostProminentPermanent")) {
                            final List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
                            chosen.add(CardFactoryUtil.getMostProminentColor(list));
                        }
                        else if (logic.equals("MostProminentAttackers")) {
                            chosen.add(CardFactoryUtil.getMostProminentColor(Singletons.getModel().getGame().getCombat()
                                    .getAttackerList()));
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

    // *************************************************************************
    // ************************* ChooseNumber **********************************
    // *************************************************************************


    /**
     * <p>
     * chooseNumberStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    
}