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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.border.Border;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardUtil;
import forge.Constant;
import forge.CounterType;
import forge.Singletons;
import forge.card.CardType;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.TriggerType;
import forge.game.GameState;
import forge.game.PlanarDice;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.IPaperCard;

public final class GuiDisplayUtil {
    private GuiDisplayUtil() {
        throw new AssertionError();
    }

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

    public static void devModeGenerateMana() {
        final Card dummy = new Card();
        final Player human = Singletons.getControl().getPlayer();
        dummy.setOwner(human);
        dummy.addController(human);
        Map<String, String> produced = new HashMap<String, String>();
        produced.put("Produced", "W W W W W W W U U U U U U U B B B B B B B G G G G G G G R R R R R R R 7");
        final AbilityManaPart abMana = new AbilityManaPart(dummy, produced);
        abMana.produceMana(null);
    }

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
            if (CardType.isASuperType(t) && !superTypes.contains(t)) {
                superTypes.add(t);
            }
            if (CardType.isACardType(t) && !cardTypes.contains(t)) {
                cardTypes.add(t);
            }
            if (CardType.isASubType(t) && !subTypes.contains(t) && (!allCreatureTypes || !CardType.isACreatureType(t))) {
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

    public static void updateGUI() {
        for (Player p : Singletons.getModel().getGame().getRegisteredPlayers()) {

            // why was it written twice?
            p.getZone(ZoneType.Battlefield).updateObservers();
        }
    }

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
        final Player ai = human.getOpponents().get(0);

        if (!tChangePlayer.trim().toLowerCase().equals("none")) {
            if (tChangePlayer.trim().toLowerCase().equals("human")) {
                Singletons.getModel().getGame().getPhaseHandler().setPlayerTurn(human);
            }
            if (tChangePlayer.trim().toLowerCase().equals("ai")) {
                Singletons.getModel().getGame().getPhaseHandler().setPlayerTurn(ai);
            }
        }



        if (!tChangePhase.trim().toLowerCase().equals("none")) {
            Singletons.getModel().getGame().getPhaseHandler().setDevPhaseState(forge.game.phase.PhaseType.smartValueOf(tChangePhase));
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

        Singletons.getModel().getGame().getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        Singletons.getModel().getGame().getCombat().reset();
        for (final Card c : humanDevSetup) {
            human.getZone(ZoneType.Hand).add(c);
            Singletons.getModel().getGame().getAction().moveToPlay(c);
            c.setSickness(false);
        }

        for (final Card c : computerDevSetup) {
            ai.getZone(ZoneType.Hand).add(c);
            Singletons.getModel().getGame().getAction().moveToPlay(c);
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

        Singletons.getModel().getGame().getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        if (setComputerLife > 0) {
            ai.setLife(setComputerLife, null);
        }
        if (setHumanLife > 0) {
            human.setLife(setHumanLife, null);
        }

        Singletons.getModel().getGame().getAction().checkStateEffects();
        Singletons.getModel().getGame().getPhaseHandler().updateObservers();
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

            final Card c = CardDb.instance().getCard(cardinfo[0]).toForgeCard(player);

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
                        c.addCounter(CounterType.valueOf(counter), 1, true);
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
            Singletons.getModel().getGame().getAction().moveToHand(c);
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
        final Card o = GuiChoose.oneOrNone("Add counters to which card?", Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield));
        if (null == o) {
            return;
        } else {
            final Card c = o;
            final CounterType counter = GuiChoose.oneOrNone("Which type of counter?", CounterType.values());
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
                    c.addCounter(counter, i, false);
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
        final List<Card> play = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        final Object o = GuiChoose.oneOrNone("Choose a permanent", CardLists.filter(play, Predicates.not(CardPredicates.Presets.TAPPED)));
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
        final List<Card> play = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        final Object o = GuiChoose.oneOrNone("Choose a permanent", CardLists.filter(play, CardPredicates.Presets.TAPPED));
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
        final List<Player> players = Singletons.getModel().getGame().getPlayers();
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

    /**
     * <p>
     * devModeTutorAnyCard.
     * </p>
     * 
     * @since 1.2.7
     */
    public static void devModeCardToHand() {
        final List<Player> players = Singletons.getModel().getGame().getPlayers();
        final Player p = GuiChoose.oneOrNone("Put card in hand for which player?", players);
        if (null == p) {
            return;
        }

        final List<CardPrinted> cards =  Lists.newArrayList(CardDb.instance().getUniqueCards());
        Collections.sort(cards);

        // use standard forge's list selection dialog
        final IPaperCard c = GuiChoose.oneOrNone("Name the card", cards);
        if (c == null) {
            return;
        }

        Card forgeCard = c.toForgeCard(p);
        Singletons.getModel().getGame().getAction().moveToHand(forgeCard);

    }

    public static void devModeCardToBattlefield() {
        final List<Player> players = Singletons.getModel().getGame().getPlayers();
        final Player p = GuiChoose.oneOrNone("Put card in play for which player?", players);
        if (null == p) {
            return;
        }

        final List<CardPrinted> cards =  Lists.newArrayList(CardDb.instance().getUniqueCards());
        Collections.sort(cards);

        // use standard forge's list selection dialog
        final IPaperCard c = GuiChoose.oneOrNone("Name the card", cards);
        if (c == null) {
            return;
        }

        Card forgeCard = c.toForgeCard(p);

        final GameState game = Singletons.getModel().getGame();
        if (forgeCard.getType().contains("Land")) {
            forgeCard.setOwner(p);
            game.getAction().moveToPlay(forgeCard);
        } else {
            final List<SpellAbility> choices = forgeCard.getBasicSpells();
            if (choices.isEmpty()) {
                return; // when would it happen?
            }

            final SpellAbility sa = choices.size() == 1 ? choices.get(0) : GuiChoose.oneOrNone("Choose", choices);
            if (sa == null) {
                return; // happens if cancelled
            }

            sa.setActivatingPlayer(p);
            game.getAction().moveToHand(forgeCard); // this is really needed
            game.getActionPlay().playSpellAbilityForFree(sa);
        }


    }

    public static void devModeBreakpoint() {
        List<Player> Players = Singletons.getModel().getGame().getPlayers();
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
            // player has no stack of his own
        }

        //Set a breakpoint on the following statement
        System.out.println("Manual Breakpoint");
    }
    
    public static void devModeRiggedPlanarRoll()
    {
        final List<Player> players = Singletons.getModel().getGame().getPlayers();
        final Player p = GuiChoose.oneOrNone("Which player should roll?", players);
        if (null == p) {
            return;
        }
        
        PlanarDice res = GuiChoose.oneOrNone("Choose result", PlanarDice.values());
        if(res == null)
            return;
        
        PlanarDice.roll(p, res);
        
        Singletons.getModel().getGame().getStack().chooseOrderOfSimultaneousStackEntryAll();
    }

} // end class GuiDisplayUtil
