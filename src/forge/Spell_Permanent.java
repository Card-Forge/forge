
package forge;


public class Spell_Permanent extends Spell {
    

    private static final long serialVersionUID = 2413495058630644447L;
    
    public Spell_Permanent(Card sourceCard) {
        super(sourceCard);
        
        setManaCost(sourceCard.getManaCost());
        
        if(sourceCard.isCreature()) {
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(sourceCard.getName()).append(" - Creature ").append(sourceCard.getNetAttack());
        	sb.append(" / ").append(sourceCard.getNetDefense());
        	setStackDescription(sb.toString());
        }
        else setStackDescription(sourceCard.getName());
        
        setDescription(getStackDescription());
    }//Spell_Permanent()
    
    @Override
    public boolean canPlay() {
        return super.canPlay()
                || (getSourceCard().getKeyword().contains("Flash") && !AllZone.GameAction.isCardInPlay(getSourceCard())
                    && !getSourceCard().isUnCastable());
    }
    
    @Override
    public boolean canPlayAI() {
    	
    	Card card = getSourceCard();
    	
        //check on legendary
        if(card.getType().contains("Legendary")) {
        	CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
            if (list.containsName(card.getName()) /*&&
            	!getSourceCard().getName().equals("Flagstones of Trokair")*/)
            	return false;
        }
        if(card.getType().contains("Planeswalker")) {
        	CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
        	list = list.getType("Planeswalker");
        	
        	for (int i=0;i<list.size();i++)
        	{
        		String subtype = card.getType().get(card.getType().size() - 1);
        		CardList cl = list.getType(subtype);
        		
        		 if(cl.size() > 0) {
                     return false;
                 }
        	}
        }
        if(card.getType().contains("World")) {
        	CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
        	list = list.getType("World");
        	if(list.size() > 0) return false;
        }
        if(card.getType().contains("Creature") && card.getNetDefense() <= 0) {
        	 return false;
        }
        
        return super.canPlayAI();
    }//canPlayAI()
    
    @Override
    public void resolve() {
        Card c = getSourceCard();
        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getController());
        play.add(c);
    }
}
