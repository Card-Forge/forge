package forge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import forge.error.ErrorViewer;


public class CardUtil {
    public final static Random r = new Random();
    
    
    public static int getRandomIndex(Object[] o) {
        if(o == null || o.length == 0) throw new RuntimeException(
                "CardUtil : getRandomIndex() argument is null or length is 0");
        
        return r.nextInt(o.length);
    }
    
    public static Card getRandom(Card[] o) {
        return o[getRandomIndex(o)];
    }
    
    public static int getRandomIndex(SpellAbilityList list) {
        if(list == null || list.size() == 0) throw new RuntimeException(
                "CardUtil : getRandomIndex(SpellAbilityList) argument is null or length is 0");
        
        return r.nextInt(list.size());
    }
    
    public static int getRandomIndex(CardList c) {
        return r.nextInt(c.size());
    }
    
    //returns Card Name (unique number) attack/defense
    //example: Big Elf (12) 2/3
    public static String toText(Card c) {
        return c.getName() + " (" + c.getUniqueNumber() + ") " + c.getNetAttack() + "/" + c.getNetDefense();
    }
    
    public static Card[] toCard(Collection<Card> col) {
        Object o[] = col.toArray();
        Card c[] = new Card[o.length];
        
        for(int i = 0; i < c.length; i++) {
            Object swap = o[i];
            if(swap instanceof Card) c[i] = (Card) o[i];
            else throw new RuntimeException("CardUtil : toCard() invalid class, should be Card - "
                    + o[i].getClass() + " - toString() - " + o[i].toString());
        }
        
        return c;
    }
    
    public static Card[] toCard(ArrayList<Card> list) {
        Card[] c = new Card[list.size()];
        list.toArray(c);
        return c;
    }
    
    public static ArrayList<Card> toList(Card c[]) {
        ArrayList<Card> a = new ArrayList<Card>();
        for(int i = 0; i < c.length; i++)
            a.add(c[i]);
        return a;
    }
    
    //returns "G", longColor is Constant.Color.Green and the like
    public static String getShortColor(String longColor) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(Constant.Color.Black, "B");
        map.put(Constant.Color.Blue, "U");
        map.put(Constant.Color.Green, "G");
        map.put(Constant.Color.Red, "R");
        map.put(Constant.Color.White, "W");
        
        Object o = map.get(longColor);
        if(o == null) throw new RuntimeException("CardUtil : getShortColor() invalid argument - " + longColor);
        
        return (String) o;
    }
    
    
    //returns something like Constant.Color.Green or something
    public static String getColor(Card c) {
        String manaCost = c.getManaCost();
        
        if(-1 != manaCost.indexOf("G")) return Constant.Color.Green;
        else if(-1 != manaCost.indexOf("W")) return Constant.Color.White;
        else if(-1 != manaCost.indexOf("B")) return Constant.Color.Black;
        else if(-1 != manaCost.indexOf("U")) return Constant.Color.Blue;
        else if(-1 != manaCost.indexOf("R")) return Constant.Color.Red;
        else return Constant.Color.Colorless;
    }
    
    public static ArrayList<String> getColors(Card c) {
        String m = c.getManaCost();
        Set<String> colors = new HashSet<String>();
        
        for(int i = 0; i < m.length(); i++) {
            switch(m.charAt(i)) {
                case ' ':
                break;
                case 'G':
                    colors.add(Constant.Color.Green);
                break;
                case 'W':
                    colors.add(Constant.Color.White);
                break;
                case 'B':
                    colors.add(Constant.Color.Black);
                break;
                case 'U':
                    colors.add(Constant.Color.Blue);
                break;
                case 'R':
                    colors.add(Constant.Color.Red);
                break;
            }
        }
        for(String kw : c.getKeyword())
        	if(kw.startsWith(c.getName()+" is ") || kw.startsWith("CARDNAME is "))
        		for(String color : Constant.Color.Colors)
        			if(kw.endsWith(color+"."))
        				colors.add(color);
        if(colors.contains(Constant.Color.Colorless))
        	colors.clear();
        // Painter's
		CardList list = AllZoneUtil.getCardsInPlay("Painter's Servant");
		if(list.size() > 0){
			for(int i = 0; i < list.size(); i++) colors.add(list.get(i).getChosenColor());	
		}
        //Painter's
        if(colors.isEmpty()) colors.add(Constant.Color.Colorless);
        
        return new ArrayList<String>(colors);
    }
    
    public static ArrayList<String> getOnlyColors(Card c) {
        String m = c.getManaCost();
        Set<String> colors = new HashSet<String>();
        
        for(int i = 0; i < m.length(); i++) {
            switch(m.charAt(i)) {
                case ' ':
                break;
                case 'G':
                    colors.add(Constant.Color.Green);
                break;
                case 'W':
                    colors.add(Constant.Color.White);
                break;
                case 'B':
                    colors.add(Constant.Color.Black);
                break;
                case 'U':
                    colors.add(Constant.Color.Blue);
                break;
                case 'R':
                    colors.add(Constant.Color.Red);
                break;
            }
        }
        for(String kw : c.getKeyword())
        	if(kw.startsWith(c.getName()+" is ") || kw.startsWith("CARDNAME is "))
        		for(String color : Constant.Color.Colors)
        			if(kw.endsWith(color+"."))
        				colors.add(color); 
        return new ArrayList<String>(colors);
    }
    
    
    public static boolean hasCardName(String cardName, ArrayList<Card> list) {
        Card c;
        boolean b = false;
        
        for(int i = 0; i < list.size(); i++) {
            c = list.get(i);
            if(c.getName().equals(cardName)) {
                b = true;
                break;
            }
        }
        return b;
    }//hasCardName()
    
    //probably should put this somewhere else, but not sure where
    static public int getConvertedManaCost(SpellAbility sa) {
        return getConvertedManaCost(sa.getManaCost());
    }
    
    static public int getConvertedManaCost(Card c)
    {
    	if (c.isToken() && !c.isCopiedToken())
    		return 0;
    	return getConvertedManaCost(c.getManaCost());
    }
    
    static public int getConvertedManaCost(String manaCost) {
        //see if the mana cost is all colorless, like "2", "0", or "12"
        
        if(manaCost.equals("")) return 0;
        
        while (manaCost.startsWith("X"))
        	manaCost = manaCost.substring(2);
        
        if(!manaCost.matches(".*[A-Z]+.*")) {
            try {
                return Integer.parseInt(manaCost);
            } catch(NumberFormatException ex) {
                ErrorViewer.showError(ex);
            }
        }
        
        //see if mana cost is colored and colorless like "2 B" or "1 U U"
        StringTokenizer tok = new StringTokenizer(manaCost);
        int cost = 0;
        try {
            //get the int from the mana cost like "1 U", get the 1
            cost = Integer.parseInt(tok.nextToken());
            //count colored mana cost
            cost += tok.countTokens();
            return cost;
        }
        //catches in case the cost has no colorless mana requirements like "U U"
        catch(NumberFormatException ex) {}
        
        //the mana cost is all colored mana like "U" or "B B B"
        tok = new StringTokenizer(manaCost);
        return tok.countTokens();
    }

    static public String addManaCosts(String mc1, String mc2)
    {
       String tMC = "";
       
       Integer cl1, cl2, tCL;
       cl1 = Integer.valueOf(mc1.replaceAll("[WUBRGSX]", "").trim());
       cl2 = Integer.valueOf(mc2.replaceAll("[WUBRGSX]", "").trim());
       tCL = cl1 + cl2;
       
       mc1 = mc1.replace(cl1.toString(), "").trim();
       mc2 = mc2.replace(cl2.toString(), "").trim();
       
       tMC = tCL.toString() + " " + mc1 + " " + mc2;
       
       //System.out.println("TMC:" + tMC);
       return tMC;
    }
    
    static public Card getRelative(Card c, String relation)
    {
    	if(relation.equals("CARDNAME")) return c;
    	else if(relation.startsWith("enchanted ")) return c.getEnchanting().get(0);
    	else if(relation.startsWith("equipped ")) return c.getEquipping().get(0);
    	//else if(relation.startsWith("target ")) return c.getTargetCard();
    	else throw new IllegalArgumentException("Error at CardUtil.getRelative: " + relation + "is not a valid relation");
    }
}
