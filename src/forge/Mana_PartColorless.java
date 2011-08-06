package forge;
import forge.error.ErrorViewer;


public class Mana_PartColorless extends Mana_Part {
    private int manaNeeded;
    public void addToManaNeeded(int additional) { manaNeeded += additional; }
    
    //String manaCostToPay is like "1", "4", but NO COLOR
    public Mana_PartColorless(String manaCostToPay) {
        try {
            manaNeeded = Integer.parseInt(manaCostToPay);
        } catch(NumberFormatException ex) {
            ErrorViewer.showError(ex, "mana cost is not a number - %s", manaCostToPay);
            throw new RuntimeException(String.format("mana cost is not a number - %s", manaCostToPay), ex);
        }
    }
    
    public Mana_PartColorless(int manaCostToPay) {
            manaNeeded = manaCostToPay;
    }
    
    @Override
    public String toString() {
        if(isPaid()) return "";
        
        return String.valueOf(manaNeeded);
    }
    
    @Override
    public boolean isNeeded(String mana) {
        //ManaPart method
        checkSingleMana(mana);
        
        return 0 < manaNeeded;
    }
    
    @Override
    public boolean isNeeded(Mana mana) {
        //ManaPart method
    	if (mana.getAmount() > 1) throw new RuntimeException("Mana_PartColorless received Mana type with amount > 1");
    	
        return 0 < manaNeeded;
    }
    
    @Override
    public boolean isColor(String mana) {
    	return false;
    }
    
    @Override
	public boolean isColor(Mana mana) {
    	return false;
	}
    
    @Override
    public boolean isEasierToPay(Mana_Part mp)
    {
    	// Colorless is always easier to Pay for
    	return true;
    }
    
    @Override
    public void reduce(String mana) {
        //if mana is needed, then this mana cost is all paid up
        if(!isNeeded(mana)) throw new RuntimeException(
                "Mana_PartColorless : reduce() error, argument mana not needed, mana - " + mana
                        + ", toString() - " + toString());
        
        manaNeeded--;
    }
    
    @Override
    public void reduce(Mana mana) {
        //if mana is needed, then this mana cost is all paid up
        if(!isNeeded(mana)) throw new RuntimeException(
                "Mana_PartColorless : reduce() error, argument mana not needed, mana - " + mana
                        + ", toString() - " + toString());
        
        manaNeeded--;
    }
    
    @Override
    public boolean isPaid() {
        return manaNeeded == 0;
    }

	@Override
	public int getConvertedManaCost() {
		return manaNeeded;
	}
}
