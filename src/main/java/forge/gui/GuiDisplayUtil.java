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
import forge.CardCharacteristicName;

import forge.CardUtil;
import forge.Constant;
import forge.Counters;
import forge.Singletons;
import forge.card.spellability.AbilityMana;
import forge.card.trigger.TriggerType;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
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
        final List<String> list = CardUtil.getColors(card);

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
        final Player human = Singletons.getControl().getPlayer();
        dummy.setOwner(human);
        dummy.addController(human);
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
        for( Player p : AllZone.getPlayersInGame())
        {
            p.getZone(ZoneType.Battlefield).updateObservers();
            p.getZone(ZoneType.Battlefield).updateObservers();
        }
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

        List<Card> humanDevSetup = new ArrayList<Card>();
        List<Card> computerDevSetup = new ArrayList<Card>();
        List<Card> humanDevHandSetup = new ArrayList<Card>();
        List<Card> computerDevHandSetup = new ArrayList<Card>();
        List<Card> humanDevGraveyardSetup = new ArrayList<Card>();
        List<Card> computerDevGraveyardSetup = new ArrayList<Card>();
        List<Card> humanDevLibrarySetup = new ArrayList<Card>();
        List<Card> computerDevLibrarySetup = new ArrayList<Card>();
        List<Card> humanDevExileSetup = new ArrayList<Card>();
        List<Card> computerDevExileSetup = new ArrayList<Card>();

        final Player human = Singletons.getControl().getPlayer();
        final Player ai = human.getOpponent();
        
        if (!tChangePlayer.trim().toLowerCase().equals("none")) {
            if (tChangePlayer.trim().toLowerCase().equals("human")) {
                Singletons.getModel().getGameState().getPhaseHandler().setPlayerTurn(human);
            }
            if (tChangePlayer.trim().toLowerCase().equals("ai")) {
                Singletons.getModel().getGameState().getPhaseHandler().setPlayerTurn(ai);
            }
        }


        
        if (!tChangePhase.trim().toLowerCase().equals("none")) {
            Singletons.getModel().getGameState().getPhaseHandler().setDevPhaseState(forge.game.phase.PhaseType.smartValueOf(tChangePhase));
        }

        if (!tHumanSetupCardsInPlay.trim().toLowerCase().equals("none")) {
            humanDevSetup = GuiDisplayUtil.devProcessCardsForZone(humanSetupCardsInPlay, human);
        }

        if (!tHumanSetupCardsInHand.trim().toLowerCase().equals("none")) {
            humanDevHandSetup = GuiDisplayUtil.devProcessCardsForZone(humanSetupCardsInHand, human);
        }

        if (!tComputerSetupCardsInPlay.trim().toLowerCase().equals("none")) {
            computerDevSetup = GuiDisplayUtil.devProcessCardsForZone(computerSetupCardsInPlay, ai);
        }

        if (!tComputerSetupCardsInHand.trim().toLowerCase().equals("none")) {
            computerDevHandSetup = GuiDisplayUtil.devProcessCardsForZone(computerSetupCardsInHand, ai);
        }

        if (!tComputerSetupGraveyard.trim().toLowerCase().equals("none")) {
            computerDevGraveyardSetup = GuiDisplayUtil.devProcessCardsForZone(computerSetupGraveyard, ai);
        }

        if (!tHumanSetupGraveyard.trim().toLowerCase().equals("none")) {
            humanDevGraveyardSetup = GuiDisplayUtil.devProcessCardsForZone(humanSetupGraveyard, human);
        }

        if (!tHumanSetupLibrary.trim().toLowerCase().equals("none")) {
            humanDevLibrarySetup = GuiDisplayUtil.devProcessCardsForZone(humanSetupLibrary, human);
        }

        if (!tComputerSetupLibrary.trim().toLowerCase().equals("none")) {
            computerDevLibrarySetup = GuiDisplayUtil.devProcessCardsForZone(computerSetupLibrary, ai);
        }

        if (!tHumanSetupExile.trim().toLowerCase().equals("none")) {
            humanDevExileSetup = GuiDisplayUtil.devProcessCardsForZone(humanSetupExile, human);
        }

        if (!tComputerSetupExile.trim().toLowerCase().equals("none")) {
            computerDevExileSetup = GuiDisplayUtil.devProcessCardsForZone(computerSetupExile, ai);
        }

        AllZone.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        AllZone.getCombat().reset();
        for (final Card c : humanDevSetup) {
            human.getZone(ZoneType.Hand).add(c);
            Singletons.getModel().getGameAction().moveToPlay(c);
            c.setSickness(false);
        }

        for (final Card c : computerDevSetup) {
            ai.getZone(ZoneType.Hand).add(c);
            Singletons.getModel().getGameAction().moveToPlay(c);
            c.setSickness(false);
        }

        if (computerDevGraveyardSetup.size() > 0) {
            ai.getZone(ZoneType.Graveyard).setCards(computerDevGraveyardSetup);
        }
        if (humanDevGraveyardSetup.size() > 0) {
            human.getZone(ZoneType.Graveyard).setCards(humanDevGraveyardSetup);
        }

        if (computerDevHandSetup.size() > 0) {
            ai.getZone(ZoneType.Hand).setCards(computerDevHandSetup);
        }
        if (humanDevHandSetup.size() > 0) {
            human.getZone(ZoneType.Hand).setCards(humanDevHandSetup);
        }

        if (humanDevLibrarySetup.size() > 0) {
            human.getZone(ZoneType.Library).setCards(humanDevLibrarySetup);
        }
        if (computerDevLibrarySetup.size() > 0) {
            ai.getZone(ZoneType.Library).setCards(computerDevLibrarySetup);
        }

        if (humanDevExileSetup.size() > 0) {
            human.getZone(ZoneType.Exile).setCards(humanDevExileSetup);
        }
        if (computerDevExileSetup.size() > 0) {
            ai.getZone(ZoneType.Exile).setCards(computerDevExileSetup);
        }

        AllZone.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        if (setComputerLife > 0) {
            ai.setLife(setComputerLife, null);
        }
        if (setHumanLife > 0) {
            human.setLife(setHumanLife, null);
        }

        Singletons.getModel().getGameAction().checkStateEffects();
        Singletons.getModel().getGameState().getPhaseHandler().updateObservers();
        updateGUI();
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
    public static List<Card> devProcessCardsForZone(final String[] data, final Player player) {
        final List<Card> cl = new ArrayList<Card>();
        for (final String element : data) {
            final String[] cardinfo = element.trim().split("\\|");

            final Card c = AllZone.getCardFactory().getCard(CardDb.instance().getCard(cardinfo[0]), player);

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
                    c.setState(CardCharacteristicName.FaceDown);
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
        final List<Card> lib = Singletons.getControl().getPlayer().getCardsIn(ZoneType.Library);
        final Object o = GuiChoose.oneOrNone("Choose a card", lib);
        if (null == o) {
            return;
        } else {
            final Card c = (Card) o;
            Singletons.getModel().getGameAction().moveToHand(c);
        }
    }

    /**
     * <p>
     * devModeTutorAnyCard.
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
            Card forgeCard = cp.toForgeCard(Singletons.getControl().getPlayer());
            Singletons.getModel().getGameAction().moveToHand(forgeCard);
        }
    }

    /**
     * <p>
     * devModeGiveAnyCard. (any card to AI hand)
     * </p>
     * 
     * @since 1.2.7
     */
    public static void devModeGiveAnyCard() {
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
            Card forgeCard = cp.toForgeCard(Singletons.getControl().getPlayer().getOpponent());
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
        final Card o = GuiChoose.oneOrNone("Add counters to which card?", AllZoneUtil.getCardsIn(ZoneType.Battlefield));
        if (null == o) {
            return;
        } else {
            final Card c = o;
            final Counters counter = GuiChoose.oneOrNone("Which type of counter?", Counters.values());
            if (null == counter) {
                return;
            } else {
                final Integer[] integers = new Integer[99];
                for (int j = 0; j < 99; j++) {
                    integers[j] = Integer.valueOf(j);
                }
                final Integer i = GuiChoose.oneOrNone("How many counters?", integers);
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
        final List<Card> play = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        final Object o = GuiChoose.oneOrNone("Choose a permanent", play);
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
        final List<Card> play = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        final Object o = GuiChoose.oneOrNone("Choose a permanent", play);
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
        Singletons.getControl().getPlayer().addMaxLandsToPlay(100);
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
        final Player o = GuiChoose.oneOrNone("Set life for which player?", players);
        if (null == o) {
            return;
        } else {
            final Player p = o;
            final Integer[] integers = new Integer[99];
            for (int j = 0; j < 99; j++) {
                integers[j] = Integer.valueOf(j);
            }
            final Integer i = GuiChoose.oneOrNone("Set life to what?", integers);
            if (null == i) {
                return;
            } else {
                p.setLife(i, null);
            }
        }
    }

    public static void devModeBreakpoint() {
        List<Player> Players = AllZone.getPlayersInGame();
/*
        Combat CombatHandler = AllZone.getCombat();
        TriggerHandler Triggers = AllZone.getTriggerHandler();
        InputControl InputHandler = AllZone.getInputControl();
        ReplacementHandler Replacements = AllZone.getReplacementHandler();
        StaticEffects StaticHandler = AllZone.getStaticEffects();
*/
        List<PlayerZone> Zones = new ArrayList<PlayerZone>();
        for (Player p : Players) {

            Zones.add(p.getZone(ZoneType.Ante));
            Zones.add(p.getZone(ZoneType.Battlefield));
            Zones.add(p.getZone(ZoneType.Command));
            Zones.add(p.getZone(ZoneType.Exile));
            Zones.add(p.getZone(ZoneType.Graveyard));
            Zones.add(p.getZone(ZoneType.Hand));
            Zones.add(p.getZone(ZoneType.Library));
            Zones.add(p.getZone(ZoneType.Sideboard));
            Zones.add(p.getZone(ZoneType.Stack));
        }

        //Set a breakpoint on the following statement
        System.out.println("Manual Breakpoint");
    }

} // end class GuiDisplayUtil
