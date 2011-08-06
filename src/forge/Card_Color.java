package forge;

import java.util.ArrayList;
import java.util.EnumSet;

import forge.card.mana.ManaCost;

public class Card_Color {
	// takes care of individual card color, for global color change effects use AllZone.GameInfo.getColorChanges()
	private EnumSet<Color> col;
	private boolean additional;
	public boolean getAdditional() { return additional; }
	private Card effectingCard = null;
	private long stamp = 0;
	public long getStamp() { return stamp; }
	
	private static long timeStamp = 0;
	public static long getTimestamp() {	return timeStamp;	}
	
	Card_Color(ManaCost mc, Card c, boolean addToColors, boolean baseColor){
		additional = addToColors;
		col = Color.ConvertManaCostToColor(mc);
		effectingCard = c;
		if (baseColor)
			stamp = 0;
		else
			stamp = timeStamp;
	}

	public Card_Color(Card c) {
		col = Color.Colorless();
		additional = false;
		stamp = 0;
		effectingCard = c;
	}

	boolean addToCardColor(String s){
		Color c = Color.ConvertFromString(s);
		if (!col.contains(c)){
			col.add(c);
			return true;
		}
		return false;
	}
	
	void fixColorless(){ 
		if (col.size() > 1 && col.contains(Color.Colorless))
			col.remove(Color.Colorless);
	}
	
	static void increaseTimestamp() { timeStamp++; }
    
    public boolean equals(String cost, Card c, boolean addToColors, long time){
    	return effectingCard == c && addToColors == additional && stamp == time;
    }
    
    public ArrayList<String> toStringArray(){
    	ArrayList<String> list = new ArrayList<String>();
    	for(Color c : col)
    		list.add(c.toString());
    	return list;
    }

    public static void main(String[] args) {
    	ManaCost mc = new ManaCost("R W U");
    	EnumSet<Color> col = Color.ConvertManaCostToColor(mc);
        System.out.println(col.toString());
    }
}
