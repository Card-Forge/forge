package forge.control.match;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JOptionPane;

import net.slightlymagic.braids.util.ImmutableIterableFrom;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Constant.Zone;
import forge.GuiDisplayUtil;
import forge.Player;
import forge.PlayerZone;
import forge.card.cardfactory.CardFactoryUtil;
import forge.deck.Deck;
import forge.gui.ForgeAction;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.InputAttack;
import forge.gui.input.InputBlock;
import forge.gui.input.InputPayManaCost;
import forge.gui.input.InputPayManaCostAbility;
import forge.item.CardPrinted;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.properties.NewConstants.Lang.GuiDisplay.ComputerHand;
import forge.properties.NewConstants.Lang.GuiDisplay.ComputerLibrary;
import forge.view.match.ViewField;
import forge.view.match.ViewTopLevel;

/** 
 * Child controller, applied to single field in battlefield.
 * Manages player view functions such as card observers,
 * life total changes, graveyard button click, etc.
 *
 */
public class ControlField {
    private Player player;
    private ViewField view;

    /** 
     * Child controller, applied to single field in battlefield.
     * Manages player view functions such as card observers,
     * life total changes, graveyard button click, etc.
     *
     * @param p &emsp; The Player this field applies to
     * @param v &emsp; The Swing component for this field
     */
    public ControlField(Player p, ViewField v) {
        player = p;
        view = v;
    }

    /** @return Player */
    public Player getPlayer() {
        return player;
    }

    /** @return ViewField */
    public ViewField getView() {
        return view;
    }

    /**
     * Adds observers to field components where required: card stats, player stats, etc.
     */
    public void addObservers() {
        // Hand, Graveyard, Library, Flashback, Exile totals, attached to respective Zones.
        Observer o1 = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                view.updateZones(player);
            }
        };

        player.getZone(Zone.Hand).addObserver(o1);

        // Life total, poison total, and keywords, attached directly to Player.
        Observer o2 = new Observer() {
            @Override
            public void update(final Observable a, final Object b) {
                view.updateDetails(player);
            }
        };
        player.addObserver(o2);

        // Card play area, attached to battlefield zone.
        Observer o3 = new Observer() {
            public void update(final Observable a, final Object b) {
                PlayerZone pZone = (PlayerZone) a;
                Card[] c = pZone.getCards(false);
                GuiDisplayUtil.setupPlayZone(view.getTabletop(), c);
            }
        };

        player.getZone(Zone.Battlefield).addObserver(o3);
    }

    /**
     * Listeners for user actions on the battlefield.
     * 
     */
    public void addListeners() {
        // When/if zone action properties become less specific, the conditional
        // tests for computer/human players can be removed.  If that's not ever
        // going to happen, this comment can be removed. :)  Doublestrike 29-10-11.

        this.addZoneListeners();

        // Battlefield card clicks
        view.getTabletop().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ViewTopLevel t = (ViewTopLevel) AllZone.getDisplay();
                Card c = t.getCardviewerController().getCurrentCard();
                Input input = t.getInputController().getInputControl().getInput();

                if (c != null) {
                    if (c.isTapped()
                            && (input instanceof InputPayManaCost
                            || input instanceof InputPayManaCostAbility)) {
                        arcane.ui.CardPanel cardPanel = view.getTabletop().getCardPanel(c.getUniqueNumber());
                        for (arcane.ui.CardPanel cp : cardPanel.getAttachedPanels()) {
                            if (cp.getCard().isUntapped()) {
                                break;
                            }
                        }
                    }

                    CardList att = new CardList(AllZone.getCombat().getAttackers());
                    if ((c.isTapped() || c.hasSickness() || ((c.hasKeyword("Vigilance")) && att.contains(c)))
                            && (input instanceof InputAttack)) {
                        arcane.ui.CardPanel cardPanel = view.getTabletop().getCardPanel(c.getUniqueNumber());
                        for (arcane.ui.CardPanel cp : cardPanel.getAttachedPanels()) {
                            if (cp.getCard().isUntapped() && !cp.getCard().hasSickness()) {
                                break;
                            }
                        }
                    }

                    if (e.isMetaDown()) {
                        if (att.contains(c) && (input instanceof InputAttack)
                                && !c.hasKeyword("CARDNAME attacks each turn if able.")) {
                            c.untap();
                            AllZone.getCombat().removeFromCombat(c);
                        } else if (input instanceof InputBlock) {
                            if (c.getController().isHuman()) {
                                AllZone.getCombat().removeFromCombat(c);
                            }
                            ((InputBlock) input).removeFromAllBlocking(c);
                        }
                    }
                    else {
                        t.getInputController().getInputControl().selectCard(c, AllZone.getHumanPlayer().getZone(Zone.Battlefield));
                    }
                }
            }
        });

        // Battlefield card mouseover
        view.getTabletop().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                ViewTopLevel t = (ViewTopLevel) AllZone.getDisplay();
                Card c = view.getTabletop().getCardFromMouseOverPanel();
                if (c != null) {
                    t.getCardviewerController().showCard(c);
                }
            }
        });

        // Player select
        view.getLblLife().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ViewTopLevel t = (ViewTopLevel) AllZone.getDisplay();
                if (player.isComputer()) {
                    t.getInputController().getInputControl().selectPlayer(AllZone.getComputerPlayer());
                }
                else {
                    t.getInputController().getInputControl().selectPlayer(AllZone.getHumanPlayer());
                }
            }
        });
    } // End addListeners()

    /** Adds listeners to "zone" labels: flashback, graveyard, etc.
     * This method only exists to avoid the 150-line limit in the checkstyle rules.
     */
    private void addZoneListeners() {
        // Graveyard card list button
        view.getLblGraveyard().enableHover();
        view.getLblGraveyard().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (player.isComputer()) {
                    new ZoneAction(player.getZone(Zone.Graveyard),
                            NewConstants.Lang.GuiDisplay.COMPUTER_GRAVEYARD).actionPerformed(null);
                }
                else {
                    new ZoneAction(player.getZone(Zone.Graveyard),
                            NewConstants.Lang.GuiDisplay.HUMAN_GRAVEYARD).actionPerformed(null);
                }
            }
        });
        // Exile card list button
        view.getLblExile().enableHover();
        view.getLblExile().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (player.isComputer()) {
                    new ZoneAction(player.getZone(Zone.Exile),
                            NewConstants.Lang.GuiDisplay.COMPUTER_REMOVED).actionPerformed(null);
                }
                else {
                    new ZoneAction(player.getZone(Zone.Exile),
                            NewConstants.Lang.GuiDisplay.HUMAN_REMOVED).actionPerformed(null);
                }
            }
        });
        // Library card list button
        view.getLblLibrary().enableHover();
        view.getLblLibrary().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!player.isComputer()) {
                    new DeckListAction(NewConstants.Lang.GuiDisplay.HUMAN_DECKLIST).actionPerformed(null);
                }
                else {
                    // TODO DeckListAction has been rewritten to accept either human or computer
                    // decklists.  However, NewConstants.Lang.GuiDisplay does not have a computer
                    // decklist available.  That needs to be added for the below line to work
                    // properly. The current solution will work in the meantime. Doublestrike 15-11-11.

                    //new DeckListAction(NewConstants.Lang.GuiDisplay).actionPerformed(null);

                    new ZoneAction(player.getZone(Zone.Library), ComputerLibrary.BASE).actionPerformed(null);
                }
            }
        });
        // Flashback card list button
        view.getLblFlashback().enableHover();
        view.getLblFlashback().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!player.isComputer()) {
                    new ZoneAction(player.getZone(Zone.Graveyard), NewConstants.Lang.GuiDisplay.HUMAN_FLASHBACK) {

                        private static final long serialVersionUID = 8120331222693706164L;

                        @Override
                        protected Iterable<Card> getCardsAsIterable() {
                            return new ImmutableIterableFrom<Card>(CardFactoryUtil.getExternalZoneActivationCards(AllZone
                                    .getHumanPlayer()));
                        }

                        @Override
                        protected void doAction(final Card c) {
                            AllZone.getGameAction().playCard(c);
                        }
                    };
                }
            }
        });
        // Hand button
        if (player.isComputer()) {
            view.getLblHand().enableHover();

            view.getLblHand().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                        new ZoneAction(player.getZone(Zone.Hand), ComputerHand.BASE).actionPerformed(null);
                }
            });
        }
    }

    /**
     * Resets all phase buttons to "inactive", so highlight won't be
     * drawn on them. "Enabled" state remains the same.
     */
    public void resetPhaseButtons() {
        view.getLblUpkeep().setActive(false);
        view.getLblDraw().setActive(false);
        view.getLblBeginCombat().setActive(false);
        view.getLblEndCombat().setActive(false);
        view.getLblEndTurn().setActive(false);
    }

    /**
     * Receives click and programmatic requests for viewing data stacks
     * in the "zones" of a player field: hand, library, etc.
     * 
     */
    private class ZoneAction extends ForgeAction {
        private static final long serialVersionUID = -5822976087772388839L;
        private PlayerZone zone;
        private String title;

        /** 
         * Receives click and programmatic requests for viewing data stacks
         * in the "zones" of a player field: hand, graveyard, etc. The library
         * "zone" is an exception to the rule; it's handled in DeckListAction.
         * 
         * @param zone &emsp; PlayerZone obj
         * @param property &emsp; String obj
         */
        public ZoneAction(final PlayerZone zone, final String property) {
            super(property);
            title = ForgeProps.getLocalized(property + "/title");
            this.zone = zone;
        }

        /** @param e &emsp; ActionEvent obj */
        public void actionPerformed(final ActionEvent e) {
            Generator<Card> c = YieldUtils.toGenerator(getCardsAsIterable());

            if (AllZone.getNameChanger().shouldChangeCardName()) {
                c = AllZone.getNameChanger().changeCard(c);
            }

            Iterable<Card> myIterable = YieldUtils.toIterable(c);
            ArrayList<Card> choices = YieldUtils.toArrayList(myIterable);
            //System.out.println("immediately after: "+choices);
            //Iterator<Card> iter = myIterable.iterator();

            ArrayList<Card> choices2 = new ArrayList<Card>();

            if (choices.isEmpty()) {
                GuiUtils.getChoiceOptional(title, new String[]{"no cards"});
            }
            else {
                for (int i = 0; i < choices.size(); i++) {
                    Card crd = choices.get(i);
                    //System.out.println(crd+": "+crd.isFaceDown());
                    if (crd.isFaceDown()) {
                        Card faceDown = new Card();
                        faceDown.setName("Face Down");
                        choices2.add(faceDown);
                        //System.out.println("Added: "+faceDown);
                    }
                    else {
                        choices2.add(crd);
                    }
                }
                //System.out.println("Face down cards replaced: "+choices2);
                Card choice = (Card) GuiUtils.getChoiceOptional(title, choices2.toArray());
                if (choice != null) {
                    doAction(choice);
                    /*
                        Card choice = GuiUtils.getChoiceOptional(title, iter);
                        if (choice != null) doAction(choice);
                     */
                }
            }
        }

        protected Iterable<Card> getCardsAsIterable() {
            return new ImmutableIterableFrom<Card>(Arrays.asList(zone.getCards()));
        }

        protected void doAction(final Card c) {
        }
    } // End ZoneAction

    /**
     * Receives click and programmatic requests for viewing a player's
     * library (typically used in dev mode).  Allows copy of the
     * cardlist to clipboard.
     *
     */
    private class DeckListAction extends ForgeAction {
        public DeckListAction(final String property) {
            super(property);
        }

        private static final long serialVersionUID = 9874492387239847L;

        public void actionPerformed(final ActionEvent e) {
            Deck targetDeck;

            if (Constant.Runtime.HUMAN_DECK[0].countMain() > 1) {
                targetDeck = Constant.Runtime.HUMAN_DECK[0];
            }
            else if (Constant.Runtime.COMPUTER_DECK[0].countMain() > 1) {
                targetDeck = Constant.Runtime.COMPUTER_DECK[0];
            }
            else {
                return;
            }

            HashMap<String, Integer> deckMap = new HashMap<String, Integer>();

            for (Entry<CardPrinted, Integer> s : targetDeck.getMain()) {
                deckMap.put(s.getKey().getName(), s.getValue());
            }

            String nl = System.getProperty("line.separator");
            StringBuilder deckList = new StringBuilder();
            String dName = targetDeck.getName();

            if (dName == null) {
                dName = "";
            } else {
                deckList.append(dName + nl);
            }

            ArrayList<String> dmKeys = new ArrayList<String>();
            for (String s : deckMap.keySet()) {
                dmKeys.add(s);
            }

            Collections.sort(dmKeys);

            for (String s : dmKeys) {
                deckList.append(deckMap.get(s) + " x " + s + nl);
            }

            int rcMsg = -1138;
            String ttl = "Human's Decklist";
            if (!dName.equals("")) {
                ttl += " - " + dName;
            }

            StringBuilder msg = new StringBuilder();
            if (deckMap.keySet().size() <= 32) {
                msg.append(deckList.toString() + nl);
            } else {
                msg.append("Decklist too long for dialog." + nl + nl);
            }

            msg.append("Copy Decklist to Clipboard?");

            rcMsg = JOptionPane.showConfirmDialog(null, msg, ttl, JOptionPane.OK_CANCEL_OPTION);

            if (rcMsg == JOptionPane.OK_OPTION) {
                StringSelection ss = new StringSelection(deckList.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
            }
        }
    } // End DeckListAction
}
