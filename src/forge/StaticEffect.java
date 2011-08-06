package forge;


public class StaticEffect {
	private Card			 	source				  			= new Card();
	private int			 		keywordNumber				  	= 0;	
    private CardList            affectedCards                	= new CardList();
	private int			 		xValue				  			= 0;	  

    
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
}
