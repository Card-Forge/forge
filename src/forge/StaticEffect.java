
package forge;


import java.util.ArrayList;
import java.util.HashMap;
import forge.card.spellability.SpellAbility;

public class StaticEffect {
	private Card			 	source				  				= new Card();
	private int			 		keywordNumber					  	= 0;	
    private CardList            affectedCards       	         	= new CardList();
	private int			 		xValue				  				= 0;
	private int					yValue								= 0;
	
	//for P/T
	private HashMap<Card, String>	originalPT						= new HashMap<Card, String>();
	
	//for types
	private boolean								overwriteTypes		= false;
	private boolean								keepSupertype		= false;
	private boolean								removeSubTypes		= false;
	private HashMap<Card, ArrayList<String>>	types				= new HashMap<Card, ArrayList<String>>();
	private HashMap<Card, ArrayList<String>>	originalTypes		= new HashMap<Card, ArrayList<String>>();
	
	//keywords
	private boolean								overwriteKeywords	= false;
	private HashMap<Card, ArrayList<String>>	originalKeywords	= new HashMap<Card, ArrayList<String>>();
	
	//for abilities
	private boolean									overwriteAbilities	= false;
	private HashMap<Card, ArrayList<SpellAbility>>	originalAbilities	= new HashMap<Card, ArrayList<SpellAbility>>();
	
	//for colors
	private	String				colorDesc 							= "";
	private	HashMap<Card, Long>	timestamps							= new HashMap<Card, Long>();
	
	
	//overwrite SAs
	public boolean isOverwriteAbilities() {
		return overwriteAbilities;
	}

	public void setOverwriteAbilities(boolean overwriteAbilities) {
		this.overwriteAbilities = overwriteAbilities;
	}
	
	//original SAs
	public void addOriginalAbilities(Card c, SpellAbility sa) {
		if(!originalAbilities.containsKey(c)) {
			ArrayList<SpellAbility> list = new ArrayList<SpellAbility>();
			list.add(sa);
			originalAbilities.put(c, list);
		}
		else originalAbilities.get(c).add(sa);
	}
	
	public void addOriginalAbilities(Card c, ArrayList<SpellAbility> s) {
		ArrayList<SpellAbility> list = new ArrayList<SpellAbility>(s);
		if(!originalAbilities.containsKey(c)) {
			originalAbilities.put(c, list);
		}
		else {
			originalAbilities.remove(c);
			originalAbilities.put(c, list);
		}
	}
    
    public ArrayList<SpellAbility> getOriginalAbilities(Card c) {
    	ArrayList<SpellAbility> returnList = new ArrayList<SpellAbility>();
    	if(originalAbilities.containsKey(c)) {
			returnList.addAll(originalAbilities.get(c));
		}
    	return returnList;
    }
    
    public void clearOriginalAbilities(Card c) {
    	if(originalAbilities.containsKey(c)) {
			originalAbilities.get(c).clear();
		}
    }
    
    public void clearAllOriginalAbilities() {
    	originalAbilities.clear();
    }

	//overwrite keywords
	public boolean isOverwriteKeywords() {
		return overwriteKeywords;
	}

	public void setOverwriteKeywords(boolean overwriteKeywords) {
		this.overwriteKeywords = overwriteKeywords;
	}
	
	//original keywords
	public void addOriginalKeyword(Card c, String s) {
		if(!originalKeywords.containsKey(c)) {
			ArrayList<String> list = new ArrayList<String>();
			list.add(s);
			originalKeywords.put(c, list);
		}
		else originalKeywords.get(c).add(s);
	}
	
	public void addOriginalKeywords(Card c, ArrayList<String> s) {
		ArrayList<String> list = new ArrayList<String>(s);
		if(!originalKeywords.containsKey(c)) {
			originalKeywords.put(c, list);
		}
		else {
			originalKeywords.remove(c);
			originalKeywords.put(c, list);
		}
	}
    
    public ArrayList<String> getOriginalKeywords(Card c) {
    	ArrayList<String> returnList = new ArrayList<String>();
    	if(originalKeywords.containsKey(c)) {
			returnList.addAll(originalKeywords.get(c));
		}
    	return returnList;
    }
    
    public void clearOriginalKeywords(Card c) {
    	if(originalKeywords.containsKey(c)) {
			originalKeywords.get(c).clear();
		}
    }
    
    public void clearAllOriginalKeywords() {
    	originalKeywords.clear();
    }
	
	//original power/toughness
	public void addOriginalPT(Card c, int power, int toughness) {
		String pt = power+"/"+toughness;
		if(!originalPT.containsKey(c)) {
			originalPT.put(c, pt);
		}
	}
	
    public int getOriginalPower(Card c) {
    	int power = -1;
    	if(originalPT.containsKey(c)) {
			power = Integer.parseInt(originalPT.get(c).split("/")[0]);
		}
    	return power;
    }
    
    public int getOriginalToughness(Card c) {
    	int tough = -1;
    	if(originalPT.containsKey(c)) {
			tough = Integer.parseInt(originalPT.get(c).split("/")[1]);
		}
    	return tough;
    }
    
    public void clearAllOriginalPTs() {
    	originalPT.clear();
    }
	
	//should we overwrite types?
	public boolean isOverwriteTypes() {
		return overwriteTypes;
	}

	public void setOverwriteTypes(boolean overwriteTypes) {
		this.overwriteTypes = overwriteTypes;
	}
	
	public boolean isKeepSupertype() {
		return keepSupertype;
	}

	public void setKeepSupertype(boolean keepSupertype) {
		this.keepSupertype = keepSupertype;
	}
	
	//should we overwrite land types?
	public boolean isRemoveSubTypes() {
		return removeSubTypes;
	}

	public void setRemoveSubTypes(boolean removeSubTypes) {
		this.removeSubTypes = removeSubTypes;
	}

	//original types
	public void addOriginalType(Card c, String s) {
		if(!originalTypes.containsKey(c)) {
			ArrayList<String> list = new ArrayList<String>();
			list.add(s);
			originalTypes.put(c, list);
		}
		else originalTypes.get(c).add(s);
	}
	
	public void addOriginalTypes(Card c, ArrayList<String> s) {
		ArrayList<String> list = new ArrayList<String>(s);
		if(!originalTypes.containsKey(c)) {
			originalTypes.put(c, list);
		}
		else {
			originalTypes.remove(c);
			originalTypes.put(c, list);
		}
	}
    
    public ArrayList<String> getOriginalTypes(Card c) {
    	ArrayList<String> returnList = new ArrayList<String>();
    	if(originalTypes.containsKey(c)) {
			returnList.addAll(originalTypes.get(c));
		}
    	return returnList;
    }
    
    public void clearOriginalTypes(Card c) {
    	if(originalTypes.containsKey(c)) {
			originalTypes.get(c).clear();
		}
    }
    
    public void clearAllOriginalTypes() {
    	originalTypes.clear();
    }
	
	//statically assigned types
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
    	timestamps.put(c, Long.valueOf(timestamp));
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
