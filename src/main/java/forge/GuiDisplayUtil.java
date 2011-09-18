package forge;


import arcane.ui.PlayArea;
import arcane.ui.util.Animation;
import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.mana.ManaPool;
import forge.card.spellability.Ability_Mana;
import forge.card.trigger.Trigger;
import forge.gui.GuiUtils;
import forge.gui.game.CardPanel;
import forge.properties.NewConstants;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * <p>GuiDisplayUtil class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public final class GuiDisplayUtil implements NewConstants {

    private GuiDisplayUtil() {
        throw new AssertionError();
    }
    /**
     * <p>getCardDetailMouse.</p>
     *
     * @param visual a {@link forge.CardContainer} object.
     * @return a {@link java.awt.event.MouseMotionListener} object.
     */
    public static MouseMotionListener getCardDetailMouse(final CardContainer visual) {
        return new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                JPanel panel = (JPanel) me.getSource();
                Object o = panel.getComponentAt(me.getPoint());

                if ((o != null) && (o instanceof CardPanel)) {
                    CardContainer cardPanel = (CardContainer) o;
                    visual.setCard(cardPanel.getCard());
                }
            } //mouseMoved
        };
    }

    /**
     * <p>getBorder.</p>
     *
     * @param card a {@link forge.Card} object.
     * @return a {@link javax.swing.border.Border} object.
     */
    public static Border getBorder(final Card card) {
        // color info
        if (card == null) {
            return BorderFactory.createEmptyBorder(2, 2, 2, 2);
        }
        java.awt.Color color;
        ArrayList<String> list = CardUtil.getColors(card);

        if (card.isFaceDown()) {
            color = Color.gray;
        } else if (list.size() > 1) {
            color = Color.orange;
        } else if (list.get(0).equals(Constant.Color.Black)) {
            color = Color.black;
        } else if (list.get(0).equals(Constant.Color.Green)) {
            color = new Color(0, 220, 39);
        } else if (list.get(0).equals(Constant.Color.White)) {
            color = Color.white;
        } else if (list.get(0).equals(Constant.Color.Red)) {
            color = Color.red;
        } else if (list.get(0).equals(Constant.Color.Blue)) {
            color = Color.blue;
        } else if (list.get(0).equals(Constant.Color.Colorless)) {
            color = Color.gray;
        } else {
            color = new Color(200, 0, 230); // If your card has a violet border, something is wrong
        }

        if (color != Color.gray) {

            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();

            int shade = 10;

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
     * <p>devModeGenerateMana.</p>
     */
    public static void devModeGenerateMana() {
        Card dummy = new Card();
        dummy.setOwner(AllZone.getHumanPlayer());
        dummy.addController(AllZone.getHumanPlayer());
        Ability_Mana abMana = new Ability_Mana(dummy, "0", "W U B G R 1", 10) {
            private static final long serialVersionUID = -2164401486331182356L;

        };
        abMana.produceMana();
    }

    /**
     * <p>formatCardType.</p>
     *
     * @param card a {@link forge.Card} object.
     * @return a {@link java.lang.String} object.
     */
    public static String formatCardType(final Card card) {
        ArrayList<String> list = card.getType();
        StringBuilder sb = new StringBuilder();

        ArrayList<String> superTypes = new ArrayList<String>();
        ArrayList<String> cardTypes = new ArrayList<String>();
        ArrayList<String> subTypes = new ArrayList<String>();
        boolean allCreatureTypes = list.contains("AllCreatureTypes");
        
        for (String t : list) {
            if(allCreatureTypes && t.equals("AllCreatureTypes")) {
                continue;
            } 
            if (CardUtil.isASuperType(t) && !superTypes.contains(t)) {
                superTypes.add(t);
            }
            if (CardUtil.isACardType(t) && !cardTypes.contains(t)) {
                cardTypes.add(t);
            }
            if (CardUtil.isASubType(t) && !subTypes.contains(t)
                    && (!allCreatureTypes || !CardUtil.isACreatureType(t))) {
                subTypes.add(t);
            }
        }

        for (String type : superTypes) {
            sb.append(type).append(" ");
        }
        for (String type : cardTypes) {
            sb.append(type).append(" ");
        }
        if (!subTypes.isEmpty() || allCreatureTypes) {
            sb.append("- ");
        }
        if (allCreatureTypes) {
            sb.append("All creature types ");
        }
        for (String type : subTypes) {
            sb.append(type).append(" ");
        }

        return sb.toString();
    }

    /**
     * <p>cleanString.</p>
     *
     * @param in a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String cleanString(final String in) {
        StringBuffer out = new StringBuffer();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if (c == ' ' || c == '-') {
                out.append('_');
            } else if (Character.isLetterOrDigit(c) || c == '_') {
                out.append(c);
            }
        }
        return out.toString().toLowerCase();
    }

    /**
     * <p>cleanStringMWS.</p>
     *
     * @return a {@link java.lang.String} object.
     * @param in a {@link java.lang.String} object.
     */
    public static String cleanStringMWS(final String in) {
        StringBuffer out = new StringBuffer();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if (c == '"' || c == '/') {
                out.append("");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * <p>setupNoLandPanel.</p>
     *
     * @param j a {@link javax.swing.JPanel} object.
     * @param c an array of {@link forge.Card} objects.
     */
    public static void setupNoLandPanel(final JPanel j, final Card[] c) {
        ArrayList<Card> a = new ArrayList<Card>();
        /*
        for(int i = 0; i < c.length; i++)
          if(c[i].isCreature() || c[i].isGlobalEnchantment() || c[i].isArtifact() || c[i].isPlaneswalker())
            a.add(c[i]);
         */

        /*

        //creatures or planeswalkers
        for(int i = 0; i < c.length; i++)
        	//!artifact because of Memnarch turning planeswalkers into artifacts.
        	if (c[i].isCreature() || (c[i].isPlaneswalker() && !c[i].isArtifact()))
        		a.add(c[i]);
        //(noncreature, non-enchantment,nonland) artifacts
        for(int i = 0; i < c.length; i++)
        	if (c[i].isArtifact() && !c[i].isCreature() && !c[i].isLand() && !c[i].isGlobalEnchantment() )
        		a.add(c[i]);
        //(noncreature) enchantments
        for(int i = 0; i < c.length; i++)
        	if (c[i].isGlobalEnchantment() && !c[i].isCreature())
        		a.add(c[i]);

         */

        for (int i = 0; i < c.length; i++) {
            a.add(c[i]);
        }

        setupNoLandPermPanel(j, a, true);
    }

    /**
     * <p>setupLandPanel.</p>
     *
     * @param j a {@link javax.swing.JPanel} object.
     * @param c an array of {@link forge.Card} objects.
     */
    public static void setupLandPanel(final JPanel j, final Card[] c) {
        ArrayList<Card> a = new ArrayList<Card>();
        for (int i = 0; i < c.length; i++) {
            if ((!(c[i].isCreature() || c[i].isEnchantment() || c[i].isArtifact() || c[i].isPlaneswalker())
                    || (c[i].isLand() && c[i].isArtifact() && !c[i].isCreature() && !c[i].isEnchantment()))
                    && !AllZone.getGameAction().isAttachee(c[i])
                    || (c[i].getName().startsWith("Mox") && !c[i].getName().equals("Mox Diamond")))
            {
                a.add(c[i]);
            }
        }
        setupPanel(j, a, true);
    }

    /*
    private static void setupPanel(JPanel p, ArrayList<Card> list) {
      setupPanel(p, list, false);
    }
     */

    //list holds Card objects
    //puts local enchanments in the right order
    //adds "<<" to local enchanments names
    /**
     * <p>setupPanel.</p>
     *
     * @param p a {@link javax.swing.JPanel} object.
     * @param list a {@link java.util.ArrayList} object.
     * @param stack a boolean.
     */
    private static void setupPanel(final JPanel p, ArrayList<Card> list, final boolean stack) {

        int maxY = 0;
        int maxX = 0;
        //remove all local enchantments

        Card c;
        /*
        for(int i = 0; i < list.size(); i++)
        {
          c = (Card)list.get(i);
          if(c.isLocalEnchantment())
            list.remove(i);
        }

        //add local enchantments to the permanents
        //put local enchantments "next to" the permanent they are enchanting
        //the inner for loop is backward so permanents with more than one local enchantments are in the right order
        Card ca[];
        for(int i = 0; i < list.size(); i++)
        {
          c = (Card)list.get(i);
          if(c.hasAttachedCards())
          {
            ca = c.getAttachedCards();
            for(int inner = ca.length - 1; 0 <= inner; inner--)
              list.add(i + 1, ca[inner]);
          }
        }
         */

        if (stack) {
            // add all Cards in list to the GUI, add arrows to Local Enchantments

            ArrayList<Card> manaPools = getManaPools(list);
            ArrayList<Card> enchantedLands = getEnchantedLands(list);
            ArrayList<Card> basicBlues = getBasics(list, Constant.Color.Blue);
            ArrayList<Card> basicReds = getBasics(list, Constant.Color.Red);
            ArrayList<Card> basicBlacks = getBasics(list, Constant.Color.Black);
            ArrayList<Card> basicGreens = getBasics(list, Constant.Color.Green);
            ArrayList<Card> basicWhites = getBasics(list, Constant.Color.White);
            ArrayList<Card> badlands = getNonBasicLand(list, "Badlands");
            ArrayList<Card> bayou = getNonBasicLand(list, "Bayou");
            ArrayList<Card> plateau = getNonBasicLand(list, "Plateau");
            ArrayList<Card> scrubland = getNonBasicLand(list, "Scrubland");
            ArrayList<Card> savannah = getNonBasicLand(list, "Savannah");
            ArrayList<Card> taiga = getNonBasicLand(list, "Taiga");
            ArrayList<Card> tropicalIsland = getNonBasicLand(list, "Tropical Island");
            ArrayList<Card> tundra = getNonBasicLand(list, "Tundra");
            ArrayList<Card> undergroundSea = getNonBasicLand(list, "Underground Sea");
            ArrayList<Card> volcanicIsland = getNonBasicLand(list, "Volcanic Island");

            ArrayList<Card> nonBasics = getNonBasics(list);

            ArrayList<Card> moxEmerald = getMoxen(list, "Mox Emerald");
            ArrayList<Card> moxJet = getMoxen(list, "Mox Jet");
            ArrayList<Card> moxPearl = getMoxen(list, "Mox Pearl");
            ArrayList<Card> moxRuby = getMoxen(list, "Mox Ruby");
            ArrayList<Card> moxSapphire = getMoxen(list, "Mox Sapphire");
            //ArrayList<Card> moxDiamond = getMoxen(list, "Mox Diamond");

            list = new ArrayList<Card>();
            list.addAll(manaPools);
            list.addAll(enchantedLands);
            list.addAll(basicBlues);
            list.addAll(basicReds);
            list.addAll(basicBlacks);
            list.addAll(basicGreens);
            list.addAll(basicWhites);
            list.addAll(badlands);
            list.addAll(bayou);
            list.addAll(plateau);
            list.addAll(scrubland);
            list.addAll(savannah);
            list.addAll(taiga);
            list.addAll(tropicalIsland);
            list.addAll(tundra);
            list.addAll(undergroundSea);
            list.addAll(volcanicIsland);

            list.addAll(nonBasics);

            list.addAll(moxEmerald);
            list.addAll(moxJet);
            list.addAll(moxPearl);
            list.addAll(moxRuby);
            list.addAll(moxSapphire);
            //list.addAll(moxDiamond);


            int atInStack = 0;

            int marginX = 5;
            int marginY = 5;

            int x = marginX;

            int cardOffset = Constant.Runtime.stackOffset[0];

            String color = "";
            ArrayList<JPanel> cards = new ArrayList<JPanel>();

            ArrayList<CardPanel> connectedCards = new ArrayList<CardPanel>();

            boolean nextEnchanted = false;
            Card prevCard = null;
            int nextXIfNotStacked = 0;
            for (int i = 0; i < list.size(); i++) {
                JPanel addPanel;
                c = list.get(i);

                addPanel = new CardPanel(c);


                boolean startANewStack = false;

                if (!isStackable(c)) {
                    startANewStack = true;
                } else {
                    String newColor = c.getName(); //CardUtil.getColor(c);

                    if (!newColor.equals(color)) {
                        startANewStack = true;
                        color = newColor;
                    }
                }

                if (i == 0) {
                    startANewStack = false;
                }


                if (!startANewStack && atInStack == Constant.Runtime.stackSize[0]) {
                    startANewStack = true;
                }


                if (c.isAura() && c.isEnchanting() && !nextEnchanted) {
                    startANewStack = false;
                } else if (c.isAura() && c.isEnchanting()) {
                    startANewStack = true;
                    nextEnchanted = false;
                }

                if (c.isLand() && c.isEnchanted()) {
                    startANewStack = false;
                    nextEnchanted = true;
                }


                //very hacky, but this is to ensure enchantment stacking occurs correctly when
                //a land is enchanted, and there are more lands of that same name

                else if ((prevCard != null && c.isLand() && prevCard.isLand() && prevCard.isEnchanted()
                        && prevCard.getName().equals(c.getName())))
                {
                    startANewStack = true;
                } else if (prevCard != null && c.isLand() && prevCard.isLand()
                        && !prevCard.getName().equals(c.getName()))
                {
                    startANewStack = true;
                }

                /*
                if (c.getName().equals("Squirrel Nest")) {
                    startANewStack = true;
                    System.out.println("startANewStack: " + startANewStack);
                }
                 */
                if (c.isAura() && c.isEnchanting() && prevCard != null && prevCard instanceof ManaPool) {
                    startANewStack = true;
                }
                if (c instanceof ManaPool && prevCard instanceof ManaPool && prevCard.isSnow()) {
                    startANewStack = false;
                }

                if (startANewStack) {
                    setupConnectedCards(connectedCards);
                    connectedCards.clear();

                    // Fixed distance if last was a stack, looks a bit nicer
                    if (atInStack > 1) {
                        x += Math.max(addPanel.getPreferredSize().width, addPanel.getPreferredSize().height)
                        + marginX;
                    } else {
                        x = nextXIfNotStacked;
                    }

                    atInStack = 0;
                } else {
                    if (i != 0) {
                        x += cardOffset;
                    }
                }

                nextXIfNotStacked = x + marginX + addPanel.getPreferredSize().width;

                int xLoc = x;

                int yLoc = marginY;
                yLoc += atInStack * cardOffset;

                addPanel.setLocation(new Point(xLoc, yLoc));
                addPanel.setSize(addPanel.getPreferredSize());


                cards.add(addPanel);

                connectedCards.add((CardPanel) addPanel);

                atInStack++;
                prevCard = c;
            }

            setupConnectedCards(connectedCards);
            connectedCards.clear();


            for (int i = cards.size() - 1; i >= 0; i--) {
                JPanel card = cards.get(i);
                //maxX = Math.max(maxX, card.getLocation().x + card.getSize().width + marginX);
                maxY = Math.max(maxY, card.getLocation().y + card.getSize().height + marginY);
                p.add(card);
            }

            maxX = nextXIfNotStacked;

            //System.out.println("x:" + maxX + ", y:" + maxY);
            if (maxX > 0 && maxY > 0) { //p.getSize().width || maxY > p.getSize().height) {
                //              p.setSize(new Dimension(maxX, maxY));
                p.setPreferredSize(new Dimension(maxX, maxY));
            }

        } else {
            //add all Cards in list to the GUI, add arrows to Local Enchantments
            JPanel addPanel;
            for (int i = 0; i < list.size(); i++) {
                c = list.get(i);
                /*if(c.isLocalEnchantment())
                  addPanel = getCardPanel(c, "<< " +c.getName());
                else
                  addPanel = getCardPanel(c);
                 */
                addPanel = new CardPanel(c);

                p.add(addPanel);
            }
        }
    } //setupPanel()

    /**
     * <p>setupNoLandPermPanel.</p>
     *
     * @param p a {@link javax.swing.JPanel} object.
     * @param list a {@link java.util.ArrayList} object.
     * @param stack a boolean.
     */
    private static void setupNoLandPermPanel(final JPanel p, ArrayList<Card> list, final boolean stack) {

        int maxY = 0;
        int maxX = 0;

        Card c;

        if (stack) {
            // add all Cards in list to the GUI, add arrows to Local Enchantments

            ArrayList<Card> planeswalkers = getPlaneswalkers(list);
            //this will also fetch the equipment and/or enchantment
            ArrayList<Card> equippedEnchantedCreatures = getEquippedEnchantedCreatures(list);
            ArrayList<Card> nonTokenCreatures = getNonTokenCreatures(list);
            ArrayList<Card> tokenCreatures = getTokenCreatures(list);

            //sort tokenCreatures by name (TODO fix the warning message somehow)
            Collections.sort(tokenCreatures, new Comparator<Card>() {
                public int compare(Card c1, Card c2) {
                    return c1.getName().compareTo(c2.getName());
                }
            });

            ArrayList<Card> artifacts = getNonCreatureArtifacts(list);
            ArrayList<Card> enchantments = getGlobalEnchantments(list);
            //ArrayList<Card> nonBasics = getNonBasics(list);


            list = new ArrayList<Card>();
            list.addAll(planeswalkers);
            list.addAll(equippedEnchantedCreatures);
            list.addAll(nonTokenCreatures);
            list.addAll(tokenCreatures);
            list.addAll(artifacts);
            list.addAll(enchantments);


            int atInStack = 0;

            int marginX = 5;
            int marginY = 5;

            int x = marginX;

            int cardOffset = Constant.Runtime.stackOffset[0];

            String color = "";
            ArrayList<JPanel> cards = new ArrayList<JPanel>();

            ArrayList<CardPanel> connectedCards = new ArrayList<CardPanel>();


            boolean nextEquippedEnchanted = false;
            int nextXIfNotStacked = 0;
            Card prevCard = null;
            for (int i = 0; i < list.size(); i++) {
                JPanel addPanel;
                c = list.get(i);
                addPanel = new CardPanel(c);

                boolean startANewStack = false;

                if (!isStackable(c)) {
                    startANewStack = true;
                } else {
                    String newColor = c.getName(); //CardUtil.getColor(c);

                    if (!newColor.equals(color)) {
                        startANewStack = true;
                        color = newColor;
                    }
                }


                if (i == 0) {
                    startANewStack = false;
                }

                if (!startANewStack && atInStack == Constant.Runtime.stackSize[0]) {
                    startANewStack = true;
                }


                if ((c.isEquipment() || c.isAura()) && (c.isEquipping() || c.isEnchanting())
                        && !nextEquippedEnchanted)
                {
                    startANewStack = false;
                } else if ((c.isEquipment() || c.isAura()) && (c.isEquipping() || c.isEnchanting())) {
                    startANewStack = true;
                    nextEquippedEnchanted = false;
                }

                if (c.isCreature() && (c.isEquipped() || c.isEnchanted())) {
                    startANewStack = false;
                    nextEquippedEnchanted = true;
                }
                //very hacky, but this is to ensure equipment stacking occurs correctly when a token
                //is equipped/enchanted, and there are more tokens of that same name
                else if ((prevCard != null && c.isCreature() && prevCard.isCreature()
                        && (prevCard.isEquipped() || prevCard.isEnchanted()) && prevCard.getName().equals(
                                c.getName())))
                {
                    startANewStack = true;
                } else if (prevCard != null && c.isCreature() && prevCard.isCreature()
                        && !prevCard.getName().equals(c.getName()))
                {
                    startANewStack = true;
                }

                if (((c.isAura() && c.isEnchanting()) || (c.isEquipment() && c.isEquipping())) && prevCard != null
                        && prevCard.isPlaneswalker())
                {
                    startANewStack = true;
                }

                if (startANewStack) {
                    setupConnectedCards(connectedCards);
                    connectedCards.clear();

                    // Fixed distance if last was a stack, looks a bit nicer
                    if (atInStack > 1) {
                        x += Math.max(addPanel.getPreferredSize().width, addPanel.getPreferredSize().height)
                        + marginX;
                    } else {
                        x = nextXIfNotStacked;
                    }

                    atInStack = 0;
                } else {
                    if (i != 0) {
                        x += cardOffset;
                    }
                }

                nextXIfNotStacked = x + marginX + addPanel.getPreferredSize().width;

                int xLoc = x;

                int yLoc = marginY;
                yLoc += atInStack * cardOffset;

                addPanel.setLocation(new Point(xLoc, yLoc));
                addPanel.setSize(addPanel.getPreferredSize());


                cards.add(addPanel);

                connectedCards.add((CardPanel) addPanel);

                atInStack++;
                prevCard = c;
            }

            setupConnectedCards(connectedCards);
            connectedCards.clear();


            for (int i = cards.size() - 1; i >= 0; i--) {
                JPanel card = cards.get(i);
                //maxX = Math.max(maxX, card.getLocation().x + card.getSize().width + marginX);
                maxY = Math.max(maxY, card.getLocation().y + card.getSize().height + marginY);
                p.add(card);
            }

            maxX = nextXIfNotStacked;

            if (maxX > 0 && maxY > 0) { //p.getSize().width || maxY > p.getSize().height) {
                p.setPreferredSize(new Dimension(maxX, maxY));
            }

        } else {
            JPanel addPanel;
            for (int i = 0; i < list.size(); i++) {
                c = list.get(i);
                addPanel = new CardPanel(c);

                p.add(addPanel);
            }
        }
    } //setupPanel()

    /**
     * <p>getPlaneswalkers.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getPlaneswalkers(final ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for (Card c : cards) {
            if (c.isPlaneswalker() && !c.isArtifact()) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>getEquippedEnchantedCreatures.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getEquippedEnchantedCreatures(final ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for (Card c : cards) {
            if (c.isCreature() && (c.isEquipped() || c.isEnchanted())) {
                if (c.isEquipped()) {
                    ret.addAll(c.getEquippedBy());
                }
                if (c.isEnchanted()) {
                    ret.addAll(c.getEnchantedBy());
                }

                ret.add(c);
            }

        }
        return ret;
    }


    /**
     * <p>getNonTokenCreatures.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getNonTokenCreatures(final ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for (Card c : cards) {
            if (c.isCreature() && !c.isToken() && !c.isEquipped() && !c.isEnchanted()) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>getTokenCreatures.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getTokenCreatures(final ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for (Card c : cards) {
            if (c.isCreature() && c.isToken() && !c.isEquipped() && !c.isEnchanted()) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>getTokenCreatures.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @param tokenName a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getTokenCreatures(final ArrayList<Card> cards, final String tokenName) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for (Card c : cards) {
            String name = c.getName();
            if (c.isCreature() && c.isToken() && name.equals(tokenName)) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>getMoxen.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @param moxName a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getMoxen(final ArrayList<Card> cards, final String moxName) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for (Card c : cards) {
            String name = c.getName();
            if (name.equals(moxName) && !c.isCreature()) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>getNonCreatureArtifacts.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getNonCreatureArtifacts(final ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for (Card c : cards) {
            String name = c.getName();
            if (c.isArtifact() && !c.isCreature() && !c.isLand() && !c.isGlobalEnchantment()
                    && !(c.isEquipment() && c.isEquipping()) && !name.equals("Mox Emerald")
                    && !name.equals("Mox Jet") && !name.equals("Mox Pearl") && !name.equals("Mox Ruby")
                    && !name.equals("Mox Sapphire"))
            {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>getGlobalEnchantments.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getGlobalEnchantments(final ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for (Card c : cards) {
            if (c.isGlobalEnchantment() && !c.isCreature()) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>getCard.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getCard(final ArrayList<Card> cards, final String name) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for (Card c : cards) {
            if (c.getName().equals(name)) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>getEnchantedLands.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getEnchantedLands(final ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for (Card c : cards) {
            if (c.isLand() && c.isEnchanted()) {
                ret.addAll(c.getEnchantedBy());
                ret.add(c);
            }

        }
        return ret;
    }


    /**
     * <p>getBasics.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @param color a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getBasics(final ArrayList<Card> cards, final String color) {
        ArrayList<Card> ret = new ArrayList<Card>();

        for (Card c : cards) {
            String name = c.getName();

            if (c.isEnchanted()) {
                //do nothing
            }

            else if (name.equals("Swamp") || name.equals("Bog")) {
                if (color == Constant.Color.Black) {
                    ret.add(c);
                }
            } else if (name.equals("Forest") || name.equals("Grass")) {
                if (color == Constant.Color.Green) {
                    ret.add(c);
                }

            } else if (name.equals("Plains") || name.equals("White Sand")) {
                if (color == Constant.Color.White) {
                    ret.add(c);
                }

            } else if (name.equals("Mountain") || name.equals("Rock")) {
                if (color == Constant.Color.Red) {
                    ret.add(c);
                }

            } else if (name.equals("Island") || name.equals("Underwater")) {
                if (color == Constant.Color.Blue) {
                    ret.add(c);
                }
            }
        }

        return ret;
    }

    /**
     * <p>getNonBasics.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getNonBasics(final ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();

        for (Card c : cards) {
            if (!c.isLand() && !c.getName().startsWith("Mox") && !(c instanceof ManaPool)) {
                ret.add(c);
            } else {
                String name = c.getName();
                if (c.isEnchanted() || name.equals("Swamp") || name.equals("Bog") || name.equals("Forest")
                        || name.equals("Grass") || name.equals("Plains") || name.equals("White Sand")
                        || name.equals("Mountain") || name.equals("Rock") || name.equals("Island")
                        || name.equals("Underwater") || name.equals("Badlands") || name.equals("Bayou")
                        || name.equals("Plateau") || name.equals("Scrubland") || name.equals("Savannah")
                        || name.equals("Taiga") || name.equals("Tropical Island") || name.equals("Tundra")
                        || name.equals("Underground Sea") || name.equals("Volcanic Island")
                        || name.startsWith("Mox") || c instanceof ManaPool)
                {
                    // do nothing.
                } else {
                    ret.add(c);
                }
            }
        }

        return ret;
    }

    /**
     * <p>getNonBasicLand.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @param landName a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getNonBasicLand(final ArrayList<Card> cards, final String landName) {
        ArrayList<Card> ret = new ArrayList<Card>();

        for (Card c : cards) {
            if (c.getName().equals(landName)) {
                ret.add(c);
            }
        }

        return ret;
    }

    /**
     * <p>getManaPools.</p>
     *
     * @param cards a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getManaPools(final ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for (Card c : cards) {
            if (c instanceof ManaPool) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>isStackable.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isStackable(final Card c) {
        if (c.isLand() || (c.getName().startsWith("Mox") && !c.getName().equals("Mox Diamond"))
                || (c.isLand() && c.isEnchanted()) || (c.isAura() && c.isEnchanting())
                || (c.isToken() && CardFactoryUtil.multipleControlled(c))
                || (c.isCreature() && (c.isEquipped() || c.isEnchanted())) || (c.isEquipment() && c.isEquipping())
                || (c.isEnchantment()) || (c instanceof ManaPool && c.isSnow()))
        {
            return true;
        }

        return false;
    }

    //~
    /**
     * <p>setupConnectedCards.</p>
     *
     * @param connectedCards a {@link java.util.ArrayList} object.
     */
    public static void setupConnectedCards(final ArrayList<CardPanel> connectedCards) {
        for (int i = connectedCards.size() - 1; i > 0; i--) {
            //System.out.println("We should have a stack");
            CardPanel cp = connectedCards.get(i);
            cp.connectedCard = connectedCards.get(i - 1);
        }
    }
    //~

    /**
     * <p>setupPlayZone.</p>
     *
     * @param p a {@link arcane.ui.PlayArea} object.
     * @param c an array of {@link forge.Card} objects.
     */
    public static void setupPlayZone(final PlayArea p, final Card[] c) {
        List<Card> tmp, diff;
        tmp = new ArrayList<Card>();
        for (arcane.ui.CardPanel cpa : p.cardPanels) {
            tmp.add(cpa.gameCard);
        }
        diff = new ArrayList<Card>(tmp);
        diff.removeAll(Arrays.asList(c));
        if (diff.size() == p.cardPanels.size()) {
            p.clear();
        } else {
            for (Card card : diff) {
                p.removeCardPanel(p.getCardPanel(card.getUniqueNumber()));
            }
        }
        diff = new ArrayList<Card>(Arrays.asList(c));
        diff.removeAll(tmp);

        arcane.ui.CardPanel toPanel = null;
        for (Card card : diff) {
            toPanel = p.addCard(card);
            Animation.moveCard(toPanel);
        }

        for (Card card : c) {
            toPanel = p.getCardPanel(card.getUniqueNumber());
            if (card.isTapped()) {
                toPanel.tapped = true;
                toPanel.tappedAngle = arcane.ui.CardPanel.TAPPED_ANGLE;
            } else {
                toPanel.tapped = false;
                toPanel.tappedAngle = 0;
            }
            toPanel.attachedPanels.clear();
            if (card.isEnchanted()) {
                ArrayList<Card> enchants = card.getEnchantedBy();
                for (Card e : enchants) {
                    arcane.ui.CardPanel cardE = p.getCardPanel(e.getUniqueNumber());
                    if (cardE != null) {
                        toPanel.attachedPanels.add(cardE);
                    }
                }
            }

            if (card.isEquipped()) {
                ArrayList<Card> enchants = card.getEquippedBy();
                for (Card e : enchants) {
                    arcane.ui.CardPanel cardE = p.getCardPanel(e.getUniqueNumber());
                    if (cardE != null) {
                        toPanel.attachedPanels.add(cardE);
                    }
                }
            }

            if (card.isEnchantingCard()) {
                toPanel.attachedToPanel = p.getCardPanel(card.getEnchantingCard().getUniqueNumber());
            } else if (card.isEquipping()) {
                toPanel.attachedToPanel = p.getCardPanel(card.getEquipping().get(0).getUniqueNumber());
            } else {
                toPanel.attachedToPanel = null;
            }

            toPanel.setCard(toPanel.gameCard);
        }
        p.invalidate();
        p.repaint();
    }

    /**
     * <p>updateGUI.</p>
     */
    public static void updateGUI() {
        AllZone.getComputerPlayer().getZone(Zone.Battlefield).updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Hand).updateObservers();
        AllZone.getComputerPlayer().updateObservers();
        AllZone.getHumanPlayer().updateObservers();
    }

    /**
     * <p>devSetupGameState.</p>
     */
    public static void devSetupGameState() {
        String t_humanLife = "-1";
        String t_computerLife = "-1";
        String t_humanSetupCardsInPlay = "NONE";
        String t_computerSetupCardsInPlay = "NONE";
        String t_humanSetupCardsInHand = "NONE";
        String t_computerSetupCardsInHand = "NONE";
        String t_humanSetupGraveyard = "NONE";
        String t_computerSetupGraveyard = "NONE";
        String t_humanSetupLibrary = "NONE";
        String t_computerSetupLibrary = "NONE";
        String t_humanSetupExile = "NONE";
        String t_computerSetupExile = "NONE";
        String t_changePlayer = "NONE";
        String t_changePhase = "NONE";

        String wd = ".";
        JFileChooser fc = new JFileChooser(wd);
        int rc = fc.showDialog(null, "Select Game State File");
        if (rc != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            FileInputStream fstream = new FileInputStream(fc.getSelectedFile().getAbsolutePath());
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String temp = "";

            while ((temp = br.readLine()) != null) {
                String[] tempData = temp.split("=");

                if (tempData.length < 2) {
                    continue;
                }
                if (tempData[0].toCharArray()[0] == '#') {
                    continue;
                }

                String categoryName = tempData[0];
                String categoryValue = tempData[1];

                if (categoryName.toLowerCase().equals("humanlife")) {
                    t_humanLife = categoryValue;
                } else if (categoryName.toLowerCase().equals("ailife")) {
                    t_computerLife = categoryValue;
                } else if (categoryName.toLowerCase().equals("humancardsinplay")) {
                    t_humanSetupCardsInPlay = categoryValue;
                } else if (categoryName.toLowerCase().equals("aicardsinplay")) {
                    t_computerSetupCardsInPlay = categoryValue;
                } else if (categoryName.toLowerCase().equals("humancardsinhand")) {
                    t_humanSetupCardsInHand = categoryValue;
                } else if (categoryName.toLowerCase().equals("aicardsinhand")) {
                    t_computerSetupCardsInHand = categoryValue;
                } else if (categoryName.toLowerCase().equals("humancardsingraveyard")) {
                    t_humanSetupGraveyard = categoryValue;
                } else if (categoryName.toLowerCase().equals("aicardsingraveyard")) {
                    t_computerSetupGraveyard = categoryValue;
                } else if (categoryName.toLowerCase().equals("humancardsinlibrary")) {
                    t_humanSetupLibrary = categoryValue;
                } else if (categoryName.toLowerCase().equals("aicardsinlibrary")) {
                    t_computerSetupLibrary = categoryValue;
                } else if (categoryName.toLowerCase().equals("humancardsinexile")) {
                    t_humanSetupExile = categoryValue;
                } else if (categoryName.toLowerCase().equals("aicardsinexile")) {
                    t_computerSetupExile = categoryValue;
                } else if (categoryName.toLowerCase().equals("activeplayer")) {
                    t_changePlayer = categoryValue;
                } else if (categoryName.toLowerCase().equals("activephase")) {
                    t_changePhase = categoryValue;
                }
            }

            in.close();
        } catch (FileNotFoundException fnfe) {
            JOptionPane.showMessageDialog(null, "File not found: " + fc.getSelectedFile().getAbsolutePath());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading battle setup file!");
            return;
        }

        int setHumanLife = Integer.parseInt(t_humanLife);
        int setComputerLife = Integer.parseInt(t_computerLife);

        String[] humanSetupCardsInPlay = t_humanSetupCardsInPlay.split(";");
        String[] computerSetupCardsInPlay = t_computerSetupCardsInPlay.split(";");
        String[] humanSetupCardsInHand = t_humanSetupCardsInHand.split(";");
        String[] computerSetupCardsInHand = t_computerSetupCardsInHand.split(";");
        String[] humanSetupGraveyard = t_humanSetupGraveyard.split(";");
        String[] computerSetupGraveyard = t_computerSetupGraveyard.split(";");
        String[] humanSetupLibrary = t_humanSetupLibrary.split(";");
        String[] computerSetupLibrary = t_computerSetupLibrary.split(";");
        String[] humanSetupExile = t_humanSetupExile.split(";");
        String[] computerSetupExile = t_computerSetupExile.split(";");

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

        if (!t_changePlayer.trim().toLowerCase().equals("none")) {
            if (t_changePlayer.trim().toLowerCase().equals("human")) {
                AllZone.getPhase().setPlayerTurn(AllZone.getHumanPlayer());
            }
            if (t_changePlayer.trim().toLowerCase().equals("ai")) {
                AllZone.getPhase().setPlayerTurn(AllZone.getComputerPlayer());
            }
        }

        if (!t_changePhase.trim().toLowerCase().equals("none")) {
            AllZone.getPhase().setDevPhaseState(t_changePhase);
        }

        if (!t_humanSetupCardsInPlay.trim().toLowerCase().equals("none")) {
            humanDevSetup = devProcessCardsForZone(humanSetupCardsInPlay, AllZone.getHumanPlayer());
        }

        if (!t_humanSetupCardsInHand.trim().toLowerCase().equals("none")) {
            humanDevHandSetup = devProcessCardsForZone(humanSetupCardsInHand, AllZone.getHumanPlayer());
        }

        if (!t_computerSetupCardsInPlay.trim().toLowerCase().equals("none")) {
            computerDevSetup = devProcessCardsForZone(computerSetupCardsInPlay, AllZone.getComputerPlayer());
        }

        if (!t_computerSetupCardsInHand.trim().toLowerCase().equals("none")) {
            computerDevHandSetup = devProcessCardsForZone(computerSetupCardsInHand, AllZone.getComputerPlayer());
        }

        if (!t_computerSetupGraveyard.trim().toLowerCase().equals("none")) {
            computerDevGraveyardSetup = devProcessCardsForZone(computerSetupGraveyard, AllZone.getComputerPlayer());
        }

        if (!t_humanSetupGraveyard.trim().toLowerCase().equals("none")) {
            humanDevGraveyardSetup = devProcessCardsForZone(humanSetupGraveyard, AllZone.getHumanPlayer());
        }

        if (!t_humanSetupLibrary.trim().toLowerCase().equals("none")) {
            humanDevLibrarySetup = devProcessCardsForZone(humanSetupLibrary, AllZone.getHumanPlayer());
        }

        if (!t_computerSetupLibrary.trim().toLowerCase().equals("none")) {
            computerDevLibrarySetup = devProcessCardsForZone(computerSetupLibrary, AllZone.getComputerPlayer());
        }

        if (!t_humanSetupExile.trim().toLowerCase().equals("none")) {
            humanDevExileSetup = devProcessCardsForZone(humanSetupExile, AllZone.getHumanPlayer());
        }

        if (!t_computerSetupExile.trim().toLowerCase().equals("none")) {
            computerDevExileSetup = devProcessCardsForZone(computerSetupExile, AllZone.getComputerPlayer());
        }

        AllZone.getTriggerHandler().suppressMode("ChangesZone");
        AllZone.getCombat().reset();
        for (Card c : humanDevSetup) {
            AllZone.getHumanPlayer().getZone(Zone.Hand).add(c);
            AllZone.getGameAction().moveToPlay(c);
            c.setSickness(false);
        }

        for (Card c : computerDevSetup) {
            AllZone.getComputerPlayer().getZone(Zone.Hand).add(c);
            AllZone.getGameAction().moveToPlay(c);
            c.setSickness(false);
        }

        if (computerDevGraveyardSetup.size() > 0) {
            AllZone.getComputerPlayer().getZone(Zone.Graveyard).setCards(computerDevGraveyardSetup.toArray());
        }
        if (humanDevGraveyardSetup.size() > 0) {
            AllZone.getHumanPlayer().getZone(Zone.Graveyard).setCards(humanDevGraveyardSetup.toArray());
        }

        if (computerDevHandSetup.size() > 0) {
            AllZone.getComputerPlayer().getZone(Zone.Hand).setCards(computerDevHandSetup.toArray());
        }
        if (humanDevHandSetup.size() > 0) {
            AllZone.getHumanPlayer().getZone(Zone.Hand).setCards(humanDevHandSetup.toArray());
        }

        if (humanDevLibrarySetup.size() > 0) {
            AllZone.getHumanPlayer().getZone(Zone.Library).setCards(humanDevLibrarySetup.toArray());
        }
        if (computerDevLibrarySetup.size() > 0) {
            AllZone.getComputerPlayer().getZone(Zone.Library).setCards(computerDevLibrarySetup.toArray());
        }

        if (humanDevExileSetup.size() > 0) {
            AllZone.getHumanPlayer().getZone(Zone.Exile).setCards(humanDevExileSetup.toArray());
        }
        if (computerDevExileSetup.size() > 0) {
            AllZone.getComputerPlayer().getZone(Zone.Exile).setCards(computerDevExileSetup.toArray());
        }

        AllZone.getTriggerHandler().clearSuppression("ChangesZone");

        if (setComputerLife > 0) {
            AllZone.getComputerPlayer().setLife(setComputerLife, null);
        }
        if (setHumanLife > 0) {
            AllZone.getHumanPlayer().setLife(setHumanLife, null);
        }

        AllZone.getGameAction().checkStateEffects();
        AllZone.getPhase().updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Exile).updateObservers();
        AllZone.getComputerPlayer().getZone(Zone.Exile).updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Hand).updateObservers();
        AllZone.getComputerPlayer().getZone(Zone.Hand).updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Graveyard).updateObservers();
        AllZone.getComputerPlayer().getZone(Zone.Graveyard).updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
        AllZone.getComputerPlayer().getZone(Zone.Battlefield).updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Library).updateObservers();
        AllZone.getComputerPlayer().getZone(Zone.Library).updateObservers();
    }

    /**
     * <p>devProcessCardsForZone.</p>
     *
     * @param data an array of {@link java.lang.String} objects.
     * @param player a {@link forge.Player} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList devProcessCardsForZone(final String[] data, final Player player) {
        CardList cl = new CardList();
        for (int i = 0; i < data.length; i++) {
            String[] cardinfo = data[i].trim().split("\\|");

            Card c = AllZone.getCardFactory().getCard(cardinfo[0], player);

            boolean hasSetCurSet = false;
            for (String info : cardinfo) {
                if (info.startsWith("Set:")) {
                    c.setCurSetCode(info.substring(info.indexOf(':') + 1));
                    hasSetCurSet = true;
                }
                else if (info.equalsIgnoreCase("Tapped:True")) {
                    c.tap();
                }
                else if (info.startsWith("Counters:")) {
                    String[] counterStrings = info.substring(info.indexOf(':') + 1).split(",");
                    for (String counter : counterStrings) {
                        c.addCounter(Counters.valueOf(counter), 1);
                    }
                }
                else if (info.equalsIgnoreCase("SummonSick:True")) {
                    c.setSickness(true);
                }
                else if (info.equalsIgnoreCase("Morphed:True")) {
                    if (!c.getCanMorph()) {
                        System.out.println("Setup game state - Can't morph a card without the morph keyword!");
                        continue;
                    }
                    c.setIsFaceDown(true);
                    c.setManaCost("");
                    c.setColor(new ArrayList<Card_Color>()); //remove all colors
                    c.addColor("0");
                    c.setBaseAttack(2);
                    c.setBaseDefense(2);
                    c.comesIntoPlay();
                    c.setIntrinsicKeyword(new ArrayList<String>()); //remove all keywords
                    c.setType(new ArrayList<String>()); //remove all types
                    c.addType("Creature");
                }
            }

            if (!hasSetCurSet) {
                c.setCurSetCode(c.getMostRecentSet());
            }

            c.setImageFilename(CardUtil.buildFilename(c));
            for (Trigger trig : c.getTriggers()) {
                AllZone.getTriggerHandler().registerTrigger(trig);
            }
            cl.add(c);
        }
        return cl;
    }

    /**
     * <p>devModeTutor.</p>
     *
     * @since 1.0.15
     */
    public static void devModeTutor() {
        CardList lib = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
        Object o = GuiUtils.getChoiceOptional("Choose a card", lib.toArray());
        if (null == o) {
            return;
        } else {
            Card c = (Card) o;
            AllZone.getGameAction().moveToHand(c);
        }
    }

    /**
     * <p>devModeAddCounter.</p>
     *
     * @since 1.0.15
     */
    public static void devModeAddCounter() {
        CardList play = AllZoneUtil.getCardsIn(Zone.Battlefield);
        Object o = GuiUtils.getChoiceOptional("Add counters to which card?", play.toArray());
        if (null == o) {
            return;
        } else {
            Card c = (Card) o;
            Counters counter = GuiUtils.getChoiceOptional("Which type of counter?", Counters.values());
            if (null == counter) {
                return;
            } else {
                Integer[] integers = new Integer[99];
                for(int j = 0; j < 99; j++) {
                    integers[j] = Integer.valueOf(j);
                }
                Integer i = GuiUtils.getChoiceOptional("How many counters?", integers);
                if (null == i) {
                    return;
                } else {
                    c.addCounterFromNonEffect(counter, i);
                }
            }
        }
    }

    /**
     * <p>devModeTapPerm.</p>
     *
     * @since 1.0.15
     */
    public static void devModeTapPerm() {
        CardList play = AllZoneUtil.getCardsIn(Zone.Battlefield);
        Object o = GuiUtils.getChoiceOptional("Choose a permanent", play.toArray());
        if (null == o) {
            return;
        } else {
            Card c = (Card) o;
            c.tap();
        }
    }

    /**
     * <p>devModeUntapPerm.</p>
     *
     * @since 1.0.15
     */
    public static void devModeUntapPerm() {
        CardList play = AllZoneUtil.getCardsIn(Zone.Battlefield);
        Object o = GuiUtils.getChoiceOptional("Choose a permanent", play.toArray());
        if (null == o) {
            return;
        } else {
            Card c = (Card) o;
            c.untap();
        }
    }

    /**
     * <p>devModeUnlimitedLand.</p>
     *
     * @since 1.0.16
     */
    public static void devModeUnlimitedLand() {
        AllZone.getHumanPlayer().addMaxLandsToPlay(100);
    }

    /**
     * <p>devModeSetLife.</p>
     *
     * @since 1.1.3
     */
    public static void devModeSetLife() {
        ArrayList<Player> players = AllZoneUtil.getPlayersInGame();
        Object o = GuiUtils.getChoiceOptional("Set life for which player?", players.toArray());
        if (null == o) {
            return;
        } else {
            Player p = (Player) o;
            Integer[] integers = new Integer[99];
            for (int j = 0; j < 99; j++) {
                integers[j] = Integer.valueOf(j);
            }
            Integer i = GuiUtils.getChoiceOptional("Set life to what?", integers);
            if (null == i) {
                return;
            } else {
                p.setLife(i, null);
            }
        }
    }

} //end class GuiDisplayUtil
