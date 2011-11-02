package forge;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
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

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;

import arcane.ui.PlayArea;
import arcane.ui.util.Animation;
import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.mana.ManaPool;
import forge.card.spellability.AbilityMana;
import forge.gui.GuiUtils;
import forge.gui.game.CardPanel;
import forge.properties.NewConstants;

/**
 * <p>
 * GuiDisplayUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class GuiDisplayUtil implements NewConstants {

    private GuiDisplayUtil() {
        throw new AssertionError();
    }

    /**
     * <p>
     * getCardDetailMouse.
     * </p>
     * 
     * @param visual
     *            a {@link forge.CardContainer} object.
     * @return a {@link java.awt.event.MouseMotionListener} object.
     */
    public static MouseMotionListener getCardDetailMouse(final CardContainer visual) {
        return new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent me) {
                final JPanel panel = (JPanel) me.getSource();
                final Object o = panel.getComponentAt(me.getPoint());

                if ((o != null) && (o instanceof CardPanel)) {
                    final CardContainer cardPanel = (CardContainer) o;
                    visual.setCard(cardPanel.getCard());
                }
            } // mouseMoved
        };
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
            if (CardUtil.isASubType(t) && !subTypes.contains(t)
                    && (!allCreatureTypes || !CardUtil.isACreatureType(t))) {
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
     * setupNoLandPanel.
     * </p>
     * 
     * @param j
     *            a {@link javax.swing.JPanel} object.
     * @param c
     *            an array of {@link forge.Card} objects.
     */
    public static void setupNoLandPanel(final JPanel j, final Card[] c) {
        final ArrayList<Card> a = new ArrayList<Card>();
        /*
         * for(int i = 0; i < c.length; i++) if(c[i].isCreature() ||
         * c[i].isGlobalEnchantment() || c[i].isArtifact() ||
         * c[i].isPlaneswalker()) a.add(c[i]);
         */

        /*
         * 
         * //creatures or planeswalkers for(int i = 0; i < c.length; i++)
         * //!artifact because of Memnarch turning planeswalkers into artifacts.
         * if (c[i].isCreature() || (c[i].isPlaneswalker() &&
         * !c[i].isArtifact())) a.add(c[i]); //(noncreature,
         * non-enchantment,nonland) artifacts for(int i = 0; i < c.length; i++)
         * if (c[i].isArtifact() && !c[i].isCreature() && !c[i].isLand() &&
         * !c[i].isGlobalEnchantment() ) a.add(c[i]); //(noncreature)
         * enchantments for(int i = 0; i < c.length; i++) if
         * (c[i].isGlobalEnchantment() && !c[i].isCreature()) a.add(c[i]);
         */

        for (final Card element : c) {
            a.add(element);
        }

        GuiDisplayUtil.setupNoLandPermPanel(j, a, true);
    }

    /**
     * <p>
     * setupLandPanel.
     * </p>
     * 
     * @param j
     *            a {@link javax.swing.JPanel} object.
     * @param c
     *            an array of {@link forge.Card} objects.
     */
    public static void setupLandPanel(final JPanel j, final Card[] c) {
        final ArrayList<Card> a = new ArrayList<Card>();
        for (int i = 0; i < c.length; i++) {
            if (((!(c[i].isCreature() || c[i].isEnchantment() || c[i].isArtifact() || c[i].isPlaneswalker()) || (c[i]
                    .isLand() && c[i].isArtifact() && !c[i].isCreature() && !c[i].isEnchantment())) && !AllZone
                    .getGameAction().isAttacheeByMindsDesire(c[i]))
                    || (c[i].getName().startsWith("Mox") && !c[i].getName().equals("Mox Diamond"))) {
                a.add(c[i]);
            }
        }
        GuiDisplayUtil.setupPanel(j, a, true);
    }

    /*
     * private static void setupPanel(JPanel p, ArrayList<Card> list) {
     * setupPanel(p, list, false); }
     */

    // list holds Card objects
    // puts local enchanments in the right order
    // adds "<<" to local enchanments names
    /**
     * <p>
     * setupPanel.
     * </p>
     * 
     * @param p
     *            a {@link javax.swing.JPanel} object.
     * @param list
     *            a {@link java.util.ArrayList} object.
     * @param stack
     *            a boolean.
     */
    private static void setupPanel(final JPanel p, ArrayList<Card> list, final boolean stack) {

        int maxY = 0;
        int maxX = 0;
        // remove all local enchantments

        Card c;
        /*
         * for(int i = 0; i < list.size(); i++) { c = (Card)list.get(i);
         * if(c.isLocalEnchantment()) list.remove(i); }
         * 
         * //add local enchantments to the permanents //put local enchantments
         * "next to" the permanent they are enchanting //the inner for loop is
         * backward so permanents with more than one local enchantments are in
         * the right order Card ca[]; for(int i = 0; i < list.size(); i++) { c =
         * (Card)list.get(i); if(c.hasAttachedCards()) { ca =
         * c.getAttachedCards(); for(int inner = ca.length - 1; 0 <= inner;
         * inner--) list.add(i + 1, ca[inner]); } }
         */

        if (stack) {
            // add all Cards in list to the GUI, add arrows to Local
            // Enchantments

            final ArrayList<Card> manaPools = GuiDisplayUtil.getManaPools(list);
            final ArrayList<Card> enchantedLands = GuiDisplayUtil.getEnchantedLands(list);
            final ArrayList<Card> basicBlues = GuiDisplayUtil.getBasics(list, Constant.Color.BLUE);
            final ArrayList<Card> basicReds = GuiDisplayUtil.getBasics(list, Constant.Color.RED);
            final ArrayList<Card> basicBlacks = GuiDisplayUtil.getBasics(list, Constant.Color.BLACK);
            final ArrayList<Card> basicGreens = GuiDisplayUtil.getBasics(list, Constant.Color.GREEN);
            final ArrayList<Card> basicWhites = GuiDisplayUtil.getBasics(list, Constant.Color.WHITE);
            final ArrayList<Card> badlands = GuiDisplayUtil.getNonBasicLand(list, "Badlands");
            final ArrayList<Card> bayou = GuiDisplayUtil.getNonBasicLand(list, "Bayou");
            final ArrayList<Card> plateau = GuiDisplayUtil.getNonBasicLand(list, "Plateau");
            final ArrayList<Card> scrubland = GuiDisplayUtil.getNonBasicLand(list, "Scrubland");
            final ArrayList<Card> savannah = GuiDisplayUtil.getNonBasicLand(list, "Savannah");
            final ArrayList<Card> taiga = GuiDisplayUtil.getNonBasicLand(list, "Taiga");
            final ArrayList<Card> tropicalIsland = GuiDisplayUtil.getNonBasicLand(list, "Tropical Island");
            final ArrayList<Card> tundra = GuiDisplayUtil.getNonBasicLand(list, "Tundra");
            final ArrayList<Card> undergroundSea = GuiDisplayUtil.getNonBasicLand(list, "Underground Sea");
            final ArrayList<Card> volcanicIsland = GuiDisplayUtil.getNonBasicLand(list, "Volcanic Island");

            final ArrayList<Card> nonBasics = GuiDisplayUtil.getNonBasics(list);

            final ArrayList<Card> moxEmerald = GuiDisplayUtil.getMoxen(list, "Mox Emerald");
            final ArrayList<Card> moxJet = GuiDisplayUtil.getMoxen(list, "Mox Jet");
            final ArrayList<Card> moxPearl = GuiDisplayUtil.getMoxen(list, "Mox Pearl");
            final ArrayList<Card> moxRuby = GuiDisplayUtil.getMoxen(list, "Mox Ruby");
            final ArrayList<Card> moxSapphire = GuiDisplayUtil.getMoxen(list, "Mox Sapphire");
            // ArrayList<Card> moxDiamond = getMoxen(list, "Mox Diamond");

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
            // list.addAll(moxDiamond);

            int atInStack = 0;

            final int marginX = 5;
            final int marginY = 5;

            int x = marginX;

            final int cardOffset = Constant.Runtime.STACK_OFFSET[0];

            String color = "";
            final ArrayList<JPanel> cards = new ArrayList<JPanel>();

            final ArrayList<CardPanel> connectedCards = new ArrayList<CardPanel>();

            boolean nextEnchanted = false;
            Card prevCard = null;
            int nextXIfNotStacked = 0;
            for (int i = 0; i < list.size(); i++) {
                JPanel addPanel;
                c = list.get(i);

                addPanel = new CardPanel(c);

                boolean startANewStack = false;

                if (!GuiDisplayUtil.isStackable(c)) {
                    startANewStack = true;
                } else {
                    final String newColor = c.getName(); // CardUtil.getColor(c);

                    if (!newColor.equals(color)) {
                        startANewStack = true;
                        color = newColor;
                    }
                }

                if (i == 0) {
                    startANewStack = false;
                }

                if (!startANewStack && (atInStack == Constant.Runtime.STACK_SIZE[0])) {
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

                // very hacky, but this is to ensure enchantment stacking occurs
                // correctly when
                // a land is enchanted, and there are more lands of that same
                // name

                else if (((prevCard != null) && c.isLand() && prevCard.isLand() && prevCard.isEnchanted() && prevCard
                        .getName().equals(c.getName()))) {
                    startANewStack = true;
                } else if ((prevCard != null) && c.isLand() && prevCard.isLand()
                        && !prevCard.getName().equals(c.getName())) {
                    startANewStack = true;
                }

                /*
                 * if (c.getName().equals("Squirrel Nest")) { startANewStack =
                 * true; System.out.println("startANewStack: " +
                 * startANewStack); }
                 */
                if (c.isAura() && c.isEnchanting() && (prevCard != null) && (prevCard instanceof ManaPool)) {
                    startANewStack = true;
                }
                if ((c instanceof ManaPool) && (prevCard instanceof ManaPool) && prevCard.isSnow()) {
                    startANewStack = false;
                }

                if (startANewStack) {
                    GuiDisplayUtil.setupConnectedCards(connectedCards);
                    connectedCards.clear();

                    // Fixed distance if last was a stack, looks a bit nicer
                    if (atInStack > 1) {
                        x += Math.max(addPanel.getPreferredSize().width, addPanel.getPreferredSize().height) + marginX;
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

                final int xLoc = x;

                int yLoc = marginY;
                yLoc += atInStack * cardOffset;

                addPanel.setLocation(new Point(xLoc, yLoc));
                addPanel.setSize(addPanel.getPreferredSize());

                cards.add(addPanel);

                connectedCards.add((CardPanel) addPanel);

                atInStack++;
                prevCard = c;
            }

            GuiDisplayUtil.setupConnectedCards(connectedCards);
            connectedCards.clear();

            for (int i = cards.size() - 1; i >= 0; i--) {
                final JPanel card = cards.get(i);
                // maxX = Math.max(maxX, card.getLocation().x +
                // card.getSize().width + marginX);
                maxY = Math.max(maxY, card.getLocation().y + card.getSize().height + marginY);
                p.add(card);
            }

            maxX = nextXIfNotStacked;

            // System.out.println("x:" + maxX + ", y:" + maxY);
            if ((maxX > 0) && (maxY > 0)) { // p.getSize().width || maxY >
                // p.getSize().height) {
                // p.setSize(new Dimension(maxX, maxY));
                p.setPreferredSize(new Dimension(maxX, maxY));
            }

        } else {
            // add all Cards in list to the GUI, add arrows to Local
            // Enchantments
            JPanel addPanel;
            for (int i = 0; i < list.size(); i++) {
                c = list.get(i);
                /*
                 * if(c.isLocalEnchantment()) addPanel = getCardPanel(c, "<< "
                 * +c.getName()); else addPanel = getCardPanel(c);
                 */
                addPanel = new CardPanel(c);

                p.add(addPanel);
            }
        }
    } // setupPanel()

    /**
     * <p>
     * setupNoLandPermPanel.
     * </p>
     * 
     * @param p
     *            a {@link javax.swing.JPanel} object.
     * @param list
     *            a {@link java.util.ArrayList} object.
     * @param stack
     *            a boolean.
     */
    private static void setupNoLandPermPanel(final JPanel p, ArrayList<Card> list, final boolean stack) {

        int maxY = 0;
        int maxX = 0;

        Card c;

        if (stack) {
            // add all Cards in list to the GUI, add arrows to Local
            // Enchantments

            final ArrayList<Card> planeswalkers = GuiDisplayUtil.getPlaneswalkers(list);
            // this will also fetch the equipment and/or enchantment
            final ArrayList<Card> equippedEnchantedCreatures = GuiDisplayUtil.getEquippedEnchantedCreatures(list);
            final ArrayList<Card> nonTokenCreatures = GuiDisplayUtil.getNonTokenCreatures(list);
            final ArrayList<Card> tokenCreatures = GuiDisplayUtil.getTokenCreatures(list);

            // sort tokenCreatures by name (TODO fix the warning message
            // somehow)
            Collections.sort(tokenCreatures, new Comparator<Card>() {
                @Override
                public int compare(final Card c1, final Card c2) {
                    return c1.getName().compareTo(c2.getName());
                }
            });

            final ArrayList<Card> artifacts = GuiDisplayUtil.getNonCreatureArtifacts(list);
            final ArrayList<Card> enchantments = GuiDisplayUtil.getGlobalEnchantments(list);
            // ArrayList<Card> nonBasics = getNonBasics(list);

            list = new ArrayList<Card>();
            list.addAll(planeswalkers);
            list.addAll(equippedEnchantedCreatures);
            list.addAll(nonTokenCreatures);
            list.addAll(tokenCreatures);
            list.addAll(artifacts);
            list.addAll(enchantments);

            int atInStack = 0;

            final int marginX = 5;
            final int marginY = 5;

            int x = marginX;

            final int cardOffset = Constant.Runtime.STACK_OFFSET[0];

            String color = "";
            final ArrayList<JPanel> cards = new ArrayList<JPanel>();

            final ArrayList<CardPanel> connectedCards = new ArrayList<CardPanel>();

            boolean nextEquippedEnchanted = false;
            int nextXIfNotStacked = 0;
            Card prevCard = null;
            for (int i = 0; i < list.size(); i++) {
                JPanel addPanel;
                c = list.get(i);
                addPanel = new CardPanel(c);

                boolean startANewStack = false;

                if (!GuiDisplayUtil.isStackable(c)) {
                    startANewStack = true;
                } else {
                    final String newColor = c.getName(); // CardUtil.getColor(c);

                    if (!newColor.equals(color)) {
                        startANewStack = true;
                        color = newColor;
                    }
                }

                if (i == 0) {
                    startANewStack = false;
                }

                if (!startANewStack && (atInStack == Constant.Runtime.STACK_SIZE[0])) {
                    startANewStack = true;
                }

                if ((c.isEquipment() || c.isAura()) && (c.isEquipping()
                        || c.isEnchanting()) && !nextEquippedEnchanted) {
                    startANewStack = false;
                } else if ((c.isEquipment() || c.isAura()) && (c.isEquipping() || c.isEnchanting())) {
                    startANewStack = true;
                    nextEquippedEnchanted = false;
                }

                if (c.isCreature() && (c.isEquipped() || c.isEnchanted())) {
                    startANewStack = false;
                    nextEquippedEnchanted = true;
                }
                // very hacky, but this is to ensure equipment stacking occurs
                // correctly when a token
                // is equipped/enchanted, and there are more tokens of that same
                // name
                else if (((prevCard != null) && c.isCreature() && prevCard.isCreature()
                        && (prevCard.isEquipped() || prevCard.isEnchanted())
                        && prevCard.getName().equals(c.getName()))) {
                    startANewStack = true;
                } else if ((prevCard != null) && c.isCreature() && prevCard.isCreature()
                        && !prevCard.getName().equals(c.getName())) {
                    startANewStack = true;
                }

                if (((c.isAura() && c.isEnchanting()) || (c.isEquipment() && c.isEquipping())) && (prevCard != null)
                        && prevCard.isPlaneswalker()) {
                    startANewStack = true;
                }

                if (startANewStack) {
                    GuiDisplayUtil.setupConnectedCards(connectedCards);
                    connectedCards.clear();

                    // Fixed distance if last was a stack, looks a bit nicer
                    if (atInStack > 1) {
                        x += Math.max(addPanel.getPreferredSize().width, addPanel.getPreferredSize().height) + marginX;
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

                final int xLoc = x;

                int yLoc = marginY;
                yLoc += atInStack * cardOffset;

                addPanel.setLocation(new Point(xLoc, yLoc));
                addPanel.setSize(addPanel.getPreferredSize());

                cards.add(addPanel);

                connectedCards.add((CardPanel) addPanel);

                atInStack++;
                prevCard = c;
            }

            GuiDisplayUtil.setupConnectedCards(connectedCards);
            connectedCards.clear();

            for (int i = cards.size() - 1; i >= 0; i--) {
                final JPanel card = cards.get(i);
                // maxX = Math.max(maxX, card.getLocation().x +
                // card.getSize().width + marginX);
                maxY = Math.max(maxY, card.getLocation().y + card.getSize().height + marginY);
                p.add(card);
            }

            maxX = nextXIfNotStacked;

            if ((maxX > 0) && (maxY > 0)) { // p.getSize().width || maxY >
                // p.getSize().height) {
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
    } // setupPanel()

    /**
     * <p>
     * getPlaneswalkers.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getPlaneswalkers(final ArrayList<Card> cards) {
        final ArrayList<Card> ret = new ArrayList<Card>();
        for (final Card c : cards) {
            if (c.isPlaneswalker() && !c.isArtifact()) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>
     * getEquippedEnchantedCreatures.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getEquippedEnchantedCreatures(final ArrayList<Card> cards) {
        final ArrayList<Card> ret = new ArrayList<Card>();
        for (final Card c : cards) {
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
     * <p>
     * getNonTokenCreatures.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getNonTokenCreatures(final ArrayList<Card> cards) {
        final ArrayList<Card> ret = new ArrayList<Card>();
        for (final Card c : cards) {
            if (c.isCreature() && !c.isToken() && !c.isEquipped() && !c.isEnchanted()) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>
     * getTokenCreatures.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getTokenCreatures(final ArrayList<Card> cards) {
        final ArrayList<Card> ret = new ArrayList<Card>();
        for (final Card c : cards) {
            if (c.isCreature() && c.isToken() && !c.isEquipped() && !c.isEnchanted()) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>
     * getTokenCreatures.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @param tokenName
     *            a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getTokenCreatures(final ArrayList<Card> cards, final String tokenName) {
        final ArrayList<Card> ret = new ArrayList<Card>();
        for (final Card c : cards) {
            final String name = c.getName();
            if (c.isCreature() && c.isToken() && name.equals(tokenName)) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>
     * getMoxen.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @param moxName
     *            a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getMoxen(final ArrayList<Card> cards, final String moxName) {
        final ArrayList<Card> ret = new ArrayList<Card>();
        for (final Card c : cards) {
            final String name = c.getName();
            if (name.equals(moxName) && !c.isCreature()) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>
     * getNonCreatureArtifacts.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getNonCreatureArtifacts(final ArrayList<Card> cards) {
        final ArrayList<Card> ret = new ArrayList<Card>();
        for (final Card c : cards) {
            final String name = c.getName();
            if (c.isArtifact() && !c.isCreature() && !c.isLand() && !c.isGlobalEnchantment()
                    && !(c.isEquipment() && c.isEquipping()) && !name.equals("Mox Emerald") && !name.equals("Mox Jet")
                    && !name.equals("Mox Pearl") && !name.equals("Mox Ruby") && !name.equals("Mox Sapphire")) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>
     * getGlobalEnchantments.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getGlobalEnchantments(final ArrayList<Card> cards) {
        final ArrayList<Card> ret = new ArrayList<Card>();
        for (final Card c : cards) {
            if (c.isGlobalEnchantment() && !c.isCreature()) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @param name
     *            a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getCard(final ArrayList<Card> cards, final String name) {
        final ArrayList<Card> ret = new ArrayList<Card>();
        for (final Card c : cards) {
            if (c.getName().equals(name)) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>
     * getEnchantedLands.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getEnchantedLands(final ArrayList<Card> cards) {
        final ArrayList<Card> ret = new ArrayList<Card>();
        for (final Card c : cards) {
            if (c.isLand() && c.isEnchanted()) {
                ret.addAll(c.getEnchantedBy());
                ret.add(c);
            }

        }
        return ret;
    }

    /**
     * <p>
     * getBasics.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @param color
     *            a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getBasics(final ArrayList<Card> cards, final String color) {
        final ArrayList<Card> ret = new ArrayList<Card>();

        for (final Card c : cards) {
            final String name = c.getName();

            if (c.isEnchanted()) {
                // do nothing
            }

            else if (name.equals("Swamp") || name.equals("Bog")) {
                if (color == Constant.Color.BLACK) {
                    ret.add(c);
                }
            } else if (name.equals("Forest") || name.equals("Grass")) {
                if (color == Constant.Color.GREEN) {
                    ret.add(c);
                }

            } else if (name.equals("Plains") || name.equals("White Sand")) {
                if (color == Constant.Color.WHITE) {
                    ret.add(c);
                }

            } else if (name.equals("Mountain") || name.equals("Rock")) {
                if (color == Constant.Color.RED) {
                    ret.add(c);
                }

            } else if (name.equals("Island") || name.equals("Underwater")) {
                if (color == Constant.Color.BLUE) {
                    ret.add(c);
                }
            }
        }

        return ret;
    }

    /**
     * <p>
     * getNonBasics.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getNonBasics(final ArrayList<Card> cards) {
        final ArrayList<Card> ret = new ArrayList<Card>();

        for (final Card c : cards) {
            if (!c.isLand() && !c.getName().startsWith("Mox") && !(c instanceof ManaPool)) {
                ret.add(c);
            } else {
                final String name = c.getName();
                if (c.isEnchanted() || name.equals("Swamp") || name.equals("Bog") || name.equals("Forest")
                        || name.equals("Grass") || name.equals("Plains") || name.equals("White Sand")
                        || name.equals("Mountain") || name.equals("Rock") || name.equals("Island")
                        || name.equals("Underwater") || name.equals("Badlands") || name.equals("Bayou")
                        || name.equals("Plateau") || name.equals("Scrubland") || name.equals("Savannah")
                        || name.equals("Taiga") || name.equals("Tropical Island") || name.equals("Tundra")
                        || name.equals("Underground Sea") || name.equals("Volcanic Island") || name.startsWith("Mox")
                        || (c instanceof ManaPool)) {
                    // do nothing.
                } else {
                    ret.add(c);
                }
            }
        }

        return ret;
    }

    /**
     * <p>
     * getNonBasicLand.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @param landName
     *            a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getNonBasicLand(final ArrayList<Card> cards, final String landName) {
        final ArrayList<Card> ret = new ArrayList<Card>();

        for (final Card c : cards) {
            if (c.getName().equals(landName)) {
                ret.add(c);
            }
        }

        return ret;
    }

    /**
     * <p>
     * getManaPools.
     * </p>
     * 
     * @param cards
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getManaPools(final ArrayList<Card> cards) {
        final ArrayList<Card> ret = new ArrayList<Card>();
        for (final Card c : cards) {
            if (c instanceof ManaPool) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * <p>
     * isStackable.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isStackable(final Card c) {
        if (c.isLand() || (c.getName().startsWith("Mox") && !c.getName().equals("Mox Diamond"))
                || (c.isLand() && c.isEnchanted()) || (c.isAura() && c.isEnchanting())
                || (c.isToken() && CardFactoryUtil.multipleControlled(c))
                || (c.isCreature() && (c.isEquipped() || c.isEnchanted())) || (c.isEquipment() && c.isEquipping())
                || (c.isEnchantment()) || ((c instanceof ManaPool) && c.isSnow())) {
            return true;
        }

        return false;
    }

    // ~
    /**
     * <p>
     * setupConnectedCards.
     * </p>
     * 
     * @param connectedCards
     *            a {@link java.util.ArrayList} object.
     */
    public static void setupConnectedCards(final ArrayList<CardPanel> connectedCards) {
        for (int i = connectedCards.size() - 1; i > 0; i--) {
            // System.out.println("We should have a stack");
            final CardPanel cp = connectedCards.get(i);
            cp.setConnectedCard(connectedCards.get(i - 1));
        }
    }

    // ~

    /**
     * <p>
     * setupPlayZone.
     * </p>
     * 
     * @param p
     *            a {@link arcane.ui.PlayArea} object.
     * @param c
     *            an array of {@link forge.Card} objects.
     */
    public static void setupPlayZone(final PlayArea p, final Card[] c) {
        List<Card> tmp, diff;
        tmp = new ArrayList<Card>();
        for (final arcane.ui.CardPanel cpa : p.getCardPanels()) {
            tmp.add(cpa.getGameCard());
        }
        diff = new ArrayList<Card>(tmp);
        diff.removeAll(Arrays.asList(c));
        if (diff.size() == p.getCardPanels().size()) {
            p.clear();
        } else {
            for (final Card card : diff) {
                p.removeCardPanel(p.getCardPanel(card.getUniqueNumber()));
            }
        }
        diff = new ArrayList<Card>(Arrays.asList(c));
        diff.removeAll(tmp);

        arcane.ui.CardPanel toPanel = null;
        for (final Card card : diff) {
            toPanel = p.addCard(card);
            Animation.moveCard(toPanel);
        }

        for (final Card card : c) {
            toPanel = p.getCardPanel(card.getUniqueNumber());
            if (card.isTapped()) {
                toPanel.setTapped(true);
                toPanel.setTappedAngle(arcane.ui.CardPanel.TAPPED_ANGLE);
            } else {
                toPanel.setTapped(false);
                toPanel.setTappedAngle(0);
            }
            toPanel.getAttachedPanels().clear();
            if (card.isEnchanted()) {
                final ArrayList<Card> enchants = card.getEnchantedBy();
                for (final Card e : enchants) {
                    final arcane.ui.CardPanel cardE = p.getCardPanel(e.getUniqueNumber());
                    if (cardE != null) {
                        toPanel.getAttachedPanels().add(cardE);
                    }
                }
            }

            if (card.isEquipped()) {
                final ArrayList<Card> enchants = card.getEquippedBy();
                for (final Card e : enchants) {
                    final arcane.ui.CardPanel cardE = p.getCardPanel(e.getUniqueNumber());
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
        AllZone.getComputerPlayer().getZone(Zone.Battlefield).updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();
        AllZone.getHumanPlayer().getZone(Zone.Hand).updateObservers();
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
                AllZone.getPhase().setPlayerTurn(AllZone.getHumanPlayer());
            }
            if (tChangePlayer.trim().toLowerCase().equals("ai")) {
                AllZone.getPhase().setPlayerTurn(AllZone.getComputerPlayer());
            }
        }

        if (!tChangePhase.trim().toLowerCase().equals("none")) {
            AllZone.getPhase().setDevPhaseState(tChangePhase);
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

        AllZone.getTriggerHandler().suppressMode("ChangesZone");
        AllZone.getCombat().reset();
        for (final Card c : humanDevSetup) {
            AllZone.getHumanPlayer().getZone(Zone.Hand).add(c);
            AllZone.getGameAction().moveToPlay(c);
            c.setSickness(false);
        }

        for (final Card c : computerDevSetup) {
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
     * <p>
     * devProcessCardsForZone.
     * </p>
     * 
     * @param data
     *            an array of {@link java.lang.String} objects.
     * @param player
     *            a {@link forge.Player} object.
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
                } else if (info.equalsIgnoreCase("Morphed:True")) {
                    if (!c.getCanMorph()) {
                        System.out.println("Setup game state - Can't morph a card without the morph keyword!");
                        continue;
                    }
                    c.setIsFaceDown(true);
                    c.setManaCost("");
                    c.setColor(new ArrayList<CardColor>()); // remove all
                                                             // colors
                    c.addColor("0");
                    c.setBaseAttack(2);
                    c.setBaseDefense(2);
                    c.comesIntoPlay();
                    c.setIntrinsicKeyword(new ArrayList<String>()); // remove
                                                                    // all
                                                                    // keywords
                    c.setType(new ArrayList<String>()); // remove all types
                    c.addType("Creature");
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
        final CardList lib = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
        final Object o = GuiUtils.getChoiceOptional("Choose a card", lib.toArray());
        if (null == o) {
            return;
        } else {
            final Card c = (Card) o;
            AllZone.getGameAction().moveToHand(c);
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
        final CardList play = AllZoneUtil.getCardsIn(Zone.Battlefield);
        final Object o = GuiUtils.getChoiceOptional("Add counters to which card?", play.toArray());
        if (null == o) {
            return;
        } else {
            final Card c = (Card) o;
            final Counters counter = GuiUtils.getChoiceOptional("Which type of counter?", Counters.values());
            if (null == counter) {
                return;
            } else {
                final Integer[] integers = new Integer[99];
                for (int j = 0; j < 99; j++) {
                    integers[j] = Integer.valueOf(j);
                }
                final Integer i = GuiUtils.getChoiceOptional("How many counters?", integers);
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
        final CardList play = AllZoneUtil.getCardsIn(Zone.Battlefield);
        final Object o = GuiUtils.getChoiceOptional("Choose a permanent", play.toArray());
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
        final CardList play = AllZoneUtil.getCardsIn(Zone.Battlefield);
        final Object o = GuiUtils.getChoiceOptional("Choose a permanent", play.toArray());
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
        final Object o = GuiUtils.getChoiceOptional("Set life for which player?", players.toArray());
        if (null == o) {
            return;
        } else {
            final Player p = (Player) o;
            final Integer[] integers = new Integer[99];
            for (int j = 0; j < 99; j++) {
                integers[j] = Integer.valueOf(j);
            }
            final Integer i = GuiUtils.getChoiceOptional("Set life to what?", integers);
            if (null == i) {
                return;
            } else {
                p.setLife(i, null);
            }
        }
    }

} // end class GuiDisplayUtil
