package forge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

public class ManaCost {
    //holds Mana_Part objects
    //ManaPartColor is stored before ManaPartColorless
    private ArrayList<Object> manaPart;
    private HashMap<String,Integer> sunburstMap = new HashMap<String,Integer>();
    
//manaCost can be like "0", "3", "G", "GW", "10", "3 GW", "10 GW"
    //or "split hybrid mana" like "2/G 2/G", "2/B 2/B 2/B"
    //"GW" can be paid with either G or W
    
    //can barely handle Reaper King mana cost "2/W 2/U 2/B 2/R 2/G"
    //to pay the colored costs for Reaper King you have to tap the colored
    //mana in the order shown from right to left (wierd I know)
    //note that when the cost is displayed it is backward "2/G 2/R 2/B 2/U 2/W"
    //so you would have to tap W, then U, then B, then R, then G (order matters)
    public ManaCost(String manaCost) {
    	if (manaCost.equals("") || manaCost.equals("X"))
    		manaCost = "0";
    	
    	while (manaCost.startsWith("X"))
    		manaCost = manaCost.substring(2);
    	manaPart = split(manaCost);
    }
    
    public int getSunburst()
    {
    	int ret = sunburstMap.size();
    	sunburstMap.clear();
    	return ret;
    }
    
    // takes a Short Color and returns true if it exists in the mana cost. Easier for split costs
    public boolean isColor(String color){
    	for(Object s : manaPart){
    		if (s.toString().contains(color))
    			return true;
    	}
    	return false;
    }
    
    // isNeeded(String) still used by the Computer, might have problems activating Snow abilities
    public boolean isNeeded(String mana) {
        if (mana.length() > 1)
        	mana = Input_PayManaCostUtil.getShortColorString(mana);
    	Mana_Part m;
        for(int i = 0; i < manaPart.size(); i++) {
            m = (Mana_Part) manaPart.get(i);
            if(m.isNeeded(mana)) return true;
        }
        return false;
    }
    
    public boolean isNeeded(Mana mana) {
    	Mana_Part m;
        for(int i = 0; i < manaPart.size(); i++) {
            m = (Mana_Part) manaPart.get(i);
            if(m.isNeeded(mana)) return true;
            if (m instanceof Mana_PartSnow && mana.isSnow()) return true;
        }
        return false;
    }
    
    public boolean isPaid() {
        Mana_Part m;
        for(int i = 0; i < manaPart.size(); i++) {
            m = (Mana_Part) manaPart.get(i);
            if(!m.isPaid()) return false;
        }
        return true;
    }//isPaid()
    
    public boolean payMana(Mana mana) {
        return addMana(mana);
    }
    
    public boolean payMana(String color) {
        color = Input_PayManaCostUtil.getShortColorString(color);
        return addMana(color);
    }
    
    public void increaseColorlessMana(int manaToAdd){
    	if (manaToAdd <= 0)
    		return;
    	
        Mana_Part m;
        for(int i = 0; i < manaPart.size(); i++) {
            m = (Mana_Part) manaPart.get(i);
            if (m instanceof Mana_PartColorless){
            	((Mana_PartColorless)m).addToManaNeeded(manaToAdd);
            	return;
            }
        }
        manaPart.add(new Mana_PartColorless(manaToAdd));
    }
    
    public boolean addMana(String mana) {
        if(!isNeeded(mana)) throw new RuntimeException("ManaCost : addMana() error, mana not needed - " + mana);
        
        Mana_Part choice = null;
        
        for(int i = 0; i < manaPart.size(); i++) {
        	Mana_Part m = (Mana_Part) manaPart.get(i);
            if(m.isNeeded(mana)) {
            	// if m is a better to pay than choice
            	if (choice == null){
	            	choice = m;
	            	continue;
            	}
            	if (m.isColor(mana) && choice.isEasierToPay(m))
            	{
            		choice = m;
            	}
            }
        }//for
        if (choice == null)
        	return false;
        
        choice.reduce(mana);
        if(!mana.equals(Constant.Color.Colorless)) {
        	if(sunburstMap.containsKey(mana))
        		sunburstMap.put(mana, sunburstMap.get(mana)+1);
        	else
        		sunburstMap.put(mana, 1);
        }
        return true;
    }
    
    public boolean addMana(Mana mana) {
        if(!isNeeded(mana)) throw new RuntimeException("ManaCost : addMana() error, mana not needed - " + mana);
        
        Mana_Part choice = null;
        
        for(int i = 0; i < manaPart.size(); i++) {
        	Mana_Part m = (Mana_Part) manaPart.get(i);
            if(m.isNeeded(mana)) {
            	// if m is a better to pay than choice
            	if (choice == null){
	            	choice = m;
	            	continue;
            	}
            	if (m.isColor(mana) && choice.isEasierToPay(m))
            	{
            		choice = m;
            	}
            }
        }//for
        if (choice == null)
        	return false;
        
        choice.reduce(mana);
        if(!mana.isColor(Constant.Color.Colorless)) {
        	if(sunburstMap.containsKey(mana.getColor()))
        		sunburstMap.put(mana.getColor(), sunburstMap.get(mana.getColor())+1);
        	else
        		sunburstMap.put(mana.getColor(), 1);
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ArrayList<Object> list = new ArrayList<Object>(manaPart);
        //need to reverse everything since the colored mana is stored first
        Collections.reverse(list);
        
        for(int i = 0; i < list.size(); i++) {
            sb.append(" ");
            sb.append(list.get(i).toString());
        }
        
        return sb.toString().trim();
    }
    
	public int getConvertedManaCost(){
		int cmc = 0;
		for(Object s : manaPart){
			cmc += ((Mana_Part)s).getConvertedManaCost();
		}
		return cmc;
	}

    private ArrayList<Object> split(String cost) {
        ArrayList<Object> list = new ArrayList<Object>();
        
        //handles costs like "3", "G", "GW", "10", "S"
        if(cost.length() == 1 || cost.length() == 2) {
            if(Character.isDigit(cost.charAt(0))) list.add(new Mana_PartColorless(cost));
            else if(cost.charAt(0) == 'S') list.add(new Mana_PartSnow());
            else list.add(new Mana_PartColor(cost));
        } else//handles "3 GW", "10 GW", "1 G G", "G G", "S 1"
        {
            //all costs that have a length greater than 2 have a space
            StringTokenizer tok = new StringTokenizer(cost);
            
            while(tok.hasMoreTokens())
                list.add(getManaPart(tok.nextToken()));
            
            //ManaPartColorless needs to be added AFTER the colored mana
            //in order for isNeeded() and addMana() to work correctly
            Object o = list.get(0);
            if(o instanceof Mana_PartSnow) {
                //move snow cost to the end of the list
                list.remove(0);
                list.add(o);
            }
            o = list.get(0);
            
            if(o instanceof Mana_PartColorless) {
                //move colorless cost to the end of the list
                list.remove(0);
                list.add(o);
            }
        }//else
        
        return list;
    }//split()
    
    private Mana_Part getManaPart(String partCost) {
        if(partCost.length() == 3) {
            return new Mana_PartSplit(partCost);
        } else if(Character.isDigit(partCost.charAt(0))) {
            return new Mana_PartColorless(partCost);
        } else if(partCost.equals("S")) {
            return new Mana_PartSnow();
        } else {
            return new Mana_PartColor(partCost);
        }
    }
}
