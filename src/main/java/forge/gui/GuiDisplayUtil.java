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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.CardPredicates;
import forge.CounterType;
import forge.Singletons;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.TriggerType;
import forge.game.Game;
import forge.game.GameType;
import forge.game.PlanarDice;
import forge.game.phase.PhaseType;
import forge.game.player.HumanPlay;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.input.InputSelectCardsFromList;
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
        getGame().getAction().invoke(new Runnable() {
            @Override public void run() { abMana.produceMana(null); }
        });
    }

    public static void devSetupGameState() {
        int humanLife = -1;
        int computerLife = -1;

        final Map<ZoneType, String> humanCardTexts = new EnumMap<ZoneType, String>(ZoneType.class);
        final Map<ZoneType, String> aiCardTexts = new EnumMap<ZoneType, String>(ZoneType.class);

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
                if (tempData.length < 2 || temp.charAt(0) == '#') {
                    continue;
                }

                final String categoryName = tempData[0].toLowerCase();
                final String categoryValue = tempData[1];

                if (categoryName.equals("humanlife"))                   humanLife = Integer.parseInt(categoryValue);
                else if (categoryName.equals("ailife"))                 computerLife = Integer.parseInt(categoryValue);

                else if (categoryName.equals("activeplayer"))           tChangePlayer = categoryValue.trim().toLowerCase();
                else if (categoryName.equals("activephase"))            tChangePhase = categoryValue;

                else if (categoryName.equals("humancardsinplay"))       humanCardTexts.put(ZoneType.Battlefield, categoryValue);
                else if (categoryName.equals("aicardsinplay"))          aiCardTexts.put(ZoneType.Battlefield, categoryValue);
                else if (categoryName.equals("humancardsinhand"))       humanCardTexts.put(ZoneType.Hand, categoryValue);
                else if (categoryName.equals("aicardsinhand"))          aiCardTexts.put(ZoneType.Hand, categoryValue);
                else if (categoryName.equals("humancardsingraveyard"))  humanCardTexts.put(ZoneType.Graveyard, categoryValue);
                else if (categoryName.equals("aicardsingraveyard"))     aiCardTexts.put(ZoneType.Graveyard, categoryValue);
                else if (categoryName.equals("humancardsinlibrary"))    humanCardTexts.put(ZoneType.Library, categoryValue);
                else if (categoryName.equals("aicardsinlibrary"))       aiCardTexts.put(ZoneType.Library, categoryValue);
                else if (categoryName.equals("humancardsinexile"))      humanCardTexts.put(ZoneType.Exile, categoryValue);
                else if (categoryName.equals("aicardsinexile"))         aiCardTexts.put(ZoneType.Exile, categoryValue);
                
            }

            in.close();
        } catch (final FileNotFoundException fnfe) {
            JOptionPane.showMessageDialog(null, "File not found: " + fc.getSelectedFile().getAbsolutePath());
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading battle setup file!");
            return;
        }

        setupGameState(humanLife, computerLife, humanCardTexts, aiCardTexts, tChangePlayer, tChangePhase);
    }

    private static void setupGameState(final int humanLife, final int computerLife, final Map<ZoneType, String> humanCardTexts,
            final Map<ZoneType, String> aiCardTexts, final String tChangePlayer, final String tChangePhase) {
        
        final Game game = getGame();
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                final Player human = game.getPlayers().get(0);
                final Player ai = game.getPlayers().get(1);

                Player newPlayerTurn = tChangePlayer.equals("human") ? newPlayerTurn = human : tChangePlayer.equals("ai") ? newPlayerTurn = ai : null;
                PhaseType newPhase = tChangePhase.trim().equalsIgnoreCase("none") ? null : PhaseType.smartValueOf(tChangePhase);
                
                game.getPhaseHandler().devModeSet(newPhase, newPlayerTurn);
              
                game.getCombat().reset(game.getPhaseHandler().getPlayerTurn());
                game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
              
                devSetupPlayerState(humanLife, humanCardTexts, human);
                devSetupPlayerState(computerLife, aiCardTexts, ai);
              
                game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
              
              
                game.getAction().checkStateEffects();
                game.getPhaseHandler().updateObservers();
                for (Player p : game.getRegisteredPlayers()) {
                    p.getZone(ZoneType.Battlefield).updateObservers();
                }
            }
        });
    }

    private static void devSetupPlayerState(int life, Map<ZoneType, String> cardTexts, final Player p) {
        Map<ZoneType, List<Card>> humanCards = new EnumMap<ZoneType, List<Card>>(ZoneType.class);
        for(Entry<ZoneType, String> kv : cardTexts.entrySet()) {
            humanCards.put(kv.getKey(), GuiDisplayUtil.devProcessCardsForZone(kv.getValue().split(";"), p));
        }

        if (life > 0) p.setLife(life, null);
        for (Entry<ZoneType, List<Card>> kv : humanCards.entrySet()) {
            if (kv.getKey() == ZoneType.Battlefield) {
                for (final Card c : kv.getValue()) {
                    p.getZone(ZoneType.Hand).add(c);
                    p.getGame().getAction().moveToPlay(c);
                    c.setSickness(false);
                }
            } else {
                p.getZone(kv.getKey()).setCards(kv.getValue());
            }
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
        final Game game = getGame();
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                final List<Card> untapped = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Predicates.not(CardPredicates.Presets.TAPPED));
                InputSelectCardsFromList inp = new InputSelectCardsFromList(0, Integer.MAX_VALUE, untapped);
                inp.setCancelAllowed(true);
                inp.setMessage("Choose permanents to tap");
                Singletons.getControl().getInputQueue().setInputAndWait(inp);
                if( !inp.hasCancelled() )
                    for(Card c : inp.getSelected())
                        c.tap();
            }
        });
    }

    /**
     * <p>
     * devModeUntapPerm.
     * </p>
     * 
     * @since 1.0.15
     */
    public static void devModeUntapPerm() {
        final Game game = getGame();
        
        

        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                final List<Card> tapped = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.TAPPED);
                InputSelectCardsFromList inp = new InputSelectCardsFromList(0, Integer.MAX_VALUE, tapped);
                inp.setCancelAllowed(true);
                inp.setMessage("Choose permanents to untap");
                Singletons.getControl().getInputQueue().setInputAndWait(inp);
                if( !inp.hasCancelled() )
                    for(Card c : inp.getSelected())
                        c.untap();
            }
        });
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

        final Game game = getGame();
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

            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    game.getAction().moveToHand(forgeCard); // this is really needed (for rollbacks at least) 
                    // Human player is choosing targets for an ability controlled by chosen player. 
                    sa.setActivatingPlayer(p);
                    HumanPlay.playSaWithoutPayingManaCost(game, sa);
                    Singletons.getControl().getInputQueue().updateObservers(); // priority can be on AI side, need this update for that case
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
        
        getGame().getAction().invoke(new Runnable() {
            @Override
            public void run() {
                p.getGame().getStack().chooseOrderOfSimultaneousStackEntryAll();
            }
        });
        
    }

    public static void devModePlaneswalkTo() {
        final Game game = getGame();
        if (game.getMatch().getGameType() != GameType.Planechase) { return; }
        final Player p = game.getPhaseHandler().getPlayerTurn();

        final List<CardPrinted> allPlanars = new ArrayList<CardPrinted>();
        for (CardPrinted c : CardDb.variants().getAllCards()) {
            if (c.getRules().getType().isPlane() || c.getRules().getType().isPhenomenon()) {
                allPlanars.add(c);
            }
        }
        Collections.sort(allPlanars);

        // use standard forge's list selection dialog
        final IPaperCard c = GuiChoose.oneOrNone("Name the card", allPlanars);
        if (c == null) { return; }
        final Card forgeCard = c.toForgeCard(p);

        forgeCard.setOwner(p);
        game.getAction().changeZone(null, p.getZone(ZoneType.PlanarDeck), forgeCard, 0);
        PlanarDice.roll(p, PlanarDice.Planeswalk);

        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                p.getGame().getStack().chooseOrderOfSimultaneousStackEntryAll();
            }
        });
    }

    private static Game getGame() {
        return Singletons.getControl().getObservedGame();
    }


} // end class GuiDisplayUtil
