package forge;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;



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
    
    public static boolean isColor(Card c, String col) {
    	ArrayList<String> list = getColors(c);
    	return list.contains(col);
    }
    
    public static ArrayList<String> getColors(Card c) {
    	return c.determineColor().toStringArray();
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
		if(manaCost.equals("")) return 0;
		
		ManaCost cost = new ManaCost(manaCost);
		return cost.getConvertedManaCost();
    }

    static public String addManaCosts(String mc1, String mc2)
    {
       String tMC = "";
       
       Integer cl1, cl2, tCL;
       String c1, c2, cc1, cc2;
       
       c1 = mc1.replaceAll("[WUBRGSX]", "").trim();
       c2 = mc2.replaceAll("[WUBRGSX]", "").trim();
       
       if (c1.length() > 0)
    	   cl1 = Integer.valueOf(c1);
       else
    	   cl1 = 0;
       
       if (c2.length() > 0)
    	   cl2 = Integer.valueOf(c2);
       else
    	   cl2 = 0;
       
       tCL = cl1 + cl2;
       
       cc1 = mc1.replaceAll("[0-9]", "").trim();
       cc2 = mc2.replaceAll("[0-9]", "").trim();
       
       tMC = tCL.toString() + " " + cc1 + " " + cc2;
       
       //System.out.println("TMC:" + tMC);
       return tMC.trim();
    }
    
    static public Card getRelative(Card c, String relation)
    {
    	if(relation.equals("CARDNAME")) return c;
    	else if(relation.startsWith("enchanted ")) return c.getEnchanting().get(0);
    	else if(relation.startsWith("equipped ")) return c.getEquipping().get(0);
    	//else if(relation.startsWith("target ")) return c.getTargetCard();
    	else throw new IllegalArgumentException("Error at CardUtil.getRelative: " + relation + "is not a valid relation");
    }
    
    public static boolean isACardType(String cardType) {
    	return (   cardType.equals("Artifact") || cardType.equals("Creature")
    			|| cardType.equals("Enchantment") || cardType.equals("Instant")
    			|| cardType.equals("Land") || cardType.equals("Planeswalker")
    			|| cardType.equals("Sorcery"));
    }
    
    public static boolean isASuperType(String cardType) {
    	return (   cardType.equals("Basic") || cardType.equals("Legendary")
    			|| cardType.equals("Snow") || cardType.equals("World"));
    }
    
    // Check if a Type is a Creature Type (by excluding all other types)
    public static boolean isACreatureType(String cardType) {
    	return (!isACardType(cardType) && !isASuperType(cardType) && !isALandType(cardType)
                && !cardType.equals("Arcane") && !cardType.equals("Trap")
                && !cardType.equals("Aura") && !cardType.equals("Shrine") 
                && !cardType.equals("Equipment") && !cardType.equals("Fortification"));
    }
    
    public static boolean isALandType(String cardType) {
    	return (isABasicLandType(cardType)
    			|| cardType.equals("Locus") || cardType.equals("Lair")
    			|| cardType.equals("Mine") || cardType.equals("Power-Plant")
    			|| cardType.equals("Tower") || cardType.equals("Urza's")
    			|| cardType.equals("Desert"));
    }
    
    public static boolean isABasicLandType(String cardType) {
    	return (cardType.equals("Plains")
    			|| cardType.equals("Island") || cardType.equals("Swamp")
    			|| cardType.equals("Mountain") || cardType.equals("Forest"));
    }
    
    public static String buildFilename(Card card)
    {	
        File path = null;
        if (card.isToken() && !card.isCopiedToken())
        	path = ForgeProps.getFile(NewConstants.IMAGE_TOKEN);
        else
        	path = ForgeProps.getFile(NewConstants.IMAGE_BASE);
        
        StringBuilder sbKey = new StringBuilder();
        
    	File  f = null;
        if (!card.getCurSetCode().equals(""))
        {
            String nn = "";
        	if (card.getRandomPicture() > 0)
                nn = Integer.toString(card.getRandomPicture());
        	
        	//First try 3 letter set code with MWS filename format 
        	sbKey.append(card.getCurSetCode() + "/");
        	sbKey.append(GuiDisplayUtil.cleanStringMWS(card.getName()) + nn + ".full");
        	
        	f = new File(path, sbKey.toString() + ".jpg");
        	if (f.exists())
        		return sbKey.toString();
        	
        	sbKey = new StringBuilder();
        	
        	//Second, try 2 letter set code with MWS filename format
        	sbKey.append(SetInfoUtil.getSetCode2_SetCode3(card.getCurSetCode()) + "/");
        	sbKey.append(GuiDisplayUtil.cleanStringMWS(card.getName()) + nn + ".full");
        	
        	f = new File(path, sbKey.toString() + ".jpg");
        	if (f.exists())
        		return sbKey.toString();
        	
        	sbKey = new StringBuilder();
        	
        	//Third, try 3 letter set code with Forge filename format
        	sbKey.append(card.getCurSetCode() + "/");
        	sbKey.append(GuiDisplayUtil.cleanString(card.getName()) + nn);
        	
        	f = new File(path, sbKey.toString() + ".jpg");
        	if (f.exists())
        		return sbKey.toString();
        	
        	sbKey = new StringBuilder();
        	
        }
    	
        //Last, give up with set images, go with the old picture type
    	sbKey.append(GuiDisplayUtil.cleanString(card.getImageName()));
    	if (card.getRandomPicture() > 1)
    		sbKey.append(card.getRandomPicture());
    	
    	f = new File(path, sbKey.toString() + ".jpg");
    	if (f.exists())
    		return sbKey.toString();
    	
    	sbKey = new StringBuilder();
    	
    	//Really last-ditch effort, forget the picture number
    	sbKey.append(GuiDisplayUtil.cleanString(card.getImageName()));
    	
    	f = new File(path, sbKey.toString() + ".jpg");
    	if (f.exists())
    		return sbKey.toString();
    	
    	//if still no file, download if option enabled?

    	return "none";
    }
}
