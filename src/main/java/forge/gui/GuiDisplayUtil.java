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

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.CardPredicates;
import forge.CounterType;
import forge.FThreads;
import forge.Singletons;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.TriggerType;
import forge.game.GameState;
import forge.game.PlanarDice;
import forge.game.player.HumanPlay;
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

    public static void devModeGenerateMana() {
        final Card dummy = new Card();
        dummy.setOwner(getGame().getPhaseHandler().getPriorityPlayer());
        Map<String, String> produced = new HashMap<String, String>();
        produced.put("Produced", "W W W W W W W U U U U U U U B B B B B B B G G G G G G G R R R R R R R 7");
        final AbilityManaPart abMana = new AbilityManaPart(dummy, produced);
        abMana.produceMana(null);
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
        final GameState game = getGame();

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

        final Player human = getGame().getPlayers().get(0);
        final Player ai = getGame().getPlayers().get(1);

        if (!tChangePlayer.trim().toLowerCase().equals("none")) {
            if (tChangePlayer.trim().toLowerCase().equals("human")) {
                game.getPhaseHandler().setPlayerTurn(human);
            }
            if (tChangePlayer.trim().toLowerCase().equals("ai")) {
                game.getPhaseHandler().setPlayerTurn(ai);
            }
        }



        if (!tChangePhase.trim().toLowerCase().equals("none")) {
            game.getPhaseHandler().setDevPhaseState(forge.game.phase.PhaseType.smartValueOf(tChangePhase));
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

        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getCombat().reset(game.getPhaseHandler().getPlayerTurn());
        for (final Card c : humanDevSetup) {
            human.getZone(ZoneType.Hand).add(c);
            game.getAction().moveToPlay(c);
            c.setSickness(false);
        }

        for (final Card c : computerDevSetup) {
            ai.getZone(ZoneType.Hand).add(c);
            game.getAction().moveToPlay(c);
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

        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        if (setComputerLife > 0) {
            ai.setLife(setComputerLife, null);
        }
        if (setHumanLife > 0) {
            human.setLife(setHumanLife, null);
        }

        game.getAction().checkStateEffects();
        game.getPhaseHandler().updateObservers();
        for (Player p : game.getRegisteredPlayers()) {
            p.getZone(ZoneType.Battlefield).updateObservers();
        }
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
    private static List<Card> devProcessCardsForZone(final String[] data, final Player player) {
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
        final List<Card> lib = getGame().getPhaseHandler().getPriorityPlayer().getCardsIn(ZoneType.Library);
        final Object o = GuiChoose.oneOrNone("Choose a card", lib);
        if (null == o) {
            return;
        } else {
            final Card c = (Card) o;
            getGame().getAction().moveToHand(c);
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
        final Card o = GuiChoose.oneOrNone("Add counters to which card?", getGame().getCardsIn(ZoneType.Battlefield));
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
        final List<Card> play = getGame().getCardsIn(ZoneType.Battlefield);
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
        final List<Card> play = getGame().getCardsIn(ZoneType.Battlefield);
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
     * devModeSetLife.
     * </p>
     * 
     * @since 1.1.3
     */
    public static void devModeSetLife() {
        final List<Player> players = getGame().getPlayers();
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
        final List<Player> players = getGame().getPlayers();
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
        getGame().getAction().moveToHand(forgeCard);

    }

    public static void devModeCardToBattlefield() {
        final List<Player> players = getGame().getPlayers();
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

        final Card forgeCard = c.toForgeCard(p);

        final GameState game = getGame();
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

            FThreads.invokeInNewThread(new Runnable() {
                @Override
                public void run() {
                    game.getAction().moveToHand(forgeCard); // this is really needed (for rollbacks at least) 
                    // Human player is choosing targets for an ability controlled by chosen player. 
                    sa.setActivatingPlayer(p);
                    HumanPlay.playSaWithoutPayingManaCost(game.getPhaseHandler().getPriorityPlayer(), sa);
                }
            });
        }


    }

    public static void devModeBreakpoint() {
        List<Player> Players = getGame().getPlayers();
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
        final List<Player> players = getGame().getPlayers();
        final Player p = GuiChoose.oneOrNone("Which player should roll?", players);
        if (null == p) {
            return;
        }
        
        PlanarDice res = GuiChoose.oneOrNone("Choose result", PlanarDice.values());
        if(res == null)
            return;
        
        System.out.println("Rigging planar dice roll: " + res.toString());

        //DBG
        //System.out.println("ActivePlanes: " + getGame().getActivePlanes());
        //System.out.println("CommandPlanes: " + getGame().getCardsIn(ZoneType.Command));
        
        PlanarDice.roll(p, res);
        
        FThreads.invokeInNewThread(new Runnable() {
            @Override
            public void run() {
                p.getGame().getStack().chooseOrderOfSimultaneousStackEntryAll();
            }
        });
        
        
    }
    
    private static GameState getGame() {
        return Singletons.getControl().getObservedGame();
    }


} // end class GuiDisplayUtil
