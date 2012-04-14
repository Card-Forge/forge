/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gui;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.border.Border;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardCharactersticName;
import forge.CardList;
import forge.CardUtil;
import forge.Constant;
import forge.Counters;
import forge.Singletons;
import forge.card.spellability.AbilityMana;
import forge.card.trigger.TriggerType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.view.arcane.PlayArea;
import forge.view.arcane.util.Animation;

/**
 * <p>
 * GuiDisplayUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class GuiDisplayUtil {

    private GuiDisplayUtil() {
        throw new AssertionError();
    }

    /**
     * <p>
     * getBorder.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link javax.swing.border.Border} object.
     */
    public static Border getBorder(final Card card) {
        // color info
        if (card == null) {
            return BorderFactory.createEmptyBorder(2, 2, 2, 2);
        }
        java.awt.Color color;
        final ArrayList<String> list = CardUtil.getColors(card);

        if (card.isFaceDown()) {
            color = Color.gray;
        } else if (list.size() > 1) {
            color = Color.orange;
        } else if (list.get(0).equals(Constant.Color.BLACK)) {
            color = Color.black;
        } else if (list.get(0).equals(Constant.Color.GREEN)) {
            color = new Color(0, 220, 39);
        } else if (list.get(0).equals(Constant.Color.WHITE)) {
            color = Color.white;
        } else if (list.get(0).equals(Constant.Color.RED)) {
            color = Color.red;
        } else if (list.get(0).equals(Constant.Color.BLUE)) {
            color = Color.blue;
        } else if (list.get(0).equals(Constant.Color.COLORLESS)) {
            color = Color.gray;
        } else {
            color = new Color(200, 0, 230); // If your card has a violet border,
                                            // something is wrong
        }

        if (color != Color.gray) {

            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();

            final int shade = 10;

            r -= shade;
            g -= shade;
            b -= shade;

            r = Math.max(0, r);
            g = Math.max(0, g);
            b = Math.max(0, b);

            color = new Color(r, g, b);

            return BorderFactory.createLineBorder(color, 2);
        } else {
            return BorderFactory.createLineBorder(Color.gray, 2);
        }
    }

    /**
     * <p>
     * devModeGenerateMana.
     * </p>
     */
    public static void devModeGenerateMana() {
        final Card dummy = new Card();
        dummy.setOwner(AllZone.getHumanPlayer());
        dummy.addController(AllZone.getHumanPlayer());
        final AbilityMana abMana = new AbilityMana(dummy, "0", "W U B G R 1", 10) {
            private static final long serialVersionUID = -2164401486331182356L;

        };
        abMana.produceMana();
    }

    /**
     * <p>
     * formatCardType.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link java.lang.String} object.
     */
    public static String formatCardType(final Card card) {
        final ArrayList<String> list = card.getType();
        final StringBuilder sb = new StringBuilder();

        final ArrayList<String> superTypes = new ArrayList<String>();
        final ArrayList<String> cardTypes = new ArrayList<String>();
        final ArrayList<String> subTypes = new ArrayList<String>();
        final boolean allCreatureTypes = list.contains("AllCreatureTypes");

        for (final String t : list) {
            if (allCreatureTypes && t.equals("AllCreatureTypes")) {
                continue;
            }
            if (CardUtil.isASuperType(t) && !superTypes.contains(t)) {
                superTypes.add(t);
            }
            if (CardUtil.isACardType(t) && !cardTypes.contains(t)) {
                cardTypes.add(t);
            }
            if (CardUtil.isASubType(t) && !subTypes.contains(t) && (!allCreatureTypes || !CardUtil.isACreatureType(t))) {
                subTypes.add(t);
            }
        }

        for (final String type : superTypes) {
            sb.append(type).append(" ");
        }
        for (final String type : cardTypes) {
            sb.append(type).append(" ");
        }
        if (!subTypes.isEmpty() || allCreatureTypes) {
            sb.append("- ");
        }
        if (allCreatureTypes) {
            sb.append("All creature types ");
        }
        for (final String type : subTypes) {
            sb.append(type).append(" ");
        }

        return sb.toString();
    }

    /**
     * <p>
     * cleanString.
     * </p>
     * 
     * @param in
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String cleanString(final String in) {
        final StringBuffer out = new StringBuffer();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if ((c == ' ') || (c == '-')) {
                out.append('_');
            } else if (Character.isLetterOrDigit(c) || (c == '_')) {
                out.append(c);
            }
        }
        return out.toString().toLowerCase();
    }

    /**
     * <p>
     * cleanStringMWS.
     * </p>
     * 
     * @param in
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String cleanStringMWS(final String in) {
        final StringBuffer out = new StringBuffer();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if ((c == '"') || (c == '/')) {
                out.append("");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }


    /**
     * <p>
     * setupPlayZone.
     * </p>
     * 
     * @param p
     *            a {@link forge.view.arcane.PlayArea} object.
     * @param c
     *            an array of {@link forge.Card} objects.
     */
    public static void setupPlayZone(final PlayArea p, final List<Card> c) {
        List<Card> tmp, diff;
        tmp = new ArrayList<Card>();
        for (final forge.view.arcane.CardPanel cpa : p.getCardPanels()) {
            tmp.add(cpa.getGameCard());
        }
        diff = new ArrayList<Card>(tmp);
        diff.removeAll(c);
        if (diff.size() == p.getCardPanels().size()) {
            p.clear();
        } else {
            for (final Card card : diff) {
                p.removeCardPanel(p.getCardPanel(card.getUniqueNumber()));
            }
        }
        diff = new ArrayList<Card>(c);
        diff.removeAll(tmp);

        forge.view.arcane.CardPanel toPanel = null;
        for (final Card card : diff) {
            toPanel = p.addCard(card);
            Animation.moveCard(toPanel);
        }

        for (final Card card : c) {
            toPanel = p.getCardPanel(card.getUniqueNumber());
            if (card.isTapped()) {
                toPanel.setTapped(true);
                toPanel.setTappedAngle(forge.view.arcane.CardPanel.TAPPED_ANGLE);
            } else {
                toPanel.setTapped(false);
                toPanel.setTappedAngle(0);
            }
            toPanel.getAttachedPanels().clear();
            if (card.isEnchanted()) {
                final ArrayList<Card> enchants = card.getEnchantedBy();
                for (final Card e : enchants) {
                    final forge.view.arcane.CardPanel cardE = p.getCardPanel(e.getUniqueNumber());
                    if (cardE != null) {
                        toPanel.getAttachedPanels().add(cardE);
                    }
                }
            }

            if (card.isEquipped()) {
                final ArrayList<Card> enchants = card.getEquippedBy();
                for (final Card e : enchants) {
                    final forge.view.arcane.CardPanel cardE = p.getCardPanel(e.getUniqueNumber());
                    if (cardE != null) {
                        toPanel.getAttachedPanels().add(cardE);
                    }
                }
            }

            if (card.isEnchantingCard()) {
                toPanel.setAttachedToPanel(p.getCardPanel(card.getEnchantingCard().getUniqueNumber()));
            } else if (card.isEquipping()) {
                toPanel.setAttachedToPanel(p.getCardPanel(card.getEquipping().get(0).getUniqueNumber()));
            } else {
                toPanel.setAttachedToPanel(null);
            }

            toPanel.setCard(toPanel.getGameCard());
        }
        p.invalidate();
        p.repaint();
    }

    /**
     * <p>
     * updateGUI.
     * </p>
     */
    public static void updateGUI() {
        AllZone.getComputerPlayer().getZone(ZoneType.Battlefield).updateObservers();
        AllZone.getHumanPlayer().getZone(ZoneType.Battlefield).updateObservers();
        //AllZone.getHumanPlayer().getZone(ZoneType.Hand).updateObservers();
        AllZone.getComputerPlayer().updateObservers();
        AllZone.getHumanPlayer().updateObservers();
    }

    /**
     * <p>
     * devSetupGameState.
     * </p>
     */
    public static void devSetupGameState() {
        String tHumanLife = "-1";
        String tComputerLife = "-1";
        String tHumanSetupCardsInPlay = "NONE";
        String tComputerSetupCardsInPlay = "NONE";
        String tHumanSetupCardsInHand = "NONE";
        String tComputerSetupCardsInHand = "NONE";
        String tHumanSetupGraveyard = "NONE";
        String tComputerSetupGraveyard = "NONE";
        String tHumanSetupLibrary = "NONE";
        String tComputerSetupLibrary = "NONE";
        String tHumanSetupExile = "NONE";
        String tComputerSetupExile = "NONE";
        String tChangePlayer = "NONE";
        String tChangePhase = "NONE";

        final String wd = ".";
        final JFileChooser fc = new JFileChooser(wd);
        final int rc = fc.showDialog(null, "Select Game State File");
        if (rc != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            final FileInputStream fstream = new FileInputStream(fc.getSelectedFile().getAbsolutePath());
            final DataInputStream in = new DataInputStream(fstream);
            final BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String temp = "";

            while ((temp = br.readLine()) != null) {
                final String[] tempData = temp.split("=");

                if (tempData.length < 2) {
                    continue;
                }
                if (tempData[0].toCharArray()[0] == '#') {
                    continue;
                }

                final String categoryName = tempData[0];
                final String categoryValue = tempData[1];

                if (categoryName.toLowerCase().equals("humanlife")) {
                    tHumanLife = categoryValue;
                } else if (categoryName.toLowerCase().equals("ailife")) {
                    tComputerLife = categoryValue;
                } else if (categoryName.toLowerCase().equals("humancardsinplay")) {
                    tHumanSetupCardsInPlay = categoryValue;
                } else if (categoryName.toLowerCase().equals("aicardsinplay")) {
                    tComputerSetupCardsInPlay = categoryValue;
                } else if (categoryName.toLowerCase().equals("humancardsinhand")) {
                    tHumanSetupCardsInHand = categoryValue;
                } else if (categoryName.toLowerCase().equals("aicardsinhand")) {
                    tComputerSetupCardsInHand = categoryValue;
                } else if (categoryName.toLowerCase().equals("humancardsingraveyard")) {
                    tHumanSetupGraveyard = categoryValue;
                } else if (categoryName.toLowerCase().equals("aicardsingraveyard")) {
                    tComputerSetupGraveyard = categoryValue;
                } else if (categoryName.toLowerCase().equals("humancardsinlibrary")) {
                    tHumanSetupLibrary = categoryValue;
                } else if (categoryName.toLowerCase().equals("aicardsinlibrary")) {
                    tComputerSetupLibrary = categoryValue;
                } else if (categoryName.toLowerCase().equals("humancardsinexile")) {
                    tHumanSetupExile = categoryValue;
                } else if (categoryName.toLowerCase().equals("aicardsinexile")) {
                    tComputerSetupExile = categoryValue;
                } else if (categoryName.toLowerCase().equals("activeplayer")) {
                    tChangePlayer = categoryValue;
                } else if (categoryName.toLowerCase().equals("activephase")) {
                    tChangePhase = categoryValue;
                }
            }

            in.close();
        } catch (final FileNotFoundException fnfe) {
            JOptionPane.showMessageDialog(null, "File not found: " + fc.getSelectedFile().getAbsolutePath());
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading battle setup file!");
            return;
        }

        final int setHumanLife = Integer.parseInt(tHumanLife);
        final int setComputerLife = Integer.parseInt(tComputerLife);

        final String[] humanSetupCardsInPlay = tHumanSetupCardsInPlay.split(";");
        final String[] computerSetupCardsInPlay = tComputerSetupCardsInPlay.split(";");
        final String[] humanSetupCardsInHand = tHumanSetupCardsInHand.split(";");
        final String[] computerSetupCardsInHand = tComputerSetupCardsInHand.split(";");
        final String[] humanSetupGraveyard = tHumanSetupGraveyard.split(";");
        final String[] computerSetupGraveyard = tComputerSetupGraveyard.split(";");
        final String[] humanSetupLibrary = tHumanSetupLibrary.split(";");
        final String[] computerSetupLibrary = tComputerSetupLibrary.split(";");
        final String[] humanSetupExile = tHumanSetupExile.split(";");
        final String[] computerSetupExile = tComputerSetupExile.split(";");

        CardList humanDevSetup = new CardList();
        CardList computerDevSetup = new CardList();
        CardList humanDevHandSetup = new CardList();
        CardList computerDevHandSetup = new CardList();
        CardList humanDevGraveyardSetup = new CardList();
        CardList computerDevGraveyardSetup = new CardList();
        CardList humanDevLibrarySetup = new CardList();
        CardList computerDevLibrarySetup = new CardList();
        CardList humanDevExileSetup = new CardList();
        CardList computerDevExileSetup = new CardList();

        if (!tChangePlayer.trim().toLowerCase().equals("none")) {
            if (tChangePlayer.trim().toLowerCase().equals("human")) {
                Singletons.getModel().getGameState().getPhaseHandler().setPlayerTurn(AllZone.getHumanPlayer());
            }
            if (tChangePlayer.trim().toLowerCase().equals("ai")) {
                Singletons.getModel().getGameState().getPhaseHandler().setPlayerTurn(AllZone.getComputerPlayer());
            }
        }

        if (!tChangePhase.trim().toLowerCase().equals("none")) {
            Singletons.getModel().getGameState().getPhaseHandler().setDevPhaseState(forge.game.phase.PhaseType.smartValueOf(tChangePhase));
        }

        if (!tHumanSetupCardsInPlay.trim().toLowerCase().equals("none")) {
            humanDevSetup = GuiDisplayUtil.devProcessCardsForZone(humanSetupCardsInPlay, AllZone.getHumanPlayer());
        }

        if (!tHumanSetupCardsInHand.trim().toLowerCase().equals("none")) {
            humanDevHandSetup = GuiDisplayUtil.devProcessCardsForZone(humanSetupCardsInHand, AllZone.getHumanPlayer());
        }

        if (!tComputerSetupCardsInPlay.trim().toLowerCase().equals("none")) {
            computerDevSetup = GuiDisplayUtil.devProcessCardsForZone(computerSetupCardsInPlay,
                    AllZone.getComputerPlayer());
        }

        if (!tComputerSetupCardsInHand.trim().toLowerCase().equals("none")) {
            computerDevHandSetup = GuiDisplayUtil.devProcessCardsForZone(computerSetupCardsInHand,
                    AllZone.getComputerPlayer());
        }

        if (!tComputerSetupGraveyard.trim().toLowerCase().equals("none")) {
            computerDevGraveyardSetup = GuiDisplayUtil.devProcessCardsForZone(computerSetupGraveyard,
                    AllZone.getComputerPlayer());
        }

        if (!tHumanSetupGraveyard.trim().toLowerCase().equals("none")) {
            humanDevGraveyardSetup = GuiDisplayUtil.devProcessCardsForZone(humanSetupGraveyard,
                    AllZone.getHumanPlayer());
        }

        if (!tHumanSetupLibrary.trim().toLowerCase().equals("none")) {
            humanDevLibrarySetup = GuiDisplayUtil.devProcessCardsForZone(humanSetupLibrary, AllZone.getHumanPlayer());
        }

        if (!tComputerSetupLibrary.trim().toLowerCase().equals("none")) {
            computerDevLibrarySetup = GuiDisplayUtil.devProcessCardsForZone(computerSetupLibrary,
                    AllZone.getComputerPlayer());
        }

        if (!tHumanSetupExile.trim().toLowerCase().equals("none")) {
            humanDevExileSetup = GuiDisplayUtil.devProcessCardsForZone(humanSetupExile, AllZone.getHumanPlayer());
        }

        if (!tComputerSetupExile.trim().toLowerCase().equals("none")) {
            computerDevExileSetup = GuiDisplayUtil.devProcessCardsForZone(computerSetupExile,
                    AllZone.getComputerPlayer());
        }

        AllZone.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        AllZone.getCombat().reset();
        for (final Card c : humanDevSetup) {
            AllZone.getHumanPlayer().getZone(ZoneType.Hand).add(c);
            Singletons.getModel().getGameAction().moveToPlay(c);
            c.setSickness(false);
        }

        for (final Card c : computerDevSetup) {
            AllZone.getComputerPlayer().getZone(ZoneType.Hand).add(c);
            Singletons.getModel().getGameAction().moveToPlay(c);
            c.setSickness(false);
        }

        if (computerDevGraveyardSetup.size() > 0) {
            AllZone.getComputerPlayer().getZone(ZoneType.Graveyard).setCards(computerDevGraveyardSetup);
        }
        if (humanDevGraveyardSetup.size() > 0) {
            AllZone.getHumanPlayer().getZone(ZoneType.Graveyard).setCards(humanDevGraveyardSetup);
        }

        if (computerDevHandSetup.size() > 0) {
            AllZone.getComputerPlayer().getZone(ZoneType.Hand).setCards(computerDevHandSetup);
        }
        if (humanDevHandSetup.size() > 0) {
            AllZone.getHumanPlayer().getZone(ZoneType.Hand).setCards(humanDevHandSetup);
        }

        if (humanDevLibrarySetup.size() > 0) {
            AllZone.getHumanPlayer().getZone(ZoneType.Library).setCards(humanDevLibrarySetup);
        }
        if (computerDevLibrarySetup.size() > 0) {
            AllZone.getComputerPlayer().getZone(ZoneType.Library).setCards(computerDevLibrarySetup);
        }

        if (humanDevExileSetup.size() > 0) {
            AllZone.getHumanPlayer().getZone(ZoneType.Exile).setCards(humanDevExileSetup);
        }
        if (computerDevExileSetup.size() > 0) {
            AllZone.getComputerPlayer().getZone(ZoneType.Exile).setCards(computerDevExileSetup);
        }

        AllZone.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        if (setComputerLife > 0) {
            AllZone.getComputerPlayer().setLife(setComputerLife, null);
        }
        if (setHumanLife > 0) {
            AllZone.getHumanPlayer().setLife(setHumanLife, null);
        }

        Singletons.getModel().getGameAction().checkStateEffects();
        Singletons.getModel().getGameState().getPhaseHandler().updateObservers();
        AllZone.getHumanPlayer().getZone(ZoneType.Exile).updateObservers();
        AllZone.getComputerPlayer().getZone(ZoneType.Exile).updateObservers();
        AllZone.getHumanPlayer().getZone(ZoneType.Hand).updateObservers();
        AllZone.getComputerPlayer().getZone(ZoneType.Hand).updateObservers();
        AllZone.getHumanPlayer().getZone(ZoneType.Graveyard).updateObservers();
        AllZone.getComputerPlayer().getZone(ZoneType.Graveyard).updateObservers();
        AllZone.getHumanPlayer().getZone(ZoneType.Battlefield).updateObservers();
        AllZone.getComputerPlayer().getZone(ZoneType.Battlefield).updateObservers();
        AllZone.getHumanPlayer().getZone(ZoneType.Library).updateObservers();
        AllZone.getComputerPlayer().getZone(ZoneType.Library).updateObservers();
    }

    /**
     * <p>
     * devProcessCardsForZone.
     * </p>
     * 
     * @param data
     *            an array of {@link java.lang.String} objects.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList devProcessCardsForZone(final String[] data, final Player player) {
        final CardList cl = new CardList();
        for (final String element : data) {
            final String[] cardinfo = element.trim().split("\\|");

            final Card c = AllZone.getCardFactory().getCard(cardinfo[0], player);

            boolean hasSetCurSet = false;
            for (final String info : cardinfo) {
                if (info.startsWith("Set:")) {
                    c.setCurSetCode(info.substring(info.indexOf(':') + 1));
                    hasSetCurSet = true;
                } else if (info.equalsIgnoreCase("Tapped:True")) {
                    c.tap();
                } else if (info.startsWith("Counters:")) {
                    final String[] counterStrings = info.substring(info.indexOf(':') + 1).split(",");
                    for (final String counter : counterStrings) {
                        c.addCounter(Counters.valueOf(counter), 1);
                    }
                } else if (info.equalsIgnoreCase("SummonSick:True")) {
                    c.setSickness(true);
                } else if (info.equalsIgnoreCase("FaceDown:True")) {
                    c.setState(CardCharactersticName.FaceDown);
                }
            }

            if (!hasSetCurSet) {
                c.setCurSetCode(c.getMostRecentSet());
            }

            c.setImageFilename(CardUtil.buildFilename(c));
            cl.add(c);
        }
        return cl;
    }

    /**
     * <p>
     * devModeTutor.
     * </p>
     * 
     * @since 1.0.15
     */
    public static void devModeTutor() {
        final CardList lib = AllZone.getHumanPlayer().getCardsIn(ZoneType.Library);
        final Object o = GuiUtils.chooseOneOrNone("Choose a card", lib.toArray());
        if (null == o) {
            return;
        } else {
            final Card c = (Card) o;
            Singletons.getModel().getGameAction().moveToHand(c);
        }
    }

    /**
     * <p>
     * devModeTutor.
     * </p>
     * 
     * @since 1.2.7
     */
    public static void devModeAddAnyCard() {
        final Iterable<CardPrinted> uniqueCards = CardDb.instance().getAllUniqueCards();
        final List<String> cards = new ArrayList<String>();
        for (final CardPrinted c : uniqueCards) {
            cards.add(c.getName());
        }
        Collections.sort(cards);

        // use standard forge's list selection dialog
        final ListChooser<String> c = new ListChooser<String>("Name the card", 0, 1, cards);
        if (c.show()) {
            CardPrinted cp = CardDb.instance().getCard(c.getSelectedValue());
            Card forgeCard = cp.toForgeCard(AllZone.getHumanPlayer());
            Singletons.getModel().getGameAction().moveToHand(forgeCard);
        }
    }

    /**
     * <p>
     * devModeAddCounter.
     * </p>
     * 
     * @since 1.0.15
     */
    public static void devModeAddCounter() {
        final CardList play = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        final Object o = GuiUtils.chooseOneOrNone("Add counters to which card?", play.toArray());
        if (null == o) {
            return;
        } else {
            final Card c = (Card) o;
            final Counters counter = GuiUtils.chooseOneOrNone("Which type of counter?", Counters.values());
            if (null == counter) {
                return;
            } else {
                final Integer[] integers = new Integer[99];
                for (int j = 0; j < 99; j++) {
                    integers[j] = Integer.valueOf(j);
                }
                final Integer i = GuiUtils.chooseOneOrNone("How many counters?", integers);
                if (null == i) {
                    return;
                } else {
                    c.addCounterFromNonEffect(counter, i);
                }
            }
        }
    }

    /**
     * <p>
     * devModeTapPerm.
     * </p>
     * 
     * @since 1.0.15
     */
    public static void devModeTapPerm() {
        final CardList play = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        final Object o = GuiUtils.chooseOneOrNone("Choose a permanent", play.toArray());
        if (null == o) {
            return;
        } else {
            final Card c = (Card) o;
            c.tap();
        }
    }

    /**
     * <p>
     * devModeUntapPerm.
     * </p>
     * 
     * @since 1.0.15
     */
    public static void devModeUntapPerm() {
        final CardList play = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        final Object o = GuiUtils.chooseOneOrNone("Choose a permanent", play.toArray());
        if (null == o) {
            return;
        } else {
            final Card c = (Card) o;
            c.untap();
        }
    }

    /**
     * <p>
     * devModeUnlimitedLand.
     * </p>
     * 
     * @since 1.0.16
     */
    public static void devModeUnlimitedLand() {
        AllZone.getHumanPlayer().addMaxLandsToPlay(100);
    }

    /**
     * <p>
     * devModeSetLife.
     * </p>
     * 
     * @since 1.1.3
     */
    public static void devModeSetLife() {
        final List<Player> players = AllZone.getPlayersInGame();
        final Object o = GuiUtils.chooseOneOrNone("Set life for which player?", players.toArray());
        if (null == o) {
            return;
        } else {
            final Player p = (Player) o;
            final Integer[] integers = new Integer[99];
            for (int j = 0; j < 99; j++) {
                integers[j] = Integer.valueOf(j);
            }
            final Integer i = GuiUtils.chooseOneOrNone("Set life to what?", integers);
            if (null == i) {
                return;
            } else {
                p.setLife(i, null);
            }
        }
    }

} // end class GuiDisplayUtil
