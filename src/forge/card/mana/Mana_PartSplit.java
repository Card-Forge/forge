
package forge.card.mana;

import forge.gui.input.Input_PayManaCostUtil;


//handles mana costs like 2/R or 2/B
//for cards like Flame Javelin (Shadowmoor)
public class Mana_PartSplit extends Mana_Part {
    private Mana_Part manaPart = null;
    private String    originalCost = "";
    
    public Mana_PartSplit(String manaCost) {
        //is mana cost like "2/R"
        if(manaCost.length() != 3) throw new RuntimeException(
                "Mana_PartSplit : constructor() error, bad mana cost parameter - " + manaCost);
        
        originalCost = manaCost;
    }
    
    private boolean isFirstTime() {
        return manaPart == null;
    }
    
    private void setup(String manaToPay) {
        //get R out of "2/R"
        String color = originalCost.substring(2, 3);
        
        //is manaToPay the one color we want or do we
        //treat it like colorless?
        //if originalCost is 2/R and is color W (treated like colorless)
        //or R?  if W use Mana_PartColorless, if R use Mana_PartColor
        //does manaToPay contain color?
        if(0 <= manaToPay.indexOf(color)) {
            manaPart = new Mana_PartColor(color);
        } else {
            //get 2 out of "2/R"
            manaPart = new Mana_PartColorless(originalCost.substring(0, 1));
        }
    }//setup()
    
    @Override
    public void reduce(String mana) {
        if(isFirstTime()) setup(mana);
        
        manaPart.reduce(mana);
    }
    
    @Override
    public void reduce(Mana mana) {
        if(isFirstTime()) setup(Input_PayManaCostUtil.getShortColorString(mana.getColor()));
        
        manaPart.reduce(mana);
    }
    
    @Override
    public boolean isNeeded(String mana) {
        if(isFirstTime()) {
            //always true because any mana can pay the colorless part of 2/G
            return true;
        }
        
        return manaPart.isNeeded(mana);
    }//isNeeded()
    
	public boolean isNeeded(Mana mana) {
        if(isFirstTime()) {
            //always true because any mana can pay the colorless part of 2/G
            return true;
        }
        
        return manaPart.isNeeded(mana);
	}
	
    @Override
    public boolean isColor(String mana) {
        //ManaPart method
    	String mp = toString();
        return mp.indexOf(mana) != -1;
    }
    
    @Override
	public boolean isColor(Mana mana) {
    	String color = Input_PayManaCostUtil.getShortColorString(mana.getColor());
    	String mp = toString();
    	return mp.indexOf(color) != -1;
	}
    
    @Override
    public boolean isEasierToPay(Mana_Part mp)
    {
    	if (mp instanceof Mana_PartColorless) return false;
    	if (!isFirstTime()) return true;
    	return toString().length() >= mp.toString().length();
    }
    
    @Override
    public String toString() {
        if(isFirstTime()) return originalCost;
        
        return manaPart.toString();
    }
    
    @Override
    public boolean isPaid() {
        if(isFirstTime()) return false;
        
        return manaPart.isPaid();
    }

	@Override
	public int getConvertedManaCost() {
		// grab the colorless portion of the split cost (usually 2, but possibly more later)
		return Integer.parseInt(originalCost.substring(0, 1));
	}
}
