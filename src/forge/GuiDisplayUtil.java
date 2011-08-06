
package forge;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;

import arcane.ui.PlayArea;
import arcane.ui.util.Animation;

import forge.gui.game.CardPanel;
import forge.properties.NewConstants;


public class GuiDisplayUtil implements NewConstants {
    public static MouseMotionListener getCardDetailMouse(final CardContainer visual) {
        return new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent me) {
                JPanel panel = (JPanel) me.getSource();
                Object o = panel.getComponentAt(me.getPoint());
                
                if((o != null) && (o instanceof CardPanel)) {
                    CardContainer cardPanel = (CardContainer) o;
                    visual.setCard(cardPanel.getCard());
                }
            }//mouseMoved
        };
    }
    
    /**
     * Returns the listener that updates the card preview panel
     */
    public static MouseMotionListener getCardDetailMouse(final GuiDisplay2 visual) {
        return new MouseInputAdapter() {
            @Override
            public void mouseMoved(MouseEvent me) {
                JPanel panel = (JPanel) me.getSource();
                Object o = panel.getComponentAt(me.getPoint());
                
                if((o != null) && (o instanceof CardPanel)) {
                    CardContainer cardPanel = (CardContainer) o;
                    visual.setCard(cardPanel.getCard());
                }
            }//mouseMoved
            
            /**
             * Could be added to the card panels for the same effect
             */
            @Override
            public void mouseEntered(MouseEvent e) {
                if(!(e.getSource() instanceof CardPanel)) return;
                CardContainer panel = (CardContainer) e.getSource();
                visual.setCard(panel.getCard());
            }
        };
    }
    
    public static Border getBorder(Card card) {
    	// color info
        if (card == null) 
        	return BorderFactory.createEmptyBorder(2, 2, 2, 2);
        java.awt.Color color;
        ArrayList<String> list = CardUtil.getColors(card);
        
        if (card.isFaceDown())
        	color = Color.gray;
        
        else if (list.size() > 1) color = Color.orange;
        
        else if (list.get(0).equals(Constant.Color.Black)) 
        	color = Color.black;

        else if (list.get(0).equals(Constant.Color.Green)) 
        	color = new Color(0, 220, 39);

        else if (list.get(0).equals(Constant.Color.White)) 
        	color = Color.white;

        else if (list.get(0).equals(Constant.Color.Red)) 
        	color = Color.red;

        else if (list.get(0).equals(Constant.Color.Blue)) 
        	color = Color.blue;
        
        else if (list.get(0).equals(Constant.Color.Colorless)) 
        	color = Color.gray;

        else color = new Color(200, 0, 230); // If your card has a violet border, something is wrong
        
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
        } else return BorderFactory.createLineBorder(Color.gray, 2);
    }
    
    public static String formatCardType(Card card) {
        ArrayList<String> list = card.getType();
        StringBuilder sb = new StringBuilder();
        String s = "";
        
        for(int i = 0; i < list.size(); i++) {
            s = list.get(i).toString();
            sb.append(s);
            if(s.equals("Creature") || s.equals("Land")) {
                sb.append(" - ");
            } else sb.append(" ");
        }
        return sb.toString();
    }
    
    public static String cleanString(String in) {
        StringBuffer out = new StringBuffer();
        char c;
        for(int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if(c == ' ' || c == '-') out.append('_');
            else if(Character.isLetterOrDigit(c) || c == '_') {
                out.append(c);
            }
        }
        return out.toString().toLowerCase();
    }
    
    public static String cleanStringMWS(String in)
    {
    	StringBuffer out = new StringBuffer();
    	char c;
    	for (int i=0; i<in.length(); i++)
    	{
    		c = in.charAt(i);
    		if (c == '"' || c == '/')
    			out.append("");
    		else
    			out.append(c);
    	}
    	return out.toString();
    }
    
    public static void setupNoLandPanel(JPanel j, Card c[]) {
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
        //(non-creature, non-enchantment,non-land) artifacts
        for(int i = 0; i < c.length; i++)
        	if (c[i].isArtifact() && !c[i].isCreature() && !c[i].isLand() && !c[i].isGlobalEnchantment() )
        		a.add(c[i]);
        //(non-creature) enchantments
        for(int i = 0; i < c.length; i++)
        	if (c[i].isGlobalEnchantment() && !c[i].isCreature())
        		a.add(c[i]);
        
        */

        for(int i = 0; i < c.length; i++) {
            a.add(c[i]);
        }
        
        setupNoLandPermPanel(j, a, true);
    }
    
    public static void setupLandPanel(JPanel j, Card c[]) {
        ArrayList<Card> a = new ArrayList<Card>();
        for(int i = 0; i < c.length; i++)
            if((!(c[i].isCreature() || c[i].isEnchantment() || c[i].isArtifact() || c[i].isPlaneswalker()) || (c[i].isLand()
                    && c[i].isArtifact() && !c[i].isCreature() && !c[i].isEnchantment()))
                    && !AllZone.GameAction.isAttachee(c[i])
                    || (c[i].getName().startsWith("Mox") && !c[i].getName().equals("Mox Diamond"))) a.add(c[i]);
        
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
    private static void setupPanel(JPanel p, ArrayList<Card> list, boolean stack) {
        
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

        if(stack) {
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
            for(int i = 0; i < list.size(); i++) {
                JPanel addPanel;
                c = list.get(i);
                
                addPanel = new CardPanel(c);
                

                boolean startANewStack = false;
                
                if(!isStackable(c)) {
                    startANewStack = true;
                } else {
                    String newColor = c.getName(); //CardUtil.getColor(c);
                    
                    if(!newColor.equals(color)) {
                        startANewStack = true;
                        color = newColor;
                    }
                }
                
                if(i == 0) {
                    startANewStack = false;
                }
                

                if(!startANewStack && atInStack == Constant.Runtime.stackSize[0]) {
                    startANewStack = true;
                }
                

                if(c.isAura() && c.isEnchanting() && !nextEnchanted) startANewStack = false;
                else if(c.isAura() && c.isEnchanting()) {
                    startANewStack = true;
                    nextEnchanted = false;
                }
                
                if(c.isLand() && c.isEnchanted()) {
                    startANewStack = false;
                    nextEnchanted = true;
                }


                //very hacky, but this is to ensure enchantment stacking occurs correctly when a land is enchanted, and there are more lands of that same name
                
                else if((prevCard != null && c.isLand() && prevCard.isLand() && prevCard.isEnchanted() && prevCard.getName().equals(
                        c.getName()))) startANewStack = true;
                else if(prevCard != null && c.isLand() && prevCard.isLand()
                        && !prevCard.getName().equals(c.getName())) startANewStack = true;
                
                /*
                if (c.getName().equals("Squirrel Nest")) {
                	startANewStack = true;
                	System.out.println("startANewStack: " + startANewStack);
                }
                */
                if(c.isAura() && c.isEnchanting() && prevCard != null && prevCard instanceof ManaPool) startANewStack = true;
                if(c instanceof ManaPool && prevCard instanceof ManaPool && prevCard.isSnow()) startANewStack = false;
                
                if(startANewStack) {
                    setupConnectedCards(connectedCards);
                    connectedCards.clear();
                    
                    // Fixed distance if last was a stack, looks a bit nicer
                    if(atInStack > 1) {
                        x += Math.max(addPanel.getPreferredSize().width, addPanel.getPreferredSize().height)
                                + marginX;
                    } else {
                        x = nextXIfNotStacked;
                    }
                    
                    atInStack = 0;
                } else {
                    if(i != 0) {
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
            

            for(int i = cards.size() - 1; i >= 0; i--) {
                JPanel card = cards.get(i);
                //maxX = Math.max(maxX, card.getLocation().x + card.getSize().width + marginX);
                maxY = Math.max(maxY, card.getLocation().y + card.getSize().height + marginY);
                p.add(card);
            }
            
            maxX = nextXIfNotStacked;
            
            //System.out.println("x:" + maxX + ", y:" + maxY);
            if(maxX > 0 && maxY > 0) { //p.getSize().width || maxY > p.getSize().height) {
//        p.setSize(new Dimension(maxX, maxY));
                p.setPreferredSize(new Dimension(maxX, maxY));
            }
            
        } else {
            //add all Cards in list to the GUI, add arrows to Local Enchantments
            JPanel addPanel;
            for(int i = 0; i < list.size(); i++) {
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
    }//setupPanel()
    
    private static void setupNoLandPermPanel(JPanel p, ArrayList<Card> list, boolean stack) {
        
        int maxY = 0;
        int maxX = 0;
        
        Card c;
        
        if(stack) {
            // add all Cards in list to the GUI, add arrows to Local Enchantments 
            
            ArrayList<Card> planeswalkers = getPlaneswalkers(list);
            ArrayList<Card> equippedEnchantedCreatures = getEquippedEnchantedCreatures(list); //this will also fetch the equipment and/or enchantment
            ArrayList<Card> nonTokenCreatures = getNonTokenCreatures(list);
            ArrayList<Card> tokenCreatures = getTokenCreatures(list);
            
            //sort tokenCreatures by name (TODO: fix the warning message somehow)
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
            for(int i = 0; i < list.size(); i++) {
                JPanel addPanel;
                c = list.get(i);
                addPanel = new CardPanel(c);
                
                boolean startANewStack = false;
                
                if(!isStackable(c)) {
                    startANewStack = true;
                } else {
                    String newColor = c.getName(); //CardUtil.getColor(c);
                    
                    if(!newColor.equals(color)) {
                        startANewStack = true;
                        color = newColor;
                    }
                }
                

                if(i == 0) {
                    startANewStack = false;
                }
                
                if(!startANewStack && atInStack == Constant.Runtime.stackSize[0]) {
                    startANewStack = true;
                }
                

                if((c.isEquipment() || c.isAura()) && (c.isEquipping() || c.isEnchanting())
                        && !nextEquippedEnchanted) startANewStack = false;
                else if((c.isEquipment() || c.isAura()) && (c.isEquipping() || c.isEnchanting())) {
                    startANewStack = true;
                    nextEquippedEnchanted = false;
                }
                
                if(c.isCreature() && (c.isEquipped() || c.isEnchanted())) {
                    startANewStack = false;
                    nextEquippedEnchanted = true;
                }
                //very hacky, but this is to ensure equipment stacking occurs correctly when a token is equipped/enchanted, and there are more tokens of that same name
                else if((prevCard != null && c.isCreature() && prevCard.isCreature()
                        && (prevCard.isEquipped() || prevCard.isEnchanted()) && prevCard.getName().equals(
                        c.getName()))) startANewStack = true;
                else if(prevCard != null && c.isCreature() && prevCard.isCreature()
                        && !prevCard.getName().equals(c.getName())) startANewStack = true;
                
                if(((c.isAura() && c.isEnchanting()) || (c.isEquipment() && c.isEquipping())) && prevCard != null
                        && prevCard.isPlaneswalker()) startANewStack = true;
                
                if(startANewStack) {
                    setupConnectedCards(connectedCards);
                    connectedCards.clear();
                    
                    // Fixed distance if last was a stack, looks a bit nicer
                    if(atInStack > 1) {
                        x += Math.max(addPanel.getPreferredSize().width, addPanel.getPreferredSize().height)
                                + marginX;
                    } else {
                        x = nextXIfNotStacked;
                    }
                    
                    atInStack = 0;
                } else {
                    if(i != 0) {
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
            

            for(int i = cards.size() - 1; i >= 0; i--) {
                JPanel card = cards.get(i);
                //maxX = Math.max(maxX, card.getLocation().x + card.getSize().width + marginX);
                maxY = Math.max(maxY, card.getLocation().y + card.getSize().height + marginY);
                p.add(card);
            }
            
            maxX = nextXIfNotStacked;
            
            if(maxX > 0 && maxY > 0) { //p.getSize().width || maxY > p.getSize().height) {
                p.setPreferredSize(new Dimension(maxX, maxY));
            }
            
        } else {
            JPanel addPanel;
            for(int i = 0; i < list.size(); i++) {
                c = list.get(i);
                addPanel = new CardPanel(c);
                
                p.add(addPanel);
            }
        }
    }//setupPanel()
    
    public static ArrayList<Card> getPlaneswalkers(ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for(Card c:cards) {
            if(c.isPlaneswalker() && !c.isArtifact()) ret.add(c);
        }
        return ret;
    }
    
    public static ArrayList<Card> getEquippedEnchantedCreatures(ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for(Card c:cards) {
            if(c.isCreature() && (c.isEquipped() || c.isEnchanted())) {
                if(c.isEquipped()) ret.addAll(c.getEquippedBy());
                if(c.isEnchanted()) ret.addAll(c.getEnchantedBy());
                
                ret.add(c);
            }
            
        }
        return ret;
    }
    
    
    public static ArrayList<Card> getNonTokenCreatures(ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for(Card c:cards) {
            if(c.isCreature() && !c.isToken() && !c.isEquipped() && !c.isEnchanted()) ret.add(c);
        }
        return ret;
    }
    
    public static ArrayList<Card> getTokenCreatures(ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for(Card c:cards) {
            if(c.isCreature() && c.isToken() && !c.isEquipped() && !c.isEnchanted()) ret.add(c);
        }
        return ret;
    }
    
    public static ArrayList<Card> getTokenCreatures(ArrayList<Card> cards, String tokenName) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for(Card c:cards) {
            String name = c.getName();
            if(c.isCreature() && c.isToken() && name.equals(tokenName)) ret.add(c);
        }
        return ret;
    }
    
    public static ArrayList<Card> getMoxen(ArrayList<Card> cards, String moxName) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for(Card c:cards) {
            String name = c.getName();
            if(name.equals(moxName) && !c.isCreature()) ret.add(c);
        }
        return ret;
    }
    
    public static ArrayList<Card> getNonCreatureArtifacts(ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for(Card c:cards) {
            String name = c.getName();
            if(c.isArtifact() && !c.isCreature() && !c.isLand() && !c.isGlobalEnchantment()
                    && !(c.isEquipment() && c.isEquipping()) && !name.equals("Mox Emerald")
                    && !name.equals("Mox Jet") && !name.equals("Mox Pearl") && !name.equals("Mox Ruby")
                    && !name.equals("Mox Sapphire")) ret.add(c);
        }
        return ret;
    }
    
    public static ArrayList<Card> getGlobalEnchantments(ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for(Card c:cards) {
            if(c.isGlobalEnchantment() && !c.isCreature()) ret.add(c);
        }
        return ret;
    }
    
    public static ArrayList<Card> getCard(ArrayList<Card> cards, String name) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for(Card c:cards) {
            if(c.getName().equals(name)) ret.add(c);
        }
        return ret;
    }
    
    public static ArrayList<Card> getEnchantedLands(ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for(Card c:cards) {
            if(c.isLand() && c.isEnchanted()) {
                ret.addAll(c.getEnchantedBy());
                ret.add(c);
            }
            
        }
        return ret;
    }
    
    
    public static ArrayList<Card> getBasics(ArrayList<Card> cards, String color) {
        ArrayList<Card> ret = new ArrayList<Card>();
        
        for(Card c:cards) {
            String name = c.getName();
            
            if(c.isEnchanted()) ;//do nothing
            
            else if(name.equals("Swamp") || name.equals("Bog")) {
                if(color == Constant.Color.Black) {
                    ret.add(c);
                }
            } else if(name.equals("Forest") || name.equals("Grass")) {
                if(color == Constant.Color.Green) {
                    ret.add(c);
                }
                
            } else if(name.equals("Plains") || name.equals("White Sand")) {
                if(color == Constant.Color.White) {
                    ret.add(c);
                }
                
            } else if(name.equals("Mountain") || name.equals("Rock")) {
                if(color == Constant.Color.Red) {
                    ret.add(c);
                }
                
            } else if(name.equals("Island") || name.equals("Underwater")) {
                if(color == Constant.Color.Blue) {
                    ret.add(c);
                }
            }
        }
        
        return ret;
    }
    
    public static ArrayList<Card> getNonBasics(ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        
        for(Card c:cards) {
            if(!c.isLand() && !c.getName().startsWith("Mox") && !(c instanceof ManaPool)) {
                ret.add(c);
            } else {
                String name = c.getName();
                if(c.isEnchanted() || name.equals("Swamp") || name.equals("Bog") || name.equals("Forest")
                        || name.equals("Grass") || name.equals("Plains") || name.equals("White Sand")
                        || name.equals("Mountain") || name.equals("Rock") || name.equals("Island")
                        || name.equals("Underwater") || name.equals("Badlands") || name.equals("Bayou")
                        || name.equals("Plateau") || name.equals("Scrubland") || name.equals("Savannah")
                        || name.equals("Taiga") || name.equals("Tropical Island") || name.equals("Tundra")
                        || name.equals("Underground Sea") || name.equals("Volcanic Island")
                        || name.startsWith("Mox") || c instanceof ManaPool) {
                    // do nothing.
                } else {
                    ret.add(c);
                }
            }
        }
        
        return ret;
    }
    
    public static ArrayList<Card> getNonBasicLand(ArrayList<Card> cards, String landName) {
        ArrayList<Card> ret = new ArrayList<Card>();
        
        for(Card c:cards)
            if(c.getName().equals(landName)) ret.add(c);
        
        return ret;
    }
    
    public static ArrayList<Card> getManaPools(ArrayList<Card> cards) {
        ArrayList<Card> ret = new ArrayList<Card>();
        for(Card c:cards) {
            if(c instanceof ManaPool) {
                ret.add(c);
            }
        }
        return ret;
    }
    
    public static boolean isStackable(Card c) {
        
        /*String name = c.getName();
        if( name.equals("Swamp") || name.equals("Bog") || 
            name.equals("Forest") || name.equals("Grass") || 
            name.equals("Plains") || name.equals("White Sand") || 
            name.equals("Mountain") || name.equals("Rock") ||
            name.equals("Island") || name.equals("Underwater")) {
          return true;
        }
        */
        if(c.isLand() || (c.getName().startsWith("Mox") && !c.getName().equals("Mox Diamond"))
                || (c.isLand() && c.isEnchanted()) || (c.isAura() && c.isEnchanting())
                || (c.isToken() && CardFactoryUtil.multipleControlled(c))
                || (c.isCreature() && (c.isEquipped() || c.isEnchanted())) || (c.isEquipment() && c.isEquipping())
                || (c.isEnchantment()) || (c instanceof ManaPool && c.isSnow())) return true;
        
        return false;
    }
    
    //~
    public static void setupConnectedCards(ArrayList<CardPanel> connectedCards) {
        for(int i = connectedCards.size() - 1; i > 0; i--) {
            //System.out.println("We should have a stack");
            CardPanel cp = connectedCards.get(i);
            cp.connectedCard = connectedCards.get(i - 1);
        }
    }
    //~
    
    public static void setupPlayZone(PlayArea p, Card c[]) {
    	List<Card> tmp, diff;
        tmp = new ArrayList<Card>();
        for(arcane.ui.CardPanel cpa : p.cardPanels)
                tmp.add(cpa.gameCard);
        diff = new ArrayList<Card>(tmp);
        diff.removeAll(Arrays.asList(c));
        if(diff.size() == p.cardPanels.size())
                p.clear();
        else {
                for(Card card : diff) {
                        p.removeCardPanel(p.getCardPanel(card.getUniqueNumber()));
                }
        }
        diff = new ArrayList<Card>(Arrays.asList(c));
        diff.removeAll(tmp);
        
        arcane.ui.CardPanel toPanel = null;
        for(Card card : diff) {
        	toPanel = p.addCard(card);
        	Animation.moveCard(toPanel);
        }
        
        for(Card card : c){
        	toPanel = p.getCardPanel(card.getUniqueNumber());
        	if(card.isTapped()){
        		toPanel.tapped = true;
        		toPanel.tappedAngle = arcane.ui.CardPanel.TAPPED_ANGLE;
        	}
        	else {
        		toPanel.tapped = false;
        		toPanel.tappedAngle = 0;
        	}
        	toPanel.attachedPanels.clear();
        	if(card.isEnchanted()){
        		ArrayList<Card> enchants = card.getEnchantedBy();
        		for(Card e : enchants){
        			arcane.ui.CardPanel cardE = p.getCardPanel(e.getUniqueNumber());
        			if(cardE != null)
        				toPanel.attachedPanels.add(cardE);
        		}
        	}
        	
        	if(card.isEquipped()){
        		ArrayList<Card> enchants = card.getEquippedBy();
        		for(Card e : enchants){
        			arcane.ui.CardPanel cardE = p.getCardPanel(e.getUniqueNumber());
        			if(cardE != null)
        				toPanel.attachedPanels.add(cardE);
        		}
        	}
        	
        	if(card.isEnchanting()){
        		toPanel.attachedToPanel = p.getCardPanel(card.getEnchanting().get(0).getUniqueNumber());
        	} else if(card.isEquipping()){
        		toPanel.attachedToPanel = p.getCardPanel(card.getEquipping().get(0).getUniqueNumber());
        	} 
        	else toPanel.attachedToPanel = null;
        	
        	toPanel.setCard(toPanel.gameCard);
        }
        p.invalidate();
        p.repaint();
    }
    
    static void updateGUI() {
        AllZone.Computer_Battlefield.updateObservers();
        AllZone.Human_Battlefield.updateObservers();
        AllZone.Human_Hand.updateObservers();
    }
}
