
package forge;


import java.util.ArrayList;
import java.util.HashMap;

public class StaticEffect {
	private Card			 	source				  			= new Card();
	private int			 		keywordNumber				  	= 0;	
    private CardList            affectedCards                	= new CardList();
	private int			 		xValue				  			= 0;
	private int					yValue							= 0;
	
	//for types
	//private ArrayList<String>	types							= new ArrayList<String>();
	private HashMap<Card, ArrayList<String>> types				= new HashMap<Card, ArrayList<String>>();
	
	//for colors
	private	String				colorDesc 						= "";
	private	HashMap<Card, Long>	timestamps						= new HashMap<Card, Long>();
	
	
	public void addType(Card c, String s) {
		if(!types.containsKey(c)) {
			ArrayList<String> list = new ArrayList<String>();
			list.add(s);
			types.put(c, list);
		}
		else types.get(c).add(s);
	}
    
    public ArrayList<String> getTypes(Card c) {
    	ArrayList<String> returnList = new ArrayList<String>();
    	if(types.containsKey(c)) {
			returnList.addAll(types.get(c));
		}
    	return returnList;
    }
    
    public void removeType(Card c, String type) {
    	if(types.containsKey(c)) {
			types.get(c).remove(type);
		}
    }
    
    public void clearTypes(Card c) {
    	if(types.containsKey(c)) {
			types.get(c).clear();
		}
    }
    
    public void clearAllTypes() {
    	types.clear();
    }
	
	public String getColorDesc() {
		return colorDesc;
	}

	public void setColorDesc(String colorDesc) {
		this.colorDesc = colorDesc;
	}
    
    public HashMap<Card, Long> getTimestamps() {
		return timestamps;
	}
    
    public long getTimestamp(Card c) {
    	long stamp = -1;
    	Long l = timestamps.get(c);
    	if(null != l) {
    		stamp = l.longValue();
    	}
		return stamp;
	}
    
    public void addTimestamp(Card c, long timestamp) {
    	timestamps.put(c, new Long(timestamp));
    }
    
    public void clearTimestamps() {
    	timestamps.clear();
    }

	public void setSource(Card card) {
    	source = card;
    }
    
    public Card getSource() {
        return source;
    }
	
    public void setKeywordNumber(int i) {
    	keywordNumber = i;
    }
    
    public int getKeywordNumber() {
        return keywordNumber;
    }
    
    public CardList getAffectedCards() {
        return affectedCards;
    }
	
    public void setAffectedCards(CardList list) {
    	affectedCards = list;
    }
	
	public void setXValue(int x) {
    	xValue = x;
    }
    
    public int getXValue() {
        return xValue;
    }
    
    public void setYValue(int y) {
    	yValue = y;
    }
    
    public int getYValue() {
        return yValue;
    }
    
}//end class StaticEffect
