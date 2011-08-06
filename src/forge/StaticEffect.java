
package forge;


import java.util.ArrayList;

public class StaticEffect {
	private Card			 	source				  			= new Card();
	private int			 		keywordNumber				  	= 0;	
    private CardList            affectedCards                	= new CardList();
	private int			 		xValue				  			= 0;
	private int					yValue							= 0;
	
	private ArrayList<String>	types							= new ArrayList<String>();

    
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
    
    public void addType(String s) {
    	types.add(s);
    }
    
    public ArrayList<String> getTypes() {
    	return types;
    }
    
    public void removeType(String type) {
    	types.remove(type);
    }
    
    public void clearTypes() {
    	types.clear();
    }
}
