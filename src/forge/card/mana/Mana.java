package forge.card.mana;

import forge.Card;
import forge.Constant;
import forge.gui.input.Input_PayManaCostUtil;

public class Mana {
	private String color;
	private int amount = 0;
	private Card sourceCard = null;
		
	public Mana(String col, int amt, Card source){
		color = col;
		amount = amt;
		if (source == null)
			return;

		sourceCard = source;
	}
	
	public String toString()
	{
		if (color.equals(Constant.Color.Colorless))
			return Integer.toString(amount);
		
		String manaString = "";
		StringBuilder sbMana = new StringBuilder();

		manaString = Input_PayManaCostUtil.getShortColorString(color);

        for(int i = 0; i < amount; i++)
        	sbMana.append(manaString);
        return sbMana.toString();
	}
	
	public String toDescriptiveString()
	{
		// this will be used for advanced choice box
		if (color.equals(Constant.Color.Colorless))
			return Integer.toString(amount);
		
		String manaString = "";
		StringBuilder sbMana = new StringBuilder();

		manaString = Input_PayManaCostUtil.getShortColorString(color);

        for(int i = 0; i < amount; i++)
        	sbMana.append(manaString);
        
        if (isSnow())
        	sbMana.append("(S)");
        
        sbMana.append(" From ");
        sbMana.append(sourceCard.getName());
        
        return sbMana.toString();
	}
	
	public Mana[] toSingleArray(){
		Mana[] normalize = new Mana[amount];
		for(int i = 0; i < normalize.length; i++){
			normalize[i] = new Mana(this.color, 1, this.sourceCard);
		}
		return normalize;
	}
	
	public boolean isSnow() { return sourceCard.isSnow(); 	}
	
	public boolean fromBasicLand() { return sourceCard.isBasicLand(); } // for Imperiosaur
	
	public int getColorlessAmount(){ return  color.equals(Constant.Color.Colorless) ? amount : 0; }
	
	public int getAmount() { return amount; }
	
	public boolean isColor(String col) { return color.equals(col); 	}
	
	public boolean isColor(String[] colors)
	{ 
		for(String col : colors)
			if (color.equals(col))
				return true;
		
		return false;
	}
	
	public String getColor(){ return color; }
	
	public Card getSourceCard(){ return sourceCard; }
	
	public boolean fromSourceCard(Card c){ return sourceCard.equals(c); }

	public void decrementAmount(){ amount--; }
}
